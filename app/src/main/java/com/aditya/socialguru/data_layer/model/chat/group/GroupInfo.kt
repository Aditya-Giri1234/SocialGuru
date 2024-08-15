package com.aditya.socialguru.data_layer.model.chat.group

import java.io.Serializable


data class GroupInfo(
    val chatRoomId:String?=null,
    val groupName:String?=null,
    val groupDescription:String?=null,
    val groupPic:String?=null,
    val groupCreationDateInTimeStamp:Long?=null,
    val groupCreationDateInString:String?=null,
    val groupAdmins:List<String>?=null,
    val creatorId:String?=null,
) : Serializable
