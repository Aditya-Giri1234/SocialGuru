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
        val gson=Gson()
        val data=gson.toJson(message.data)
        val notificationData= Gson().fromJson(data,NotificationData::class.java)
       val (title, notificatioMessage) =when(notificationData.type?.name){
            Constants.NotificationType.NEW_FOLLOWER.name->{
                Pair("New Follower" , "You got new follower !")
            }
            Constants.NotificationType.FRIEND_REQUEST_COME.name->{
                Pair("Friend Request" , "You got new friend request !")
            }
            Constants.NotificationType.ACCEPT_FRIEND_REQUEST.name->{
                Pair("Accept Friend Request" , "Your friend request accepted !")
            }
           else->{
               Pair("Others" , "You got messsage")
           }
        }
        showNotification(this@MyFirebaseMessagingService,title,notificatioMessage)
        MyLogger.v(tagNotification, msg = message.toString(), isJson = true, jsonTitle = "Notification Received ")
    }

    private fun showNotification(context: Context, title: String, message: String) {
        // Create a notification builder

        val name = "UserAction"
        val descriptionText = "Channel for user actions"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("user_action_channel", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, "user_action_channel")
            .setSmallIcon(R.drawable.app_icon) // Replace with your notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MyFirebaseMessagingService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, builder.build()) // Use a unique ID for each notification
        }
    }

}