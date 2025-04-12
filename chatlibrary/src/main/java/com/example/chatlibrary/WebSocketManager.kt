package com.example.chatlibrary

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.toByteString
import kotlin.coroutines.cancellation.CancellationException

internal class WebSocketManager {
    companion object {
        private const val TAG = "ChatWebSocketManager"
        private const val WEBSOCKET_URL = "wss://echo.websocket.org"
    }

    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, "Ktor: $message")
                }
            }
        }
    }

    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var messageListener: MessageListener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false
    private var connectJob: Job? = null
    private var listenerJob: Job? = null

    interface MessageListener {
        fun onMessageReceived(message: MessageItem)
        fun onConnectionStatusChanged(connected: Boolean)
        fun onError(message: String)
    }

    fun setMessageListener(listener: MessageListener) {
        this.messageListener = listener
    }

    fun connect() {
        if (isConnected || connectJob?.isActive == true) {
            Log.w(TAG, "Already connected or connecting.")
            return
        }
        Log.d(TAG, "Attempting to connect")
        connectJob = coroutineScope.launch {
            try {
                client.wss(WEBSOCKET_URL) {
                    webSocketSession = this
                    isConnected = true
                    Log.i(TAG, "WebSocket Connection Established.")
                    withContext(Dispatchers.Main) {
                        messageListener?.onConnectionStatusChanged(true)
                    }
                    listenerJob = launch { listenIncomingMessages() }
                    listenerJob?.join()
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket Connection Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    messageListener?.onError("Connection failed: ${e.localizedMessage}")
                    messageListener?.onConnectionStatusChanged(false)
                }
            } finally {
                Log.i(TAG, "WebSocket connection block finished.")
                isConnected = false
                webSocketSession = null
                if (listenerJob?.isActive == true) {
                    withContext(Dispatchers.Main) {
                        messageListener?.onConnectionStatusChanged(false)
                    }
                }
                listenerJob?.cancelAndJoin()
                listenerJob = null
            }
        }
        connectJob?.invokeOnCompletion { throwable ->
            if (throwable != null && throwable !is CancellationException) {
                Log.e(TAG, "Connect Job completed with error", throwable)
            } else {
                Log.d(TAG, "Connect Job completed.")
            }
            if (!isConnected) {
                MainScope().launch {
                    messageListener?.onConnectionStatusChanged(false)
                }
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.listenIncomingMessages() {
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val receivedText = frame.readText()
                        Log.d(TAG, "<<< Received Text: $receivedText")

                        val messageItem = MessageItem(text = receivedText, isSentByUser = false)
                        withContext(Dispatchers.Main) {
                            messageListener?.onMessageReceived(messageItem)
                        }
                    }

                    is Frame.Binary -> {
                        val bytes = frame.readBytes()
                        Log.d(
                            TAG,
                            "<<< Received Binary: ${bytes.size} bytes, Hex: ${
                                bytes.toByteString().hex()
                            }"
                        )
                        val messageItem = MessageItem(
                            text = "Received Binary: ${bytes.toByteString().hex()}",
                            isSentByUser = false
                        )
                        withContext(Dispatchers.Main) {
                            messageListener?.onMessageReceived(messageItem)
                        }
                    }

                    is Frame.Close -> {
                        Log.i(TAG, "<<< Received Close Frame: ${frame.readReason()}")
                        break
                    }

                    is Frame.Ping -> {
                        Log.d(TAG, "<<< Received Ping")
                    }

                    is Frame.Pong -> {
                        Log.d(TAG, "<<< Received Pong")
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            Log.i(TAG, "WebSocket incoming channel closed.")
        } catch (e: CancellationException) {
            Log.i(TAG, "Listener Job cancelled.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during message listening: ${e.message}", e)
            withContext(Dispatchers.Main) {
                messageListener?.onError("Error receiving message: ${e.localizedMessage}")
            }
        } finally {
            Log.d(TAG, "Exiting listenIncomingMessages loop.")
        }
    }

    fun sendMessage(text: String) {
        if (!isConnected || webSocketSession == null) {
            Log.w(TAG, "Cannot send message, WebSocket is not connected.")
            MainScope().launch {
                messageListener?.onError("Not connected. Cannot send message.")
            }
            return
        }
        Log.d(TAG, ">>> Sending Text: $text")
        coroutineScope.launch {
            try {
                webSocketSession?.send(Frame.Text(text))
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    messageListener?.onError("Failed to send message: ${e.localizedMessage}")
                }
            }
        }
    }

    fun sendBytes(bytes: ByteString) {
        if (!isConnected || webSocketSession == null) {
            Log.w(TAG, "Cannot send bytes, WebSocket is not connected.")
            MainScope().launch { messageListener?.onError("Not connected. Cannot send bytes.") }
            return
        }
        Log.d(TAG, ">>> Sending Bytes: ${bytes.hex()}")
        coroutineScope.launch {
            try {
                webSocketSession?.send(Frame.Binary(true, bytes.toByteArray()))
            } catch (e: Exception) {
                Log.e(TAG, "Error sending binary message: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    messageListener?.onError("Failed to send binary message: ${e.localizedMessage}")
                }
            }
        }
    }

    fun disconnect() {
        Log.i(TAG, "Disconnect requested.")
        listenerJob?.cancel()
        connectJob?.cancel()
        coroutineScope.launch {
            try {
                webSocketSession?.close(
                    CloseReason(
                        CloseReason.Codes.NORMAL,
                        "Client requested disconnect"
                    )
                )
                Log.d(TAG, "WebSocket session closed.")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing WebSocket session: ${e.message}")
            } finally {
                webSocketSession = null
                isConnected = false
                withContext(Dispatchers.Main) {
                    messageListener?.onConnectionStatusChanged(false)
                }
            }
        }
    }
}