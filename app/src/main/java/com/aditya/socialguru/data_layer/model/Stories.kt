package com.aditya.socialguru.data_layer.model

import android.net.Uri
import com.aditya.socialguru.domain_layer.helper.Constants

data class Stories(
    val type: Constants.StoryType,
    val id: Int? = null,
    val userName: String? = null,
    val profileImage:String?=null,
    val image: Uri? = null,
    val storyCount: Int? = null,
    val timeStamp:Long?=null
)
