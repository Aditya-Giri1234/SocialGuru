package com.aditya.socialguru.data_layer.model.chat

import com.aditya.socialguru.data_layer.model.User

data class UserRecentModel(
    val user: User?=null,
    var recentChat: RecentChat?=null
)