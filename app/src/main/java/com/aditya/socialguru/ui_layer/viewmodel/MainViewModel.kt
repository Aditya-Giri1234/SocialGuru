package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.MainRepository
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(val app:Application) : AndroidViewModel(app) {
    val repository=MainRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val _user = MutableSharedFlow<Resource<User>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val user: SharedFlow<Resource<User>> get() = _user.asSharedFlow()

    fun getUser()=viewModelScope.launch {
        _user.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)){

            AuthManager.currentUserId()?.let {
                repository.getUser(it).onEach {
                    _user.tryEmit(it)
                }.launchIn(this)
            } ?: run {
                _user.tryEmit(Resource.Error("User Not found !"))
            }


        }else{
            _user.tryEmit(Resource.Error("Network Not Available !"))
        }
    }

    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }
}