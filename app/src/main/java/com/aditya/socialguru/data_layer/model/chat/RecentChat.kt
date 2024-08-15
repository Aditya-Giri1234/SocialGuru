package com.aditya.socialguru.data_layer.model.chat

data class RecentChat(
    val chatRoomId:String?=null,
    val lastMessageTimeInTimeStamp:Long?=null,
    val lastMessageTimeInText:String?=null,
    val unSeenMessageCount:Int?=0,
    val message:String?=null,
    val receiverId:String?=null,
    val senderId:String?=null,
    val userId:String?=null,
    val lastMessageSeen:String?=null,
    //This is help to determine the type of message
    val lastMessageType:String?=null,
    // This is for group Chat
    @field:JvmField
    val isGroupChat:Boolean?=false,
    val infoMessageType:String?=null,
    val addedOrRemovedUserId:String?=null ,
    val newMembers:List<String>?=null
)
