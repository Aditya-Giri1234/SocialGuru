package com.aditya.socialguru.data_layer.model.post

import com.aditya.socialguru.domain_layer.helper.Constants

data class Post(
    val postId:String?=null,
    val commentId:String?=null,
    val userId:String?=null,
    val postType: String?=null,
    val text:String?=null,
    val imageUrl:String?=null,
    val videoUrl:String?=null ,
    val uploadingTime:Long?=null
)
