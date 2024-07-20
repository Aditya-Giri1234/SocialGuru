package com.aditya.socialguru.data_layer.model.chat.group

data class GroupMember(
   val  memberId:String ?=null ,
   @field:JvmField
   val isOnline:Boolean ?=null ,
)
