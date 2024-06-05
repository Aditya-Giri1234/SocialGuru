package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.ProfileRepository
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(val app: Application) : AndroidViewModel(app) {

    val repository=ProfileRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded


    private val _followerCount = MutableSharedFlow<Int>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val followerCount: SharedFlow<Int> get() = _followerCount.asSharedFlow()

    private val _followingCount = MutableSharedFlow<Int>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val followingCount: SharedFlow<Int> get() = _followingCount.asSharedFlow()

    private val _postCount = MutableSharedFlow<Int>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val postCount: SharedFlow<Int> get() = _postCount.asSharedFlow()

    private val _likeCount = MutableSharedFlow<Int>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val likeCount: SharedFlow<Int> get() = _likeCount.asSharedFlow()


    private val _userSignOut = MutableSharedFlow<Resource<UpdateResponse>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userSignOut: SharedFlow<Resource<UpdateResponse>> get() = _userSignOut.asSharedFlow()


     fun subscribeToFollowerCount(userId: String) = viewModelScope.launch {
         repository.subscribeToFollowerCount(userId).onEach {
             _followerCount.tryEmit(it)
         }.launchIn(this)
     }

     fun subscribeToFollowingCount(userId: String) = viewModelScope.launch {
         repository.subscribeToFollowingCount(userId).onEach {
             _followingCount.tryEmit(it)
         }.launchIn(this)
     }

     fun subscribeToPostCount(userId: String) = viewModelScope.launch {
         repository.subscribeToPostCount(userId).onEach {
             _postCount.tryEmit(it)
         }.launchIn(this)
     }

     fun subscribeToLikeCount(userId: String) = viewModelScope.launch {
         repository.subscribeToLikeCount(userId).onEach {
             _likeCount.tryEmit(it)
         }.launchIn(this)
     }


    fun singOutUser()=viewModelScope.launch {
        _userSignOut.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)){
            _userSignOut.tryEmit(Resource.Success(repository.signOut()))
        }else{
            _userSignOut.tryEmit(Resource.Error("No Internet Available !"))
        }
    }



    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }

}