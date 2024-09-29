package com.aditya.socialguru.domain_layer.remote_service

interface DeleteAccountPasswordResult {
    fun onResult(isSuccess:Boolean , errorMessage :String?)
}