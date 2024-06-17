package com.aditya.socialguru.ui_layer.viewmodel.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.PostListenerEmissionType
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.post.FollowingPostRepository
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FollowingPostViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagPost = Constants.LogTag.Post

    private val repository = FollowingPostRepository()

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


    //region:: Get Discover Post

    fun getDiscoverPost() = viewModelScope.myLaunch {
        _userPost.tryEmit(Resource.Loading())
        MyLogger.v(tagPost, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagPost, msg = "Network available !")
            repository.getFollowingPost().onEach {
                MyLogger.d(tagPost, msg = it.userPostList, isJson = true)
                _userPost.tryEmit(handleGetDiscover(it))
            }.launchIn(this)
        } else {
            MyLogger.v(tagPost, msg = "Network not available !")
            _userPost.tryEmit(Resource.Error(message = "Internet not available ."))
        }
    }

    private suspend fun handleGetDiscover(discoverPostHandling: PostListenerEmissionType): Resource<List<UserPostModel>> {

        MyLogger.v(tagPost, isFunctionCall = true)

        val userPostList = userPost.replayCache[0].data?.toMutableList() ?: mutableListOf<UserPostModel>()

        when (discoverPostHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagPost,
                    msg = "This is starting story type :- ${discoverPostHandling.userPostList}"
                )
                discoverPostHandling.userPostList?.let {
                    userPostList.addAll(it.toMutableList() as ArrayList<UserPostModel>)
                    userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagPost,
                    msg = "This is added post type :- ${discoverPostHandling.userPostModel}"
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
                    MyLogger.d(tagPost, msg = userPostList, isJson = true)
                }
                MyLogger.d(tagPost, msg = userPostList, isJson = true)

            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagPost, msg = "This is removed post type")

                discoverPostHandling.userPostModel?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEach { userPost ->
                            val currentPostId=userPost.post?.postId
                            if (currentPostId != null&& currentPostId==postId) {
                                userPostList.remove(userPost)
                                return@let
                            }
                        }
                    }
                    MyLogger.d(tagPost, msg = userPostList, isJson = true)
                }
                MyLogger.d(tagPost, msg = userPostList, isJson = true)

            }

            Constants.ListenerEmitType.Modify -> {
                // For Future
                discoverPostHandling.userPostModel?.let { post ->
                    post.postId?.let { postId ->
                        userPostList.forEach { userPost ->
                            val currentPostId=userPost.post?.postId
                            if (currentPostId != null&& currentPostId==postId) {
                                userPost.post=userPost.post?.copy(
                                    commentCount=post.commentCount,
                                    likeCount = post.likeCount ,
                                    likedUserList=post.likedUserList
                                )
                                return@let
                            }
                        }
                    }
                    MyLogger.d(tagPost, msg = userPostList, isJson = true)
                }
                MyLogger.d(tagPost, msg = userPostList, isJson = true)
            }
        }

        return Resource.Success(userPostList.toList())
    }

    //endregion

    // region:: Update like post count

     fun updateLikeCount(postId: String ,postCreatorUserId:String, isLiked: Boolean) = viewModelScope.myLaunch {
         _likePost.tryEmit(Resource.Loading())
         if (SoftwareManager.isNetworkAvailable(app)){
             repository.updateLikeCount(postId,postCreatorUserId,isLiked).onEach {
                 if (it.isSuccess){
                     _likePost.tryEmit(Resource.Success(it))
                 }else{
                     _likePost.tryEmit(Resource.Error("Some error occurred !"))
                 }

             }.launchIn(this)
         }else{
             _likePost.tryEmit(Resource.Error("No Internet Available !"))
         }
     }

    //endregion

    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }

}