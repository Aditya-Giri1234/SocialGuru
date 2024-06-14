package com.aditya.socialguru.domain_layer.helper

import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.notification.NotificationService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private var _retrofit: Retrofit? = null

    private fun initializeRetrofit(): Retrofit {
        MyLogger.i(msg = "Retrofit is now reassign !")
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client =
            OkHttpClient.Builder().apply {
                connectTimeout(30, TimeUnit.SECONDS)
                writeTimeout(600, TimeUnit.SECONDS)
                readTimeout(1, TimeUnit.MINUTES)
                retryOnConnectionFailure(true)
                addInterceptor(logging)
            }.build()
        return Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).client(client).build()
    }

    private fun getRetrofitInstance(): Retrofit {
        if (_retrofit == null) {
            synchronized(this) {
                if (_retrofit == null) {
                    _retrofit = initializeRetrofit()
                }
            }
        }
        return _retrofit!!
    }

    private fun resetRetrofitInstance() {
        synchronized(this) {
            _retrofit = null
        }
    }
    val notificationApi: NotificationService get() = getRetrofitInstance().create(NotificationService::class.java)
}