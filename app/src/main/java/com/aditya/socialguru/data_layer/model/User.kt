package com.aditya.socialguru.data_layer.model

import java.io.Serializable

data class User(
    val userId:String?=null,
    val userName:String?=null,
    val userBio:String?=null,
    val userProfession:String?=null,
    val userEmailId:String?=null,
    val userPassword:String?=null,
    val userProfileImage:String?=null,
    val loginTimeInText:String?=null,
    val loginTimeInTimeStamp:Long?=null,
    val logoutTimeInText:String?=null,
    val logoutTimeInTimeStamp:Long?=null,
    val fcmToken:String?=null,
    val postCount:Int?=null,
    val userAvailable:Boolean?=false,
    var userSetting:UserSetting?=null
) : Serializable


data class UserSetting(
    @field:JvmField
    val isStoryRemoveAfter24HourActive:Boolean?=false,
    @field:JvmField
    val isMyOnlineStatusHideEnable:Boolean ?=false ,
    @field : JvmField
    val isSingleChatNotificationMute:Boolean?=false,
    @field : JvmField
    val isGroupChatNotificationMute:Boolean?=false,
    @field : JvmField
    val isPostNotificationMute:Boolean?=false,
    @field : JvmField
    val isFriendCircleNotificationMute:Boolean?=false,
) : Serializable