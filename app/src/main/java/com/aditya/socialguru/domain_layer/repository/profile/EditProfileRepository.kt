package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.domain_layer.service.FirebaseManager

class EditProfileRepository {

    suspend fun updateUser(user: User,oldImage:String?=null,newImage:String?=null)=FirebaseManager.updateUser(user,oldImage,newImage)

}