package com.aditya.socialguru.data_layer.model.post.post_meta_data

import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.post.comment.CommentedPost

data class CommentedUserPostModel(
    val commentModel: CommentedPost?=null,
    val userPostModel: UserPostModel?= null

)
