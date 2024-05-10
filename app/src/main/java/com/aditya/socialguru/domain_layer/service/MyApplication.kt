package com.aditya.socialguru.domain_layer.service

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication :Application(){

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

}