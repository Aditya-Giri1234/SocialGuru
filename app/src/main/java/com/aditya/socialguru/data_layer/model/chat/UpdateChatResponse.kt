package com.aditya.socialguru.data_layer.model.chat

import com.aditya.socialguru.data_layer.model.Resource

data class UpdateChatResponse(
    val isSending:Boolean=false,
    val sendingMessage:String?=null,
    val progress:Int?=null,
    val isSuccess: Boolean=false,
    val errorMessage:String?=null
)