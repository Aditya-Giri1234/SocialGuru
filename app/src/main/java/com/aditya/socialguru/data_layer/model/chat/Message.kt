package com.aditya.socialguru.data_layer.model.chat

data class Message(
    val messageId:String?=null,
    val messageType:String?=null,
    val text:String?=null,
    val imageUri:String?=null,
    val videoUri:String?=null,
    val senderId:String?=null,
    val receiverId:String?=null,
    val messageSentTimeInTimeStamp:Long?=null,
    val messageSendTimeInText:String?=null,
    val seenStatus:String?=null
)
