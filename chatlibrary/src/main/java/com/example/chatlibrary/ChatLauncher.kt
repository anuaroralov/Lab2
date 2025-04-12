package com.example.chatlibrary

import android.content.Context
import android.content.Intent

object ChatLauncher {

    /**
     * Starts the chat activity.
     *
     * @param context The context (Activity, Application) to start the chat from.
     */
    @JvmStatic
    fun start(context: Context) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            if (context !is android.app.Activity) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }
}