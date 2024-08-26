package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.SettingRepo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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


}