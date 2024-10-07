package com.aditya.socialguru.ui_layer.viewmodel.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.post.DiscoverPostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DiscoverPostViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagPost = Constants.LogTag.Post

    private val repository = DiscoverPostRepository()

    private var discoverUserPostListJob: Job? = null

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded


    private val _userPost = MutableSharedFlow<Resource<List<UserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userPost get() = _userPost.asSharedFlow()

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


    //region:: Get Discover Post

     fun getDiscoverPost() {
          discoverUserPostListJob = viewModelScope.myLaunch {
             _userPost.tryEmit(Resource.Loading())
             MyLogger.v(tagPost, msg = "Request sending ....")
             if (SoftwareManager.isNetworkAvailable(app)) {
                 MyLogger.v(tagPost, msg = "Network available !")
                 repository.getDiscoverPost().onEach {
                     _userPost.tryEmit(handleGetDiscover(it))
                 }.launchIn(this)
             } else {
                 MyLogger.v(tagPost, msg = "Network not available !")
                 _userPost.tryEmit(Resource.Error(message = Constants.ErrorMessage.InternetNotAvailable.message))
             }
         }
     }

    private suspend fun handleGetDiscover(response: List<ListenerEmissionType<UserPostModel, UserPostModel>>): Resource<List<UserPostModel>> {

        MyLogger.v(tagPost, isFunctionCall = true)

        val userPostList =
            userPost.replayCache[0].data?.toMutableList() ?: mutableListOf<UserPostModel>()

        response.forEach {
            when (it.emitChangeType) {
                Constants.ListenerEmitType.Starting -> {
                    // Don't do any thing here
                }

                Constants.ListenerEmitType.Added -> {
                    it.singleResponse?.let {
                        userPostList.add(it)
                    }
                }

                Constants.ListenerEmitType.Removed -> {
                    it.singleResponse?.post?.postId?.let { postId ->
                        val removePost = userPostList.find { it.post?.postId == postId }

                        if (removePost != null) {
                            userPostList.remove(removePost)
                        }
                    }
                }

                Constants.ListenerEmitType.Modify -> {
                    // Don't do anything
                    it.singleResponse?.post?.postId?.let { postId ->
                        val modifyPost = userPostList.find { it.post?.postId == postId }
                        modifyPost?.apply {
                            this.post = it.singleResponse.post
                        }
                    }
                }
            }
        }

        userPostList.sortByDescending {
            it.post?.postUploadingTimeInTimeStamp
        }
        MyLogger.v(
            Constants.LogTag.JobManager,
            msg = userPostList.map { it.post },
            jsonTitle = "Post List After Sort !",
            isJson = true)
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

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

    fun removeListener() {
        _isDataLoaded = false
        discoverUserPostListJob?.cancel()
    }

}