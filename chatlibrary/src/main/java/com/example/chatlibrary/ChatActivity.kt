package com.example.chatlibrary

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatlibrary.databinding.ActivityChatBinding
import okio.ByteString

internal class ChatActivity : AppCompatActivity(), WebSocketManager.MessageListener {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val webSocketManager = WebSocketManager()
    private val messageList = mutableListOf<MessageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: Activity started")
        setupSendButton()
        setupRecyclerView()

        webSocketManager.setMessageListener(this)
        webSocketManager.connect()

    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isEmpty()) return@setOnClickListener

            val byteToSend: ByteString? = tryParseByteInput(messageText)

            val userMessage = MessageItem(text = messageText, isSentByUser = true)
            addMessageToAdapter(userMessage)
            scrollToBottom()
            binding.editTextMessage.text.clear()

            if (byteToSend != null) {
                webSocketManager.sendBytes(byteToSend)
            } else {
                webSocketManager.sendMessage(messageText)
            }
        }
    }

    private fun tryParseByteInput(input: String): ByteString? {
        return runCatching {
            byteInputRegex.matchEntire(input)?.let { matchResult ->
                val (decStr, hexStr) = matchResult.destructured
                val decimalValue = decStr.toInt()
                val hexValue = hexStr.toInt(16)

                if (decimalValue == hexValue && hexValue in 0..255) {
                    ByteString.of(hexValue.toByte())
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
            setItemViewCacheSize(20)
        }
        chatAdapter.submitList(messageList)
        Log.d(TAG, "RecyclerView setup complete.")
    }

    private fun addMessageToAdapter(message: MessageItem) {
        Log.d(TAG, "Adding message to adapter: ${message.text}")
        messageList.add(message)
        chatAdapter.submitList(messageList.toList())
    }

    private fun scrollToBottom() {
        binding.recyclerViewChat.post {
            val lastPosition = chatAdapter.itemCount - 1
            if (lastPosition >= 0) {
                binding.recyclerViewChat.smoothScrollToPosition(lastPosition)
            }
        }
    }


    override fun onMessageReceived(message: MessageItem) {
        Log.i(TAG, "Message received: ${message.text}")
        addMessageToAdapter(message)
    }

    override fun onConnectionStatusChanged(connected: Boolean) {
        val status = if (connected) "Connected" else "Disconnected"
        Log.i(TAG, "Connection status changed: $status")
        binding.buttonSend.isEnabled = connected
        binding.editTextMessage.isEnabled = connected
    }

    override fun onError(message: String) {
        Log.e(TAG, "WebSocket Error: $message")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Disconnecting WebSocket")
        webSocketManager.disconnect()
    }

    companion object {
        private const val TAG = "ChatActivity"
        private val byteInputRegex = Regex("^(\\d+)\\s*=\\s*0x([0-9a-fA-F]+)$")
    }
}