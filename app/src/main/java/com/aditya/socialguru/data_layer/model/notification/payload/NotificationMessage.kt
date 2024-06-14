package com.aditya.socialguru.data_layer.model.notification.payload

import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.google.gson.annotations.SerializedName

data class NotificationMessage(
    @SerializedName("token")
    val token: String?,
    @SerializedName("data")
    val data: NotificationData?,
    @SerializedName("android")
    val android: Android?
)
