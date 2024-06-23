package com.aditya.socialguru.data_layer.model.chat

data class RecentChat(
    val chatRoomId:String?=null,
    val lastMessageTimeInTimeStamp:Long?=null,
    val lastMessageTimeInText:String?=null,
    val unSeenMessageCount:Int?=0,
    val message:String?=null,
    val lastMessageType:String?=null,
    val receiverId:String?=null,
    val senderId:String?=null,
    val userId:String?=null,
    val lastMessageSeen:String?=null
)
