package com.aditya.socialguru.data_layer.model.chat.group

data class GroupLastMessage(
    val senderId:String?=null,
    val chatType:String?=null,  // Text or Image Or Video
    val message:String?=null,
    val lastMessageSentTimeInTimeStamp:Long?=null,
    val lastMessageSentTimeInText:String?=null,
    //This is help to determine the type of message
    val messageType:String?=null,  // Message or Date or Info
    // This is for group Chat
    val infoMessageType:String?=null,
    val addedOrRemovedUserId:String?=null
)