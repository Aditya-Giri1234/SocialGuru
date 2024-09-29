package com.aditya.socialguru.data_layer.model.chat

data class ChatMuteNotificationData(
    val userId:String?=null,
    val muteTimeInTimeStamp:Long?=null,
    val muteTimeInText:String?=null
)
