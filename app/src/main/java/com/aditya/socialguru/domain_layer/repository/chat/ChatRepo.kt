package com.aditya.socialguru.domain_layer.repository.chat

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.ChatManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import kotlinx.coroutines.flow.Flow

class ChatRepo {
    suspend fun sendMessage(message: Message,lastMessage: LastMessage, chatRoomId: String , isUserOnline:Boolean=false)=
        FirebaseManager.sentMessage(message,lastMessage,chatRoomId,isUserOnline)
    suspend fun getFriendList() = FirebaseManager.getFriendListAndListenChange()

    suspend fun getUser(userId: String): Flow<Resource<User>> = FirebaseManager.getUser(userId)

    suspend fun getChatMessageAndListen(chatRoomId: String) = FirebaseManager.getChatMessageAndListen(chatRoomId)

    suspend fun listenLastMessage(chatRoomId: String) = ChatManager.listenLastMessage(chatRoomId)
    suspend fun updateUserAvailabilityForChatRoom(chatRoomId: String,isIAmUser1:Boolean,status: Boolean)=FirebaseManager.updateUserAvailabilityForChatRoom(chatRoomId
        ,isIAmUser1,status)

    suspend fun updateMessageChatAvailability(status: String,messageId:String,chatRoomId: String ,receiverId:String) = FirebaseManager.updateSeenStatus(status,messageId,chatRoomId ,receiverId)

    suspend fun getRecentChatAndListen() = ChatManager.getRecentChatAndListen()

    suspend fun deleteMessage(message: Message,chatRoomId:String,userId:String  , lastMessage: LastMessage?, secondLastMessage:Message?=null) = FirebaseManager.deleteMessage(message,chatRoomId,userId,lastMessage,secondLastMessage)

}