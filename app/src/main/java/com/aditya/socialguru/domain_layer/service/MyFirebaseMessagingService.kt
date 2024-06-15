package com.aditya.socialguru.domain_layer.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.json_event_handling.DataProcessor
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val tagNotification=Constants.LogTag.Notification
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        MyLogger.v(tagNotification, msg = message.data, isJson = true, jsonTitle = "Notification Received ")

        val gson=Gson()
        val data=gson.toJson(message.data)
        DataProcessor(this).handle(Constants.DataSource.FCM,data)
    }



}