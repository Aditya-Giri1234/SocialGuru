package com.aditya.socialguru.data_layer.model.story

import com.aditya.socialguru.data_layer.model.User

data class UserStories(
    val user: User?=null,
    val stories: List<Stories>?=null
)
