package com.aditya.socialguru.data_layer.model.notification

import com.aditya.socialguru.domain_layer.helper.Constants

data class NotificationData(
    val type: String?=null,
    val friendOrFollowerId:String?=null,
    val postId:String?=null,
    val notificationTimeInText:String?=null,
    val notificationTimeInTimeStamp:String?=null,
    val notificationId:String?=null,
    val chatRoomId:String?=null,
    val messageId:String?=null,


    //For Group Chat
    @field:JvmField
    val isGroupMessage:String?=null // Data in string when sending to fcm notification

)