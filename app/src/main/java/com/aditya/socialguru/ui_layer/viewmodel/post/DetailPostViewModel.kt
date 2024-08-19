package com.aditya.socialguru.ui_layer.viewmodel.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.post.DetailPostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DetailPostViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagPost = Constants.LogTag.Post

    private val repository = DetailPostRepository()
    private val jobList: MutableList<Job> = mutableListOf()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val _postDetail = MutableSharedFlow<Resource<UserPostModel>>(
        1, 64, BufferOverflow.DROP_OLDEST
    )
    val postDetail get() = _postDetail.asSharedFlow()

    private val _userList = MutableSharedFlow<Resource<List<FriendCircleData>>>(
        1, 64, BufferOverflow.DROP_OLDEST
    )
    val userList get() = _userList.asSharedFlow()

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


    fun getPostById(postId: String) {
        val job = viewModelScope.myLaunch {
            _postDetail.tryEmit(Resource.Loading())
            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.getPostById(postId).onEach {
                    MyLogger.i(tagPost, msg = "Post event come in detail view model !")
                    it.singleResponse?.let {
                        _postDetail.tryEmit(Resource.Success(it))
                    }
                }.launchIn(this)
            } else {
                _postDetail.tryEmit(Resource.Error("No Internet Available !"))
            }
        }
        jobList.add(job)
    }

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


    // region:: Get User List In Post given post id

    fun getPostLikeUser(postId: String) {
        val job = viewModelScope.myLaunch {
            _userList.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.getPostLikeUser(postId).onEach {
                    MyLogger.v(tagPost, msg = it, isJson = true)
                    _userList.tryEmit(Resource.Success(handleLikePostUser(it)))
                }.launchIn(this)
            } else {
                _userList.tryEmit(Resource.Error("No Internet Available !"))
            }

        }
        jobList.add(job)

    }


    private fun handleLikePostUser(postLikedHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): List<FriendCircleData> {

        MyLogger.v(tagPost, isFunctionCall = true)

        val userList =
            userList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (postLikedHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagPost,
                    msg = "This is starting user like type "
                )
                postLikedHandling.responseList?.let {
                    userList.addAll(it)
                    userList.sortByDescending { it.timeStamp }
                }

            }

            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagPost,
                    msg = "This is added user like type "
                )

                postLikedHandling.singleResponse?.let {
                    userList.add(it)
                }

                MyLogger.d(tagPost, msg = userList, isJson = true)

            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagPost, msg = "This is removed user like type")

                postLikedHandling.singleResponse?.let { user ->
                    user.userId?.let { userId ->
                        userList.forEach { userPost ->
                            val currentPostId = userPost.user?.userId
                            if (currentPostId != null && currentPostId == userId) {
                                userList.remove(userPost)
                                return@let
                            }
                        }
                    }
                }
                MyLogger.d(tagPost, msg = userList, isJson = true)

            }

            Constants.ListenerEmitType.Modify -> {
                // For Future
            }
        }

        return userList.toList()
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

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

    fun removeAllListener() {
        _isDataLoaded = false
        jobList.forEach {
            it.cancel()
        }
    }

}