package com.aditya.socialguru.ui_layer.viewmodel.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.post.DetailPostRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DetailPostViewModel(val app:Application) : AndroidViewModel(app) {

    private val tagPost = Constants.LogTag.Post

    private val repository=DetailPostRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val _postDetail=MutableSharedFlow<Resource<UserPostModel>>(
        1,64,BufferOverflow.DROP_OLDEST
    )
    val postDetail get() = _postDetail.asSharedFlow()

    fun getPostById(postId:String)= viewModelScope.myLaunch{
        _postDetail.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.getPostById(postId).onEach {
                it.singleResponse?.let {
                    _postDetail.tryEmit(Resource.Success(it))
                }
            }.launchIn(this)
        }else{
            _postDetail.tryEmit(Resource.Error("No Internet Available !"))
        }
    }


    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

}