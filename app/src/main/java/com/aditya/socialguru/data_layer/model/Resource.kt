package com.aditya.socialguru.data_layer.model

sealed class Resource<T>(open val data: T? = null, open val message: String? = null) {
    class Loading<T>() : Resource<T>()
    data class Success<T>(override val data: T?=null, override val message: String?=null) : Resource<T>(data, message){
        var hasBeenMessagedToUser = false
    }
    data class Error<T>(override val message: String?) : Resource<T>( message=message){
        var hasBeenMessagedToUser = false
    }
}