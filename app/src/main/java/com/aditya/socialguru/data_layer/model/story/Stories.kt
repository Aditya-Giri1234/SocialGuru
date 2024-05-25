package com.aditya.socialguru.data_layer.model.story

data class Stories(
    val userId: String? = null,
    val storyId: String? = null, // This is uuid [Story_userId_uuid]
    val storyUploadingTimeInText:String?=null,
    val storyUploadingTimeInTimeStamp: Long?=null ,
    val storyType:String?=null,
    val storyUri:String?=null,
    val text:String?=null,
    val textBackGroundColor:Int?=null,
    val textFontFamily:Int?=null,
    val storyViewCount: Int? = null,  // This is not use full so after some time removed it.
    val storySeenUsersId:List<String>?=null
)
