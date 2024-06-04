package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.EditProfileRepository
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EditProfileViewModel(val app: Application) : AndroidViewModel(app) {

    private val repository = EditProfileRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

    private val _userUpdateStatus = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64, BufferOverflow.DROP_OLDEST
    )
    val userUpdateStatus: SharedFlow<Resource<UpdateResponse>> = _userUpdateStatus


    private val _user = MutableSharedFlow<Resource<User>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val user: SharedFlow<Resource<User>> get() = _user.asSharedFlow()

    fun updateProfile(user: User,oldImage:String?=null,newImage:String?=null) = viewModelScope.launch {
        _userUpdateStatus.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)) {

            repository.updateUser(user,oldImage,newImage).onEach {
                if (it.isSuccess) {
                    _userUpdateStatus.tryEmit(Resource.Success(it))
                } else {
                    _userUpdateStatus.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)


        } else {
            _userUpdateStatus.tryEmit(Resource.Error("No Internet Available !"))
        }
    }


    fun getUser()=viewModelScope.launch {
        _user.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)){

            AuthManager.currentUserId()?.let {
                repository.getUser(it).onEach {
                    MyLogger.i(Constants.LogTag.Profile, msg = "User details get ! - ${it.data}")
                    _user.tryEmit(it)
                }.launchIn(this)
            } ?: run {
                _user.tryEmit(Resource.Error("User Not found !"))
            }


        }else{
            _user.tryEmit(Resource.Error("Network Not Available !"))
        }
    }


}