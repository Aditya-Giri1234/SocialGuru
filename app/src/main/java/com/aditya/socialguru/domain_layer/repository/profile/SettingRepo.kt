package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.data_layer.model.UserSetting
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class SettingRepo {
    suspend fun deleteAccount() = FirebaseManager.deleteAccount()
    suspend fun updateUserSetting(userSetting: UserSetting) = FirebaseManager.updateUserSetting(userSetting)

    suspend fun sendPasswordResetEmail(email:String?) = FirebaseManager.sendPasswordResetEmail(email)

    suspend fun updateUserEmailId(newEmailId: String, password: String) = FirebaseManager.updateUserEmailId(newEmailId , password)

    suspend fun updateUserPassword(currentPassword:String ,newPassword:String) = FirebaseManager.updateUserPassword(currentPassword , newPassword)
}