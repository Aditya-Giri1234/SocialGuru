package com.aditya.socialguru.data_layer.model.post

import com.aditya.socialguru.data_layer.model.User

data class UserPostModel(
    val user: User?=null,
    val post:Post?=null
)