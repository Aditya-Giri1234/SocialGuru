package com.aditya.socialguru.data_layer.model.chat.group

import com.aditya.socialguru.data_layer.model.User


data class GroupMemberDetails(
    var member:GroupMember,
    val memberInfo: User
)