package com.aditya.socialguru.data_layer.model.notification.payload


import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.google.gson.annotations.SerializedName

data class NotificationDataPayload(
    @SerializedName("message")
    val message: NotificationMessage?
)