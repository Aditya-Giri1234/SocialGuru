package com.aditya.socialguru.data_layer.model.chat

data class LastMessage(
    val senderId:String?=null,
    val receiverId:String?=null,
    val messageType:String?=null,
    val chatType:String?=null,
    val message:String?=null,
    val unSeenMessageCount:Int?=null,
    val lastMessageSentTimeInTimeStamp:Long?=null,
    val lastMessageSentTimeInText:String?=null,
    val isUser1Online:Boolean?=false,  // User 1 and 2 determine which userId sorting , who come first that is user 1
    val isUser2Online:Boolean?=false
)
