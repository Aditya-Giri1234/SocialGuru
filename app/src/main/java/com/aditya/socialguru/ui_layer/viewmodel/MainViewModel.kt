package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.FCMTokenManager
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.MainRepository
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(val app: Application) : AndroidViewModel(app) {
    val repository = MainRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val _user = MutableSharedFlow<Resource<User>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val user: SharedFlow<Resource<User>> get() = _user.asSharedFlow()

    private val _fcmToken= MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val fcmToken: SharedFlow<Resource<UpdateResponse>> get() = _fcmToken.asSharedFlow()



    fun getUser() = viewModelScope.myLaunch{
        _user.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {

            AuthManager.currentUserId()?.let {
                repository.subscribeToCurrentUser(it).onEach {

                    _user.tryEmit(
                        if (it == null) {
                            Resource.Error("No user current user found !")
                        } else {
                            Resource.Success(it)
                        }
                    )


                }.launchIn(this)
            } ?: run {
                _user.tryEmit(Resource.Error("User Not found !"))
            }


        } else {
            _user.tryEmit(Resource.Error("Network Not Available !"))
        }
    }

      fun setFcmToken(token: String) = viewModelScope.myLaunch{
          _fcmToken.tryEmit(Resource.Loading())

          if (SoftwareManager.isNetworkAvailable(app)) {

              repository.setFcmToken(token).onEach {
                  if (it.isSuccess){
                      _fcmToken.tryEmit(Resource.Success(it))
                  }else{
                      _fcmToken.tryEmit(Resource.Error(it.errorMessage))
                  }
              }.launchIn(this)
          } else {
              _fcmToken.tryEmit(Resource.Error("Network Not Available !"))
          }
      }
    fun updateUserAvailability(isUserAvailable:Boolean) = viewModelScope.myLaunch {
        repository.updateUserAvailability(isUserAvailable)
    }

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }
}