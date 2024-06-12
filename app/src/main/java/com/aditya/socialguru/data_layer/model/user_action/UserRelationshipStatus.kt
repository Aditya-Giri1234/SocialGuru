package com.aditya.socialguru.data_layer.model.user_action

import com.aditya.socialguru.domain_layer.helper.Constants

data class UserRelationshipStatus(
    val isFollowing: Boolean,
    val friendStatus: Constants.FriendStatus
)
