package com.aditya.socialguru.domain_layer.service

import android.app.Application
import com.google.firebase.FirebaseApp
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider

class MyApplication :Application(){

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        EmojiManager.install(GoogleEmojiProvider())
    }

}