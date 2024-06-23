package com.aditya.socialguru.data_layer.model.chat

data class Message(
    val messageId:String?=null,
    val messageType:String?=null,  // Date Header or Chat
    val chatType:String?=null,  // text or image or video or all
    val text:String?=null,
    val imageUri:String?=null,
    val videoUri:String?=null,
    val senderId:String?=null,
    val receiverId:String?=null,
    val messageSentTimeInTimeStamp:Long?=null,
    val messageSendTimeInText:String?=null,
    var seenStatus:String?=null ,
    val senderProfileImage:String?=null  // This is for recycle view showing receiver profile on message
)
