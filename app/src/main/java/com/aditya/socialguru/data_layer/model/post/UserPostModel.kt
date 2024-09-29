package com.aditya.socialguru.data_layer.model.post

import com.aditya.socialguru.data_layer.model.User

data class UserPostModel(
    var user: User?=null,
    var post:Post?=null
)