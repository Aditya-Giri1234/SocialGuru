package com.aditya.socialguru.ui_layer.viewmodel.profile.friend_circle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.friend_circle.FriendRepository
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FriendViewModel(val app: Application) : AndroidViewModel(app) {


    private val tagProfile = Constants.LogTag.Profile
    private val repository = FriendRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val _followerList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val followerList get() = _followerList.asSharedFlow()

    private val _friendRequestList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val friendRequestList get() = _friendRequestList.asSharedFlow()


    private val _removeFollower =
        MutableSharedFlow<Resource<UpdateResponse>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val removeFollower get() = _removeFollower.asSharedFlow()

    private val _followingList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val followingList get() = _followingList.asSharedFlow()


    private val _unFollow =
        MutableSharedFlow<Resource<UpdateResponse>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val unFollow get() = _unFollow.asSharedFlow()

    private val _friendList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val friendList get() = _friendList.asSharedFlow()


    private val _removeFriend =
        MutableSharedFlow<Resource<UpdateResponse>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val removeFriend get() = _removeFriend.asSharedFlow()

    private val _acceptFriendRequest = MutableSharedFlow<Resource<UpdateResponse>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val acceptFriendRequest get() = _acceptFriendRequest.asSharedFlow()

    private val _deleteFriendRequest = MutableSharedFlow<Resource<UpdateResponse>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val deleteFriendRequest get() = _deleteFriendRequest.asSharedFlow()


    //region:: Follower Operation

    fun getFollowerAndListenChangeEvent() = viewModelScope.myLaunch{
        _followerList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFollowerAndListenChangeEvent().onEach {
                _followerList.tryEmit(handleFollowerResponse(it))
            }.launchIn(this)

        } else {
            _followerList.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleFollowerResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagProfile, isFunctionCall = true)

        val followerList =
            followerList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.responseList, isJson = true,
                    jsonTitle = "This is starting follower type"
                )
                followerList.clear()
                listenerHandling.responseList?.let {
                    followerList.addAll(it.toMutableList() as ArrayList<FriendCircleData>)
                    followerList.sortByDescending { it.timeStamp }
                }
            }


            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.singleResponse, isJson = true,
                    jsonTitle = "This is added follower type"
                )

                listenerHandling.singleResponse?.let {
                    followerList.add(it)
                    followerList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed follower type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        followerList.forEach { temp ->
                            if (temp.userId == userId) {
                                followerList.remove(temp)
                                followerList.sortBy { it.timeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(followerList.toList())
    }

    fun removeFollower(userId: String) = viewModelScope.myLaunch{
        _removeFollower.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.removeFollower(userId).onEach {
                if (it.isSuccess) {
                    _removeFollower.tryEmit(Resource.Success(it))
                } else {
                    _removeFollower.tryEmit(
                        Resource.Error(
                            giveMeErrorMessage(
                                "Removing Follower",
                                it.errorMessage.toString()
                            )
                        )
                    )
                }
            }.launchIn(this)
        } else {
            _removeFollower.tryEmit(Resource.Error("No Internet Available !"))
        }
    }


    //endregion

    //region:: Following Operation
    fun getFollowingListAndListenChange() = viewModelScope.myLaunch{
        _followingList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFollowingListAndListenChange().onEach {
                _followingList.tryEmit(handleFollowingResponse(it))
            }.launchIn(this)

        } else {
            _followingList.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleFollowingResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagProfile, isFunctionCall = true)

        val followingList =
            followingList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.responseList, isJson = true,
                    jsonTitle = "This is starting following type"
                )

                followingList.clear()
                listenerHandling.responseList?.let {
                    followingList.addAll(it.toMutableList() as ArrayList<FriendCircleData>)
                    followingList.sortByDescending { it.timeStamp }
                }
            }


            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.singleResponse, isJson = true,
                    jsonTitle = "This is added following type"
                )

                listenerHandling.singleResponse?.let {
                    followingList.add(it)
                    followingList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed following type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        followingList.forEach { temp ->
                            if (temp.userId == userId) {
                                followingList.remove(temp)
                                followingList.sortBy { it.timeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(followingList.toList())
    }

    fun unFollow(userId: String) = viewModelScope.myLaunch{
        _unFollow.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.unFollow(userId).onEach {
                if (it.isSuccess) {
                    _unFollow.tryEmit(Resource.Success(it))
                } else {
                    _unFollow.tryEmit(
                        Resource.Error(
                            giveMeErrorMessage(
                                "Un Follow",
                                it.errorMessage.toString()
                            )
                        )
                    )
                }
            }.launchIn(this)
        } else {
            _unFollow.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Friend Operation
    fun getFriendListAndListenChange() = viewModelScope.myLaunch{
        _friendList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFriendListAndListenChange().onEach {
                _friendList.tryEmit(handleFriendResponse(it))
            }.launchIn(this)

        } else {
            _friendList.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleFriendResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagProfile, isFunctionCall = true)

        val friendList =
            friendList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {


            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.responseList, isJson = true,
                    jsonTitle = "This is starting friend type"
                )

                friendList.clear()
                listenerHandling.responseList?.let {
                    friendList.addAll(it.toMutableList() as ArrayList<FriendCircleData>)
                    friendList.sortByDescending { it.timeStamp }
                }
            }


            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.singleResponse, isJson = true,
                    jsonTitle = "This is added friend type"
                )

                listenerHandling.singleResponse?.let {
                    friendList.add(it)
                    friendList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed friend type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        friendList.forEach { temp ->
                            if (temp.userId == userId) {
                                friendList.remove(temp)
                                friendList.sortBy { it.timeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(friendList.toList())
    }

    fun removeFriend(userId: String) = viewModelScope.myLaunch{
        _removeFriend.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.removeFriend(userId).onEach {
                if (it.isSuccess) {
                    _removeFriend.tryEmit(Resource.Success(it))
                } else {
                    _removeFriend.tryEmit(
                        Resource.Error(
                            giveMeErrorMessage(
                                "Removing Friend",
                                it.errorMessage.toString()
                            )
                        )
                    )
                }
            }.launchIn(this)
        } else {
            _removeFriend.tryEmit(Resource.Error("No Internet Available !"))
        }
    }


    //endregion

    //region:: Friend request operation

    fun getAndListenFriendRequestComeEvent() = viewModelScope.myLaunch{
        _friendRequestList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.listenFriendRequestComeEvent().onEach {
                _friendRequestList.tryEmit(handleFriendRequestResponse(it))
            }.launchIn(this)

        } else {
            _friendRequestList.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleFriendRequestResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagProfile, isFunctionCall = true)

        val friendRequestList =
            friendRequestList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {

                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.responseList, isJson = true,
                    jsonTitle = "This is starting friend request type"
                )

                friendRequestList.clear()
                listenerHandling.responseList?.let {
                    friendRequestList.addAll(it.toMutableList() as ArrayList<FriendCircleData>)
                    friendRequestList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Added -> {

                MyLogger.v(
                    tagProfile,
                    msg = listenerHandling.singleResponse, isJson = true,
                    jsonTitle = "This is added friend request type"
                )

                listenerHandling.singleResponse?.let {
                    friendRequestList.add(it)
                    friendRequestList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed friend type")

                listenerHandling.singleResponse?.let { friend ->
                    friend.userId?.let { userId ->
                        friendRequestList.forEach { temp ->
                            if (temp.userId == userId) {
                                friendRequestList.remove(temp)
                                friendRequestList.sortBy { it.timeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(friendRequestList.toList())
    }
    fun acceptFriendRequest(followedId: String) = viewModelScope.myLaunch{
        _acceptFriendRequest.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.acceptFriendRequest(
                AuthManager.currentUserId()!!, followedId
            ).onEach {
                if (it.isSuccess) {
                    _acceptFriendRequest.tryEmit(Resource.Success(it))
                } else {
                    _acceptFriendRequest.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)

        } else {
            _acceptFriendRequest.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun declineFriendRequest(followedId: String) = viewModelScope.myLaunch{
        _deleteFriendRequest.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.declineFriendRequest(AuthManager.currentUserId()!!, followedId).onEach {
                if (it.isSuccess) {
                    _deleteFriendRequest.tryEmit(Resource.Success(it))
                } else {
                    _deleteFriendRequest.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)

        } else {
            _deleteFriendRequest.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion


    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

}