package com.aditya.socialguru.data_layer.model.post

data class Comment(
    val commentId:String?=null, // this made by [Comment_userId_uuId]
    val userId:String?=null,  // This user who comment on the post
    val postCommentId:String?=null, // This comment on which post
    val postId:String?=null, // This is real post id which help to increase comment count,
    val postCreatorId:String?=null, // This id hold creator of post
    val commentType:String?=null,  // Text , image, Video
    val text:String?=null,
    val imageUri:String?=null,
    val videoUri:String?=null,
    val userName:String?=null,
    val commentUploadingTimeInTimestamp:Long?=null,
    val commentUploadingTimeInText:String?=null
)
