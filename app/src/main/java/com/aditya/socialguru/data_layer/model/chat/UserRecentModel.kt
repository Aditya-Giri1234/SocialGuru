package com.aditya.socialguru.data_layer.model.chat

import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo

data class UserRecentModel(
    val user: User?=null,
    var recentChat: RecentChat?=null ,
    var groupInfo:GroupInfo?=null
)