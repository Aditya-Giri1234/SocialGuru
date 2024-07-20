package com.aditya.socialguru.data_layer.model.chat

import com.google.firebase.database.PropertyName
import retrofit2.http.Field

data class LastMessage(
    val senderId:String?=null,
    val receiverId:String?=null,
    val chatType:String?=null,  // Text or Image Or Video
    val message:String?=null,
    val unSeenMessageCount:Int?=null,
    val lastMessageSentTimeInTimeStamp:Long?=null,
    val lastMessageSentTimeInText:String?=null,
    @field:JvmField
    var isUser1Online:Boolean?=null,  // User 1 and 2 determine which userId sorting , who come first that is user 1
    @field:JvmField
    var isUser2Online:Boolean?=null,

    //This is help to determine the type of message
    val messageType:String?=null,  // Message or Date or Info

)
