package com.aditya.socialguru.domain_layer.remote_service.notification

import com.aditya.socialguru.data_layer.model.notification.NotificationResponse
import com.aditya.socialguru.data_layer.model.notification.payload.NotificationDataPayload
import com.aditya.socialguru.domain_layer.helper.Constants
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationService {


    // header must in function call parameter else give error
    @POST("v1/projects/socialguru-706ba/messages:send")
    suspend fun sendNotification(
        @Body message: NotificationDataPayload,
        @Header("authorization") token: String ,
        @Header("content-type") type:String="application/json"
    ): Response<NotificationResponse>
}