package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.domain_layer.service.FirebaseManager

class SettingRepo {
    suspend fun deleteAccount() = FirebaseManager.deleteAccount()
}