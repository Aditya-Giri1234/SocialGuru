package com.aditya.socialguru.data_layer.model.chat.group

data class GroupMessage(
    val messageId:String?=null,
    val messageType:String?=null,  // text or image or video or all
    val chatType:String?=null,  // Date Header or Chat or Info
    val text:String?=null,
    val imageUri:String?=null,
    val videoUri:String?=null,
    val senderId:String?=null,
    val messageSentTimeInTimeStamp:Long?=null,
    val messageSendTimeInText:String?=null,
    var seenStatus:String?=null,
    val infoMessageType:String?=null,
    val addedOrRemovedUserId:String?=null,
    val sendTimeUsers:List<String>?=null,
    val messageReceivedByUsers:List<String>?=null,
    val messageSeenByUsers:List<String>?=null ,
    val newMembers:List<String>?=null,
    val newMembersName: List<String>?=null ,
    val senderUserName:String?=null
)
