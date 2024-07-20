package com.aditya.socialguru.domain_layer.remote_service.chat

import android.net.Uri
import com.aditya.socialguru.data_layer.model.chat.Message

interface ChatMessageOption {
    fun onImageClick() : (Uri)->Unit
    fun onVideoClick()  : (Uri) -> Unit
    fun <T> onMessageClick(message: T)
    fun <T> onLongMessageClick(message: T)
    fun onProfileClick()

}