package com.aditya.socialguru.data_layer.model.post

import com.aditya.socialguru.domain_layer.helper.Constants

data class PostListenerEmissionType(
    val emitChangeType: Constants.PostEmitType,
    val userPostList: List<UserPostModel>? = null,
    val userPostModel: Post? = null,
)
