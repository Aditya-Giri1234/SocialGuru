package com.aditya.socialguru.domain_layer.repository.notification

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class NotificationRepo {
    suspend fun getMyNotificationAndListen() = FirebaseManager.getMyNotificationAndListen()
    suspend fun deleteSingleNotification(notificationId:String)= FirebaseManager.deleteSingleNotification(notificationId)
    suspend fun deleteAllNotification()= FirebaseManager.deleteAllNotification()
}