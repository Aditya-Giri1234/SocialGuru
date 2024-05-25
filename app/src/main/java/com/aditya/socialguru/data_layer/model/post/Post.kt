package com.aditya.socialguru.data_layer.model.post

import com.aditya.socialguru.domain_layer.helper.Constants

data class Post(
    val postId:String?=null, //post id made by [Post_userId_uuid]
    val commentId:String?=null, // this comment id made by [Post_Comment_postId]
    val userId:String?=null,
    val postType: String?=null,
    val text:String?=null,
    val imageUrl:String?=null,
    val videoUrl:String?=null ,
    val postUploadingTimeInText:String?=null ,
    val postUploadingTimeInTimeStamp:Long?=null,
    val commentCount:Int?=null,
    val likeCount:Int?=null
)
