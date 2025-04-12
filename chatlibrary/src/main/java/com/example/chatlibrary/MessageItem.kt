package com.example.chatlibrary

internal data class MessageItem(
    val id: Long = System.nanoTime(),
    val text: String,
    val isSentByUser: Boolean
)