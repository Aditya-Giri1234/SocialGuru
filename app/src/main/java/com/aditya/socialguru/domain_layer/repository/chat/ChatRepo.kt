package com.aditya.socialguru.domain_layer.repository.chat

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.domain_layer.helper.Constants
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

    suspend fun listenLastMessage(chatRoomId: String) = FirebaseManager.listenLastMessage(chatRoomId)
    suspend fun updateUserAvailabilityForChatRoom(chatRoomId: String,isIAmUser1:Boolean,status: Boolean)=FirebaseManager.updateUserAvailabilityForChatRoom(chatRoomId
        ,isIAmUser1,status)

    suspend fun updateMessageChatAvailability(status: String,messageId:String,chatRoomId: String ,receiverId:String) = FirebaseManager.updateSeenStatus(status,messageId,chatRoomId ,receiverId)

    suspend fun getRecentChatAndListen() = FirebaseManager.getRecentChatAndListen()

    suspend fun deleteMessage(message: Message,chatRoomId:String,userId:String  , lastMessage: LastMessage?, secondLastMessage:Message?=null) = FirebaseManager.deleteMessage(message,chatRoomId,userId,lastMessage,secondLastMessage)

    suspend fun clearChats(chatRoomId: String,receiverId: String) = ChatManager.clearChats(chatRoomId,receiverId)

    suspend fun isUserMutedAndListen(userId: String) = FirebaseManager.isUserMutedAndListen(userId)

    suspend fun muteChatNotification(userId: String, isMute: Boolean) = FirebaseManager.muteChatNotification(userId, isMute)

    suspend fun getAllMediaOfChat(chatRoomId: String) = FirebaseManager.getAllMediaOfChat(chatRoomId)

    suspend fun sentGroupMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>, // Just for add  message recent chat
        action: Constants.InfoType? = null,
        addedOrRemovedUserId: String? = null,
        groupInfo: GroupInfo? = null
    ) = FirebaseManager.sentGroupMessage(
        message,
        lastMessage,
        chatRoomId,
        users,
        action,
        addedOrRemovedUserId,
        groupInfo
    )

    suspend fun deleteMessage(
        message: GroupMessage,
        chatRoomId: String,
        users: List<String>,
        lastMessage: GroupLastMessage?,
        secondLastMessage: GroupMessage? = null
    ) = FirebaseManager.deleteMessage(message, chatRoomId, users, lastMessage, secondLastMessage)

    suspend fun clearChats(chatRoomId: String, users: List<String>, lastMessage: GroupLastMessage,
    secondLastMessage: GroupMessage) = FirebaseManager.clearChats(chatRoomId, users , lastMessage,secondLastMessage)


    suspend fun getGroupChatMessageAndListen(chatRoomId: String) = FirebaseManager.getGroupChatMessageAndListen(chatRoomId)

    suspend fun getGroupMemberInfo(chatRoomId: String) = FirebaseManager.getGroupMemberInfo(chatRoomId)

    suspend fun getGroupInfo(chatRoomId: String) = FirebaseManager.getGroupInfo(chatRoomId)

    suspend fun updateGroupMemberOnlineStatus(chatRoomId: String,status:Boolean) = FirebaseManager.updateGroupMemberOnlineStatus(chatRoomId,status)

    suspend fun listenNewMessage(chatRoomId: String) = FirebaseManager.listenNewMessage(chatRoomId)

    suspend fun updateGroupInfo(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>, // Just for add  message recent chat
        groupInfo: GroupInfo? = null ,
        deleteImage:String?=null ,
        uploadingImage:String?=null
    ) = FirebaseManager.updateGroupInfo(
        message,
        lastMessage,
        chatRoomId,
        users,
        groupInfo ,
        deleteImage ,
        uploadingImage
    )

    suspend fun sentGroupInfoMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,// Just for add  message recent chat
        action: Constants.InfoType,
        addedOrRemovedUserId: String? = null,
        newMembers:List<String>?=null,
        groupInfo: GroupInfo? = null
    ) = ChatManager.sentGroupInfoMessage(message, lastMessage, chatRoomId, users, action, addedOrRemovedUserId ,newMembers, groupInfo)


    suspend fun getAllUserByIds(userIds:List<String>) = FirebaseManager.getAllUserByIds(userIds)


    //region :: Recent Chat
    suspend fun deleteRecentChat(chatRoomId: String) = ChatManager.deleteRecentChat(chatRoomId)
    //endregion

}