package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.FCMTokenManager
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.AuthRepositoryImpl
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagLogin = Constants.LogTag.LogIn

    private val _signUpStatus = MutableStateFlow<Resource<User>?>(null)
    val signUpStatus: StateFlow<Resource<User>?> get() = _signUpStatus

    private val _loginStatus = MutableStateFlow<Resource<User>?>(null)
    val loginStatus: StateFlow<Resource<User>?> get() = _loginStatus


    private val repositoryImpl = AuthRepositoryImpl()


    fun createUser(user: User) = viewModelScope.myLaunch {
        _signUpStatus.value = Resource.Loading()
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagLogin, msg = "Internet Available !")
            val result = repositoryImpl.signUp(user = user)
            _signUpStatus.value = handleCreateUserResponse(user, result)
        } else {
            MyLogger.v(tagLogin, msg = "Internet Not Available !")
            _signUpStatus.value = Resource.Error("No Internet  Available!")
        }
    }

    private suspend fun handleCreateUserResponse(
        user: User,
        result: Pair<FirebaseUser?, String?>
    ): Resource<User>? {
        return if (result.first == null) {
            Resource.Error(result.second ?: "Some error occurred during signUp !")
        } else {
            val fcmToken: String? = FCMTokenManager.generateToken().first()
            val saveUser = User(
                result.first!!.uid,
                user.userName,
                user.userBio,
                user.userProfession,
                user.userEmailId,
                user.userPassword, fcmToken = fcmToken
            )
            val saveResult = repositoryImpl.saveUserToDatabase(saveUser)
            if (saveResult.first) {
                Resource.Success(saveUser)
            } else {
                Resource.Error(result.second ?: "Some error occurred during signUp !")
            }
        }
    }

    fun loginUser(email: String, password: String) = viewModelScope.myLaunch {
        _loginStatus.value = Resource.Loading()
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagLogin, msg = "Internet available !")
            val result = repositoryImpl.login(email, password)
            _loginStatus.value = handleLoginResponse(result)
        } else {
            MyLogger.e(tagLogin, msg = "Internet not available!")
            _loginStatus.value = Resource.Error("No Internet  Available!")
        }
    }

    private suspend fun handleLoginResponse(result: Pair<String?, String?>): Resource<User>? {
        MyLogger.v(tagLogin, isFunctionCall = true)
        return if (result.first == null) {
            Resource.Error(result.second ?: "Some error occurred during signUp !")
        } else {
            val fcmToken: String? = FCMTokenManager.generateToken().first()
            repositoryImpl.setFcmToken(fcmToken)
            val user = repositoryImpl.getUser(result.first!!).first()
            MyLogger.d(tagLogin, msg = "User := $user && Data := ${user.data}")
            user
        }

    }


}