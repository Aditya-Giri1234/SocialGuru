package com.aditya.socialguru.domain_layer.helper

import android.content.Context
import com.aditya.socialguru.AppConfig
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.notification.NotificationService
import com.aditya.socialguru.domain_layer.remote_service.update.UpdateService
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private var _retrofit: Retrofit? = null
    private var _retrofitGit:Retrofit?=null

    private fun logInterceptor(): Interceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        if (AppConfig.LOG_CAN_SHOW) {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            // disable retrofit log on release
            loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        return loggingInterceptor
    }

    private fun initializeRetrofit(): Retrofit {
        MyLogger.i(msg = "Retrofit is now reassign !")
        val client =
            OkHttpClient.Builder().apply {
                connectTimeout(30, TimeUnit.SECONDS)
                writeTimeout(600, TimeUnit.SECONDS)
                readTimeout(1, TimeUnit.MINUTES)
                retryOnConnectionFailure(true)
                addNetworkInterceptor(logInterceptor())
            }.build()
        return Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).client(client).build()
    }

    private fun headerInterceptor(packageName: String): Interceptor {
        return Interceptor {
            val original = it.request()
            val request = original.newBuilder()
                .header("User-Agent", packageName)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .method(original.method, original.body)
                .build()
            it.proceed(request)
        }
    }
    private fun initializeRetrofitGit(packageName: String): Retrofit {
        MyLogger.i(msg = "Retrofit Git is now reassign !")
        val gson = GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()
        val client =
            OkHttpClient.Builder().apply {
                connectTimeout(30, TimeUnit.SECONDS)
                writeTimeout(600, TimeUnit.SECONDS)
                readTimeout(1, TimeUnit.MINUTES)
                retryOnConnectionFailure(true)
                addNetworkInterceptor(logInterceptor())
                addInterceptor(headerInterceptor(packageName))
            }.build()
        return Retrofit.Builder().baseUrl(Constants.RETROFIT_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)).client(client).build()
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

    private fun getRetrofitInstanceForGit(packageName: String): Retrofit {
        if (_retrofitGit == null) {
            synchronized(this) {
                if (_retrofitGit == null) {
                    _retrofitGit = initializeRetrofitGit(packageName)
                }
            }
        }
        return _retrofitGit!!
    }



    private fun resetRetrofitInstance() {
        synchronized(this) {
            _retrofit = null
        }
    }
    val notificationApi: NotificationService get() = getRetrofitInstance().create(NotificationService::class.java)

    fun updateService(packageName: String): UpdateService = getRetrofitInstanceForGit(packageName).create(UpdateService::class.java)
}