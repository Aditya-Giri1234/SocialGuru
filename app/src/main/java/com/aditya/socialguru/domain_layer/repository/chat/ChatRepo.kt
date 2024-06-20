package com.aditya.socialguru.domain_layer.repository.chat

import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.domain_layer.service.FirebaseManager

class ChatRepo {
    suspend fun sendMessage(message: Message,chatRoomId:String)=FirebaseManager.sentMessage(message,chatRoomId)
    suspend fun getFriendList() = FirebaseManager.getFriendListAndListenChange()
}