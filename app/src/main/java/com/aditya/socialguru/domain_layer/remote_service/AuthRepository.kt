package com.aditya.socialguru.domain_layer.remote_service

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email:String, password:String):Pair<String?,String?>
    suspend fun signUp(user:User):Pair<FirebaseUser?,String?>
    suspend fun saveUserToDatabase(user: User):Pair<Boolean, String?>
    suspend fun getUser(userId:String): Flow<Resource<User>>
}