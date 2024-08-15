package com.aditya.socialguru.data_layer.model.chat.group

import java.io.Serializable

data class GroupMembersList(
    val members:List<GroupMember>
): Serializable
