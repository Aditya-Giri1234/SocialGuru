package com.aditya.socialguru.data_layer.shared_model

import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.domain_layer.helper.Constants

data class ListenerEmissionType<T,R>(
    val emitChangeType: Constants.ListenerEmitType,
    val responseList: List<T>? = null,
    val singleResponse: R? = null,
)
