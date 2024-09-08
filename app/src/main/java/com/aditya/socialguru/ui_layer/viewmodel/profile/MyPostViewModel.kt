package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.PostListenerEmissionType
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.post.post_meta_data.CommentedUserPostModel
import com.aditya.socialguru.data_layer.model.post.post_meta_data.SavedUserPostModel
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.MyPostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MyPostViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagProfile = Constants.LogTag.Profile
    private val tagPost = Constants.LogTag.Post
    private val repository = MyPostRepository()


    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val jobList = mutableListOf<Job>()


    private val _myPost = MutableSharedFlow<Resource<List<UserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val myPost: SharedFlow<Resource<List<UserPostModel>>> get() = _myPost.asSharedFlow()

    private val _myLikedPost = MutableSharedFlow<Resource<List<UserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val myLikedPost get() = _myLikedPost.asSharedFlow()

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

    private val _commentedPost = MutableSharedFlow<Resource<List<CommentedUserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val commentedPost get() = _commentedPost.asSharedFlow()

    private val _savedPostList = MutableSharedFlow<Resource<List<SavedUserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val savedPostList get() = _savedPostList.asSharedFlow()


    //region:: Get My Post

    fun getMyPost(userId: String) {
        val job = viewModelScope.myLaunch {
            _myPost.tryEmit(Resource.Loading())
            MyLogger.v(tagProfile, msg = "Request sending ....")
            if (SoftwareManager.isNetworkAvailable(app)) {
                MyLogger.v(tagProfile, msg = "Network available !")
                repository.getMyPost(userId).onEach {
                    MyLogger.d(tagProfile, msg = it.userPostList, isJson = true)
                    _myPost.tryEmit(handelMyPost(it))
                }.launchIn(this)
            } else {
                MyLogger.v(tagProfile, msg = "Network not available !")
                _myPost.tryEmit(Resource.Error(message = "Internet not available ."))
            }
        }
        jobList.add(job)
    }

    private fun handelMyPost(postHandling: PostListenerEmissionType): Resource<List<UserPostModel>> {

        MyLogger.v(tagProfile, isFunctionCall = true)

        val userPostList =
            myPost.replayCache[0].data?.toMutableList() ?: mutableListOf<UserPostModel>()

        when (postHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = "This is starting my post type "
                )
                postHandling.userPostList?.let {
                    userPostList.addAll(it.toMutableList() as ArrayList<UserPostModel>)
                    userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed my post type")

                postHandling.userPostModel?.postId?.let { postId ->
                    userPostList.forEach { temp ->
                        val currentPostId = temp.post?.postId
                        if (currentPostId != null) {
                            if (currentPostId == postId) {
                                userPostList.remove(temp)
                                userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                                return@let
                            }
                        }
                    }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                MyLogger.v(tagProfile, msg = "My Post Modify event come !")

                postHandling.userPostModel?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEachIndexed { index, userPost ->
                            if (userPost.post?.postId == postId) {
                                userPostList[index].post = userPostList[index].post?.copy(
                                    likeCount = post.likeCount,
                                    commentCount = post.commentCount,
                                    likedUserList = post.likedUserList
                                )
                                userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                                return@let
                            }
                        }
                    }
                }
                MyLogger.d(tagProfile, msg = userPostList, isJson = true)

            }

            else -> {}
        }

        return Resource.Success(userPostList.toList())
    }

    //endregion

    //region:: Get My Liked Post

    fun getMyLikedPost(userId: String) {
        val job = viewModelScope.myLaunch {
            _myLikedPost.tryEmit(Resource.Loading())
            MyLogger.v(tagProfile, msg = "Request sending ....")
            if (SoftwareManager.isNetworkAvailable(app)) {
                MyLogger.v(tagProfile, msg = "Network available !")
                repository.getMyLikedPost(userId).onEach {
                    _myLikedPost.tryEmit(handelMyLikedPost(it))
                }.launchIn(this)
            } else {
                MyLogger.v(tagProfile, msg = "Network not available !")
                _myLikedPost.tryEmit(Resource.Error(message = "Internet not available ."))
            }
        }
        jobList.add(job)
    }

    private suspend fun handelMyLikedPost(postHandling: ListenerEmissionType<UserPostModel, UserPostModel>): Resource<List<UserPostModel>> {

        MyLogger.v(tagProfile, isFunctionCall = true)

        val userPostList =
            myLikedPost.replayCache[0].data?.toMutableList() ?: mutableListOf<UserPostModel>()

        when (postHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = "This is starting my liked post type :- ${postHandling.responseList}"
                )
                postHandling.responseList?.let {
                    userPostList.addAll(it.toMutableList() as ArrayList<UserPostModel>)
                    userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                MyLogger.v(tagProfile, msg = "My Liked Post Modify event come !")

                postHandling.singleResponse?.post?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEachIndexed { index, userPost ->
                            if (userPost.post?.postId == postId) {
                                userPostList[index].post = userPostList[index].post?.copy(
                                    likeCount = post.likeCount,
                                    commentCount = post.commentCount,
                                    likedUserList = post.likedUserList
                                )
                                userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                                return@let
                            }
                        }
                    }
                    MyLogger.d(tagProfile, msg = userPostList, isJson = true)
                }

            }

            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagProfile,
                    msg = "This is added post type :- ${postHandling.singleResponse}"
                )
                postHandling.singleResponse?.let {
                    userPostList.add(it)
                    userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed my liked post type")

                postHandling.singleResponse?.post?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEach { temp ->
                            val currentPostId = temp.post?.postId
                            if (currentPostId != null) {
                                if (currentPostId == postId) {
                                    userPostList.remove(temp)
                                    userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                                    return@let
                                }
                            }

                        }
                    }
                }
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

    //region:: Commented Post


    fun getCommentedPost(userId: String) {
        val job = viewModelScope.myLaunch {
            _commentedPost.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.listenCommentedPost(userId).onEach {
                    _commentedPost.tryEmit(handleCommentedPostResponse(it))
                }.launchIn(this)
            } else {
                _commentedPost.tryEmit(Resource.Error("No Internet Available !"))
            }
        }
    }

    private fun handleCommentedPostResponse(response: List<ListenerEmissionType<CommentedUserPostModel, CommentedUserPostModel>>): Resource<List<CommentedUserPostModel>> {

        val updatedCommentedPost =
            commentedPost.replayCache[0].data?.toMutableList() ?: mutableListOf()

        response.forEach {
            when (it.emitChangeType) {
                Constants.ListenerEmitType.Starting -> {}
/*                Constants.ListenerEmitType.Added -> {
                    updatedCommentedPost.add(it.singleResponse!!)
                    updatedCommentedPost.sortByDescending { it.commentModel?.updatedCommentdPostTimeInTimeStamp }
                }*/

                Constants.ListenerEmitType.Removed -> {
                    MyLogger.d(tagPost, msg = it.singleResponse, isJson = true , jsonTitle = "Removal Post Event Come ")
                    val removePostId = it.singleResponse?.commentModel?.postId
                    removePostId ?: return@forEach
                    val removeItem =
                        updatedCommentedPost.find { it.commentModel?.postId == removePostId }
                    if (removeItem != null) {
                        updatedCommentedPost.remove(removeItem)
                    }
                }

                else -> {
                    val updatedPostId = it.singleResponse?.commentModel?.postId
                    updatedPostId ?: return@forEach
                    val updatedItem =
                        updatedCommentedPost.find { it.commentModel?.postId == updatedPostId }
                    if (updatedItem != null) {
                        MyLogger.d(tagPost, msg = it.singleResponse, isJson = true , jsonTitle = "Modify Post Event Come ")
                        updatedItem.apply {
                            commentModel = it.singleResponse.commentModel
                            userPostModel = it.singleResponse.userPostModel
                        }
                    }else{
                        MyLogger.d(tagPost, msg = it.singleResponse, isJson = true , jsonTitle = "Add Post Event Come ")
                        updatedCommentedPost.add(it.singleResponse)
                    }
                }
            }
        }
        updatedCommentedPost.sortByDescending { it.commentModel?.updatedCommentdPostTimeInTimeStamp }


        return Resource.Success(updatedCommentedPost)
    }

    //endregion

    //region:: My Saved Post


    fun getMySavedPost() {
        val job = viewModelScope.myLaunch {
            _savedPostList.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.listenMySavedPostForScreenView().onEach {
                    _savedPostList.tryEmit(handleMySavedPostResponse(it))
                }.launchIn(this)
            } else {
                _savedPostList.tryEmit(Resource.Error("No Internet Available !"))
            }
        }
    }

    private fun handleMySavedPostResponse(response: List<ListenerEmissionType<SavedUserPostModel, SavedUserPostModel>>): Resource<List<SavedUserPostModel>> {
        val updatedSavedPostList =
            savedPostList.replayCache[0].data?.toMutableList() ?: mutableListOf()

        response.forEach {
            when(it.emitChangeType){
                Constants.ListenerEmitType.Starting -> {}
                Constants.ListenerEmitType.Added -> {
                    updatedSavedPostList.add(it.singleResponse!!)
                    updatedSavedPostList.sortByDescending { it.savedPost?.savedPostTimeInTimeStamp}
                }
                Constants.ListenerEmitType.Removed -> {
                    val removePostId = it.singleResponse?.userPostModel?.post?.postId
                    removePostId ?: return@forEach
                    val removeItem = updatedSavedPostList.find { it.savedPost?.postId == removePostId }
                    if (removeItem!=null){
                        updatedSavedPostList.remove(removeItem)
                        updatedSavedPostList.sortByDescending { it.savedPost?.savedPostTimeInTimeStamp}
                    }
                }
                Constants.ListenerEmitType.Modify ->{
                    val updatedPostId = it.singleResponse?.savedPost?.postId
                    updatedPostId ?: return@forEach
                    val updatedItem = updatedSavedPostList.find { it.savedPost?.postId == updatedPostId }
                    if (updatedItem!=null){
                        val updatedIemIndex = updatedSavedPostList.indexOf(updatedItem)
                        updatedSavedPostList[updatedIemIndex] = updatedSavedPostList[updatedIemIndex].copy(
                            savedPost  =updatedItem.savedPost,
                            userPostModel = updatedItem.userPostModel
                        )
                        updatedSavedPostList.sortByDescending { it.savedPost?.savedPostTimeInTimeStamp}
                    }
                }
            }
        }

        return Resource.Success(updatedSavedPostList)
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