package com.aditya.socialguru.data_layer.model.notification.payload


import com.google.gson.annotations.SerializedName

data class Android(
    @SerializedName("priority")
    val priority: String?,
    @SerializedName("ttl")
    val ttl: String?
)