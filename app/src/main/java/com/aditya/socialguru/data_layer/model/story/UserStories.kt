package com.aditya.socialguru.data_layer.model.story

import com.aditya.socialguru.data_layer.model.User
import java.io.Serializable

data class UserStories(
    val user: User?=null,
    val stories: MutableList<Stories>?=null
) : Serializable
