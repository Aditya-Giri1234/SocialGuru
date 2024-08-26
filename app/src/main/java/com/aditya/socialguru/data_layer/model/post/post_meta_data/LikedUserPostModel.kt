package com.aditya.socialguru.data_layer.model.post.post_meta_data

import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.post.comment.CommentedPost

data class LikedUserPostModel(
    val likedPostModel: LikedPostModel?=null,
    val userPostModel: UserPostModel?= null
)
