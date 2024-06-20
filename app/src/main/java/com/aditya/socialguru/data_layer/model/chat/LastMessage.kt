package com.aditya.socialguru.data_layer.model.chat

data class LastMessage(
    val senderId:String?=null,
    val receiverId:String?=null,
    val messageType:String?=null,
    val message:String?=null,
    val unSeenMessageCount:Int?=null,
    val lastMessageSentTimeInTimeStamp:Long?=null,
    val lastMessageSentTimeInText:String?=null
)
