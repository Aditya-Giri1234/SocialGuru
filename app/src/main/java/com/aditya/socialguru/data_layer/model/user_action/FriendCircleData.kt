package com.aditya.socialguru.data_layer.model.user_action

import com.aditya.socialguru.data_layer.model.User


data class  FriendCircleData(
    val userId:String?=null,
    val timeStamp:Long?=null,
    var user: User?=null
)