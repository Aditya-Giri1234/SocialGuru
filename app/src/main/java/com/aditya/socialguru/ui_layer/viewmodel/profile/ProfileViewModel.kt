package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.model.user_action.UserRelationshipStatus
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.ProfileRepository
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(val app: Application) : AndroidViewModel(app) {

    val repository = ProfileRepository()

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
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userSignOut: SharedFlow<Resource<UpdateResponse>> get() = _userSignOut.asSharedFlow()

    private val _userDetails = MutableSharedFlow<Resource<User>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userDetails: SharedFlow<Resource<User>> get() = _userDetails.asSharedFlow()

    private val _followUser = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val followUser get() = _followUser.asSharedFlow()

    private val _unFollow = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val unFollow get() = _unFollow.asSharedFlow()

    private val _sendFriendRequest = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val sendFriendRequest get() = _sendFriendRequest.asSharedFlow()

    private val _acceptFriendRequest = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val acceptFriendRequest get() = _acceptFriendRequest.asSharedFlow()

    private val _deleteFriendRequest = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val deleteFriendRequest get() = _deleteFriendRequest.asSharedFlow()

    private val _removeFriend = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val removeFriend get() = _removeFriend.asSharedFlow()

    private val _userRelationshipStatus = MutableSharedFlow<Resource<UserRelationshipStatus>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userRelationshipStatus get() = _userRelationshipStatus.asSharedFlow()

    private val _userRelationshipStatusUpdate = MutableSharedFlow<Resource<UpdateResponse>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userRelationshipStatusUpdate get() = _userRelationshipStatusUpdate.asSharedFlow()

    private val _findUsers = MutableSharedFlow<Resource<List<FriendCircleData>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val findUsers get() = _findUsers.asSharedFlow()


    fun subscribeToFollowerCount(userId: String) = viewModelScope.myLaunch{
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.subscribeToFollowerCount(userId).onEach {
                _followerCount.tryEmit(it)
            }.launchIn(this)
        }else{
            _followerCount.tryEmit(Int.MIN_VALUE)
        }
    }

    fun subscribeToFollowingCount(userId: String) = viewModelScope.myLaunch{
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.subscribeToFollowingCount(userId).onEach {
                _followingCount.tryEmit(it)
            }.launchIn(this)
        }else{
            _followingCount.tryEmit(Int.MIN_VALUE)
        }
    }

    fun subscribeToPostCount(userId: String) = viewModelScope.myLaunch{
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.subscribeToPostCount(userId).onEach {
                _postCount.tryEmit(it)
            }.launchIn(this)
        }else{
            _postCount.tryEmit(Int.MIN_VALUE)
        }
    }

    fun subscribeToLikeCount(userId: String) = viewModelScope.myLaunch{
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.subscribeToLikeCount(userId).onEach {
                _likeCount.tryEmit(it)
            }.launchIn(this)
        }else{
            _likeCount.tryEmit(Int.MIN_VALUE)
        }
    }


    fun singOutUser() = viewModelScope.myLaunch{
        _userSignOut.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.signOut().first() {
                _userSignOut.tryEmit(Resource.Success(it))
            }
        } else {
            _userSignOut.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun followUser(followedId: String) = viewModelScope.myLaunch{
        _followUser.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.followUser(AuthManager.currentUserId()!!, followedId).first() {
                if (it.isSuccess) {
                    _followUser.tryEmit(Resource.Success(it))
                } else {
                    _followUser.tryEmit(Resource.Error(it.errorMessage))
                }
            }

        } else {
            _followUser.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun unFollow(followedId: String) = viewModelScope.myLaunch{
        _unFollow.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.unFollow(followedId).first() {
                if (it.isSuccess) {
                    _unFollow.tryEmit(Resource.Success(it))
                } else {
                    _unFollow.tryEmit(Resource.Error(it.errorMessage))
                }
            }

        } else {
            _unFollow.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun sendFriendRequest(followedId: String) = viewModelScope.myLaunch{
        _sendFriendRequest.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.sendFriendRequest(AuthManager.currentUserId()!!, followedId).first() {
                if (it.isSuccess) {
                    _sendFriendRequest.tryEmit(Resource.Success(it))
                } else {
                    _sendFriendRequest.tryEmit(Resource.Error(it.errorMessage))
                }
            }

        } else {
            _sendFriendRequest.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun acceptFriendRequest(followedId: String) = viewModelScope.myLaunch{
        _acceptFriendRequest.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.acceptFriendRequest(
                AuthManager.currentUserId()!!, followedId
            ).first() {
                if (it.isSuccess) {
                    _acceptFriendRequest.tryEmit(Resource.Success(it))
                } else {
                    _acceptFriendRequest.tryEmit(Resource.Error(it.errorMessage))
                }
            }

        } else {
            _acceptFriendRequest.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun deleteFriendRequest(followedId: String) = viewModelScope.myLaunch{
        _deleteFriendRequest.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.deleteFriendRequest(AuthManager.currentUserId()!!, followedId).first() {
                if (it.isSuccess) {
                    _deleteFriendRequest.tryEmit(Resource.Success(it))
                } else {
                    _deleteFriendRequest.tryEmit(Resource.Error(it.errorMessage))
                }
            }

        } else {
            _deleteFriendRequest.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun removeFriend(followedId: String) = viewModelScope.myLaunch{
        _removeFriend.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.removeFriend(followedId).first() {
                if (it.isSuccess) {
                    _removeFriend.tryEmit(Resource.Success(it))
                } else {
                    _removeFriend.tryEmit(Resource.Error(it.errorMessage))
                }
            }

        } else {
            _removeFriend.tryEmit(Resource.Error("No Internet Available !"))
        }
    }



    fun getUserRelationshipStatus(followedId: String) = viewModelScope.myLaunch{
        _userRelationshipStatus.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getUserRelationshipStatus(AuthManager.currentUserId()!!, followedId).onEach {
                _userRelationshipStatus.tryEmit(Resource.Success(it))
            }.launchIn(this)

        } else {
            _userRelationshipStatus.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun listenUserRelationStatus(followedId: String) = viewModelScope.myLaunch{
        _userRelationshipStatusUpdate.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.listenUserRelationStatus(followedId).onEach() {
                if (it.isSuccess) {
                    _userRelationshipStatusUpdate.tryEmit(Resource.Success(it))
                }else{
                    _userRelationshipStatusUpdate.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)

        } else {
            // Do Nothing
            _userRelationshipStatusUpdate.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    fun getUser(userId: String) = viewModelScope.myLaunch{
        _userDetails.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getUser(userId).onEach {
                MyLogger.v(Constants.LogTag.Profile, msg = it.data, isJson = true)
                _userDetails.tryEmit(it)
            }.launchIn(this)
        } else {
            _userDetails.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }

    }

    fun findUser(query: String) = viewModelScope.myLaunch {
        _findUsers.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.findUser(query).onEach {
                _findUsers.tryEmit(Resource.Success(it))
            }.launchIn(this)
        }else{
            _findUsers.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    override fun onCleared() {
        MyLogger.e(msg = "Profile View Model is cleared !")
        super.onCleared()
    }

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

}