package com.aditya.socialguru.data_layer.model

sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Loading<T>() : Resource<T>()
    class Success<T>(data: T?=null,message: String?=null) : Resource<T>(data, message)
    class Error<T>(message: String?) : Resource<T>( message=message)
}