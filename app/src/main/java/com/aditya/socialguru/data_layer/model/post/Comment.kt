package com.aditya.socialguru.data_layer.model.post

data class Comment(
    val commentId:String?=null, // this made by [Comment_userId_uuId]
    val userId:String?=null,  // This user who comment on the post
    val postCommentId:String?=null, // This comment on which post
    val postId:String?=null, // This is real post id which help to increase comment count
    val commentType:String?=null,  // Text , image, gif
    val text:String?=null,
    val image:String?=null
)
