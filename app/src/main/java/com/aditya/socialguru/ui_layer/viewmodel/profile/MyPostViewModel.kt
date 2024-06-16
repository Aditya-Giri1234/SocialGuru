package com.aditya.socialguru.ui_layer.viewmodel.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.PostListenerEmissionType
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.profile.MyPostRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MyPostViewModel(val app:Application) : AndroidViewModel(app) {

    private val tagProfile=Constants.LogTag.Profile
    private val repository=MyPostRepository()


    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded



    private val _myPost = MutableSharedFlow<Resource<List<UserPostModel>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val myPost: SharedFlow<Resource<List<UserPostModel>>> get() = _myPost.asSharedFlow()


    //region:: Get Discover Post

    fun getMyPost(userId:String) = viewModelScope.myLaunch {
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

    private suspend fun handelMyPost(postHandling: PostListenerEmissionType): Resource<List<UserPostModel>> {

        MyLogger.v(tagProfile, isFunctionCall = true)

        val userPostList = myPost.replayCache[0].data?.toMutableList() ?: mutableListOf<UserPostModel>()

        when (postHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = "This is starting story type :- ${postHandling.userPostList}"
                )
                postHandling.userPostList?.let {
                    userPostList.addAll(it.toMutableList() as ArrayList<UserPostModel>)
                    userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                MyLogger.v(tagProfile, msg = "Post Modify event come !")

                postHandling.userPostModel?.let { post ->
                    post.userId?.let { userId ->
                        userPostList.forEachIndexed { index, userPost ->
                            if (userPost.user?.userId == userId) {
                                userPostList[index].post=userPostList[index].post?.copy(
                                    likeCount = post.likeCount,
                                    commentCount = post.commentCount
                                )
                                userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }
                                return@let
                            }
                        }
                    }
                    MyLogger.d(tagProfile, msg = userPostList, isJson = true)
                }
                MyLogger.d(tagProfile, msg = userPostList, isJson = true)

            }
            else->{}
        }

        return Resource.Success(userPostList.toList())
    }

    //endregion


    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }

}