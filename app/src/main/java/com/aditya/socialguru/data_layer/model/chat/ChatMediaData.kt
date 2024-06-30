package com.aditya.socialguru.data_layer.model.chat

import com.google.firebase.encoders.annotations.Encodable.Field

data class ChatMediaData(
    val mediaId:String?=null,
    val mediaUri:String?=null,
    val mediaUploadingTimeInTimeStamp:Long?=null,
    val mediaUploadingTimeInText:String?=null,
    @field:JvmField
    val isImage:Boolean?=null,
    val date:String?=null // This is only for local
)
