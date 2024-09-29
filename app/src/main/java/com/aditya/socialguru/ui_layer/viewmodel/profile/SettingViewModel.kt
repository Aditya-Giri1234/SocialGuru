package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.UserSetting
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.SettingRepo
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingViewModel(val app :Application) : AndroidViewModel(app) {

    private val repository = SettingRepo()

    private val _deleteAccount = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val deleteAccount: SharedFlow<Resource<UpdateResponse>> get() = _deleteAccount.asSharedFlow()

    private val _settingUpdate = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val settingUpdate: SharedFlow<Resource<UpdateResponse>> get() = _settingUpdate.asSharedFlow()


    private val _resetPassword = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val resetPassword: SharedFlow<Resource<UpdateResponse>> get() = _resetPassword.asSharedFlow()

    private val _changePassword = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val changePassword: SharedFlow<Resource<UpdateResponse>> get() = _changePassword.asSharedFlow()

    private val _updateEmail = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val updateEmail: SharedFlow<Resource<UpdateResponse>> get() = _updateEmail.asSharedFlow()

    fun deleteAccount() = viewModelScope.launch {
        _deleteAccount.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.deleteAccount().onEach {
                if (it.isSuccess){
                    _deleteAccount.tryEmit(Resource.Success(it))
                }else{
                    _deleteAccount.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        }else{
            _deleteAccount.tryEmit(Resource.Error("No Internet Connection"))
        }
    }

    fun updateUserSetting(userSetting: UserSetting) = viewModelScope.launch {
        _settingUpdate.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.updateUserSetting(userSetting).onEach {
                if (it.isSuccess){
                    _settingUpdate.tryEmit(Resource.Success(it))
                }else{
                    _settingUpdate.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        }else{
            _settingUpdate.tryEmit(Resource.Error("No Internet Connection"))
        }
    }

    fun resetPassword(email:String?) = viewModelScope.myLaunch {
        _resetPassword.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.sendPasswordResetEmail(email).first().let {
                if (it.isSuccess){
                    _resetPassword.tryEmit(Resource.Success(it))
                }else{
                    _resetPassword.tryEmit(Resource.Error(it.errorMessage))
                }
            }
        }else{
            _resetPassword.tryEmit(Resource.Error("No Internet Available !"))
        }
    }
    fun changePassword(currentPassword:String, newPassword:String) = viewModelScope.myLaunch {
        _changePassword.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.updateUserPassword(currentPassword , newPassword).first().let {
                if (it.isSuccess){
                    _changePassword.tryEmit(Resource.Success(it))
                }else{
                    _changePassword.tryEmit(Resource.Error(it.errorMessage))
                }
            }
        }else{
            _changePassword.tryEmit(Resource.Error("No Internet Available !"))
        }
    }
    fun updateEmail(newEmailId:String, password:String) = viewModelScope.myLaunch {
        _updateEmail.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.updateUserEmailId(newEmailId , password).first().let {
                if (it.isSuccess){
                    _updateEmail.tryEmit(Resource.Success(it))
                }else{
                    _updateEmail.tryEmit(Resource.Error(it.errorMessage))
                }
            }
        }else{
            _updateEmail.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

}