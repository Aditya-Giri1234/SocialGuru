package com.aditya.socialguru.ui_layer.viewmodel.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.PostListenerEmissionType
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.post.FollowingPostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FollowingPostViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagPost = Constants.LogTag.Post

    private val repository = FollowingPostRepository()
    private var followingUserPostJob: Job? = null
    private var followingUserListJob:Job?=null

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded


    private val _userPost = MutableSharedFlow<Resource<List<UserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userPost: SharedFlow<Resource<List<UserPostModel>>> get() = _userPost.asSharedFlow()

    private val _likePost = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val likePost get() = _likePost.asSharedFlow()

    private val _savePost = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val savePost get() = _savePost.asSharedFlow()

    private val _followingList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val followingList get() = _followingList.asSharedFlow()


    //region:: Get Following Post

    fun getFollowingPost(userIds: List<String>) {
        followingUserPostJob?.cancel()

        followingUserPostJob = viewModelScope.myLaunch {
            _userPost.tryEmit(Resource.Loading())
            MyLogger.v(tagPost, msg = "Request sending ....")
            if (SoftwareManager.isNetworkAvailable(app)) {
                MyLogger.v(tagPost, msg = "Network available !")
                repository.getFollowingPost(userIds).onEach {
                    MyLogger.d(tagPost, msg = it.userPostList, isJson = true)
                    _userPost.tryEmit(handleGetFollowing(it))
                }.launchIn(this)
            } else {
                MyLogger.v(tagPost, msg = "Network not available !")
                _userPost.tryEmit(Resource.Error(message = "Internet not available ."))
            }
        }
    }

    private suspend fun handleGetFollowing(discoverPostHandling: PostListenerEmissionType): Resource<List<UserPostModel>> {

        MyLogger.v(tagPost, isFunctionCall = true)

        val userPostList =
            userPost.replayCache[0].data?.toMutableList() ?: mutableListOf<UserPostModel>()

        when (discoverPostHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                // Don't Do anything
            }

            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagPost,
                    msg = "This is added post type "
                )
                discoverPostHandling.userPostModel?.let { post ->
                    post.userId?.let { userId ->
                        userPostList.add(
                            UserPostModel(
                                repository.getUser(userId).first().data,
                                post
                            )
                        )
                        userPostList.sortByDescending {
                            it.post?.postUploadingTimeInTimeStamp
                        }
                    }
                }
                MyLogger.d(tagPost, msg = userPostList, isJson = true)

            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagPost, msg = "This is removed post type")

                discoverPostHandling.userPostModel?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEach { userPost ->
                            val currentPostId = userPost.post?.postId
                            if (currentPostId != null && currentPostId == postId) {
                                userPostList.remove(userPost)
                                return@let
                            }
                        }
                    }
                }
                MyLogger.d(tagPost, msg = userPostList, isJson = true)

            }

            Constants.ListenerEmitType.Modify -> {
                // For Future
                MyLogger.v(tagPost, msg = "This modify post type ..")
                discoverPostHandling.userPostModel?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEach { userPost ->
                            val currentPostId = userPost.post?.postId
                            if (currentPostId != null && currentPostId == postId) {
                                userPost.post = userPost.post?.copy(
                                    commentCount = post.commentCount,
                                    likeCount = post.likeCount,
                                    likedUserList = post.likedUserList
                                )
                                return@let
                            }
                        }
                    }
                }
                MyLogger.d(tagPost, msg = userPostList, isJson = true)
            }
        }

        return Resource.Success(userPostList.toList())
    }

    //endregion

    // region:: Update like post count

    fun updateLikeCount(postId: String, postCreatorUserId: String, isLiked: Boolean) =
        viewModelScope.myLaunch {
            _likePost.tryEmit(Resource.Loading())
            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.updateLikeCount(postId, postCreatorUserId, isLiked).onEach {
                    if (it.isSuccess) {
                        _likePost.tryEmit(Resource.Success(it))
                    } else {
                        _likePost.tryEmit(Resource.Error("Some error occurred !"))
                    }

                }.launchIn(this)
            } else {
                _likePost.tryEmit(Resource.Error("No Internet Available !"))
            }
        }

    //endregion

    //region:: Update post saved state

    fun updatePostSaveStatus(postId: String) =
        viewModelScope.myLaunch {
            _savePost.tryEmit(Resource.Loading())
            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.updatePostSaveStatus(postId).onEach {
                    if (it.isSuccess) {
                        _savePost.tryEmit(Resource.Success(it))
                    } else {
                        _savePost.tryEmit(Resource.Error("Some error occurred !"))
                    }
                }.launchIn(this)
            } else {
                _savePost.tryEmit(Resource.Error("No Internet Available !"))
            }
        }

    //endregion

    //region:: Following Operation

    fun getFollowingListAndListenChange() {
        followingUserListJob?.cancel()
        followingUserListJob = viewModelScope.myLaunch {
            _followingList.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.getFollowingListAndListenChange().onEach {
                    _followingList.tryEmit(handleFollowingResponse(it))
                }.launchIn(this)

            } else {
                _followingList.tryEmit(Resource.Error("No Internet Available !"))
            }
        }
    }

    private fun handleFollowingResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagPost, isFunctionCall = true)

        val followingList =
            followingList.replayCache[0].data?.toMutableList()
                ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagPost,
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
                    tagPost,
                    msg = listenerHandling.singleResponse, isJson = true,
                    jsonTitle = "This is added following type"
                )

                listenerHandling.singleResponse?.let {
                    followingList.add(it)
                    followingList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagPost, msg = "This is removed following type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        followingList.forEach { temp ->
                            if (temp.userId == userId) {
                                followingList.remove(temp)
                                followingList.sortByDescending { it.timeStamp }
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

    //endregion

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }
    fun removeListener(){
        _isDataLoaded=false
        followingUserListJob?.cancel()
    }

}