package com.aditya.socialguru.domain_layer.manager

import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.model.notification.payload.Android
import com.aditya.socialguru.data_layer.model.notification.payload.NotificationDataPayload
import com.aditya.socialguru.data_layer.model.notification.payload.NotificationMessage
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationSendingManager {
    private val tagNotification = Constants.LogTag.Notification

    suspend fun sendNewFollowerNotification(token: String, data: NotificationData) {
        MyLogger.d(tagNotification, msg = "Token :- $token and data :-> $data")
        val payload = NotificationDataPayload(
            NotificationMessage(
                token = token, data = data, android = Android(
                    Constants.NotificationPriority.High.priority, "2000s"
                )
            )

        )


        CoroutineScope(Dispatchers.Default).launch {
            val accessToken = FCMTokenManager.getAccessToken()
            val result =
                RetrofitInstance.notificationApi.sendNotification(payload, "Bearer $accessToken")
            MyLogger.v(
                tagNotification,
                msg = result.body(),
                isJson = true,
                jsonTitle = "Follower notification sending response"
            )
        }

    }

}