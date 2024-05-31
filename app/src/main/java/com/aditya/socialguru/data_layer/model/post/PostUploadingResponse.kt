package com.aditya.socialguru.data_layer.model.post

import com.aditya.socialguru.domain_layer.helper.Constants

data class PostUploadingResponse(
    val state: Constants.PostUploadState,
    val progress:Int?=0,
    val errorMessage: String? = null

)