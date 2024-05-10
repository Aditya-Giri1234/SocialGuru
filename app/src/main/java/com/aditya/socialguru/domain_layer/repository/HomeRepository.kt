package com.aditya.socialguru.domain_layer.repository

import android.net.Uri
import com.aditya.socialguru.domain_layer.service.FirebaseManager

class HomeRepository {

    fun storeImageInFirebaseStorage(image: Uri){
        FirebaseManager.uploadStory(image)
    }
}