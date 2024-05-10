package com.aditya.socialguru.domain_layer.repository

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.domain_layer.remote_service.AuthRepository
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl : AuthRepository {


    override suspend fun login(email: String, password: String): Pair<String?,String?> = FirebaseManager.loginUser(email,password)

    override suspend fun signUp(user: User): Pair<FirebaseUser?,String?> = FirebaseManager.createUser(user)

    override suspend fun saveUserToDatabase(user: User): Pair<Boolean, String?> =FirebaseManager.saveUser(user)
    override suspend fun getUser(userId: String): Flow<Resource<User>> = FirebaseManager.getUserById(userId)
}