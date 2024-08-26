package com.aditya.socialguru.data_layer.model.post.post_meta_data

data class LikedPostModel(
    val postId:String?=null,
    val likedPostTimeInTimeStamp : Long?=null,
    val likedPostTimeInText : String?=null
)