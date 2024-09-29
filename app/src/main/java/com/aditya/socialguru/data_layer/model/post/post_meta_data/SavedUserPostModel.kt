package com.aditya.socialguru.data_layer.model.post.post_meta_data

import com.aditya.socialguru.data_layer.model.post.UserPostModel

data class SavedUserPostModel(
    val savedPost : SavedPostModel?=null,
    val userPostModel: UserPostModel?=null
)
