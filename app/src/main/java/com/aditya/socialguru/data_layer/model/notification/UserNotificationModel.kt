package com.aditya.socialguru.data_layer.model.notification

import com.aditya.socialguru.data_layer.model.User

data class UserNotificationModel(
    val user: User,
    val notificationData: NotificationData
)
