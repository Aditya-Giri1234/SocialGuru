package com.aditya.socialguru.domain_layer.remote_service

interface OnVideoStateChange {
    fun onLoad()
    fun onReady()

    fun onError()
    fun onComplete()
}