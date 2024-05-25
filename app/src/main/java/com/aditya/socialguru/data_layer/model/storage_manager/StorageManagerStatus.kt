package com.aditya.socialguru.data_layer.model.storage_manager

import com.aditya.socialguru.domain_layer.helper.Constants

data class StorageManagerStatus(
    val state:Constants.StorageManagerState,
    val url:String?=null,
    val error:String?=null,
    val  progress:Int=0
)