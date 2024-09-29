package com.aditya.socialguru.ui_layer.viewmodel.comment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.UpdateChatResponse
import com.aditya.socialguru.data_layer.model.post.Comment
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.comment.CommentRepo
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.CommentManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CommentViewModel(val app: Application) : AndroidViewModel(app) {

    private val tagComment = Constants.LogTag.Comment

    private val repository = CommentRepo()

    private val jobList = mutableListOf<Job>()

    private var _isDataLoaded = false

    val isDataLoaded get() = _isDataLoaded

    private val _sendComment =
        MutableSharedFlow<Resource<UpdateChatResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val sendComment get() = _sendComment.asSharedFlow()

    private val _deleteComment =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val deleteComment get() = _deleteComment.asSharedFlow()

    private val _getComments =
        MutableSharedFlow<Resource<List<Comment>>>(1, 64, BufferOverflow.DROP_OLDEST)
    val getComments get() = _getComments.asSharedFlow()

    private val _getCommenters =
        MutableSharedFlow<Resource<List<User>>>(1, 64, BufferOverflow.DROP_OLDEST)
    val getCommenters get() = _getCommenters.asSharedFlow()


    private var _imageUri: Uri? = null
    val imageUri get() = _imageUri

    private var _videoUri: Uri? = null
    val videoUri get() = _videoUri


    fun setImageUriData(uri: Uri?) {
        _imageUri = uri
    }

    fun setVideoUriData(uri: Uri?) {
        _videoUri = uri
    }




    //region:: Send Comment

    fun sendMessage(
        comment: Comment, isCreatorOnline: Boolean, onSend: () -> Unit
    ) = viewModelScope.myLaunch {
        val isImagePresent = comment.imageUri != null
        val isVideoPresent = comment.videoUri != null

        if (SoftwareManager.isNetworkAvailable(app)) {
            if (isVideoPresent || isImagePresent) {
                _sendComment.tryEmit(Resource.Loading())
            } else {
                onSend.invoke()
            }
            repository.sendComment(comment, isCreatorOnline).onEach {
                MyLogger.i(
                    tagComment, msg = it, isJson = true, jsonTitle = "Message sent response come"

                )
                if (it.isSuccess || it.isSending) {
                    _sendComment.tryEmit(Resource.Success(it))
                } else {
                    _sendComment.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _sendComment.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Delete Comment

    fun deleteMessage(
        comment: Comment
    ) = viewModelScope.myLaunch {
        _deleteComment.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.deleteComment(
                comment
            ).onEach {
                MyLogger.i(
                    tagComment, msg = it, isJson = true, jsonTitle = "Delete message response come"
                )
                if (it.isSuccess) {
                    _deleteComment.tryEmit(Resource.Success(it))
                } else {
                    _deleteComment.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _deleteComment.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Get Comments

    fun getComments(postId: String) {
        val job =viewModelScope.myLaunch {
            _getComments.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.getCommentAndListen(postId).onEach {
                    MyLogger.v(
                        tagComment,
                        msg = it,
                        isJson = true,
                        jsonTitle = "Comment response come"
                    )
                    _getComments.tryEmit(handleCommentResponse(it))
                }.launchIn(this)
            } else {
                _getComments.tryEmit(Resource.Error("No Internet Available !"))
            }
        }

        jobList.add(job)
    }

    private fun handleCommentResponse(
        response: List<ListenerEmissionType<Comment, Comment>>): Resource<List<Comment>> {

        val commentList = getComments.replayCache[0].data?.toMutableList() ?: mutableListOf()

        response.forEach {
            when (it.emitChangeType) {
                Constants.ListenerEmitType.Starting -> {
                   // Don't do any thing here
                }

                Constants.ListenerEmitType.Added -> {
                    it.singleResponse?.let {
                        commentList.add(it)
                        commentList.sortByDescending { it.commentUploadingTimeInTimestamp }
                    }
                }

                Constants.ListenerEmitType.Removed -> {
                    it.singleResponse?.commentId?.let { commentId ->
                        val removeComment = commentList.find { it.commentId == commentId }

                        if (removeComment!=null){
                            commentList.remove(removeComment)
                            commentList.sortByDescending { it.commentUploadingTimeInTimestamp }
                        }
                    }
                }

                Constants.ListenerEmitType.Modify -> {
                   // Don't do anything
                }
            }
        }



        return Resource.Success(commentList)

    }

    //endregion

    //region:: Get Commenters

    fun getCommenters(postId: String){
        val job = viewModelScope.myLaunch {
            _getCommenters.tryEmit(Resource.Loading())

            if(SoftwareManager.isNetworkAvailable(app)){
                repository.getCommentersAndListen(postId).onEach {
                    _getCommenters.tryEmit(handleCommentersResponse(it))
                }.launchIn(this)
            }else{
                _getCommenters.tryEmit(Resource.Error("No Internet Available !"))
            }
        }
        jobList.add(job)
    }

    private fun handleCommentersResponse(response: List<ListenerEmissionType<User, User>>): Resource.Success<List<User>> {
        val commentersList = getCommenters.replayCache[0].data?.toMutableList() ?: mutableListOf()

        response.forEach {
            when (it.emitChangeType) {
                Constants.ListenerEmitType.Starting -> {
                    // Don't do any thing here
                }

                Constants.ListenerEmitType.Added -> {
                    it.singleResponse?.let {
                        commentersList.add(it)
                    }
                }

                Constants.ListenerEmitType.Removed -> {
                    it.singleResponse?.userId?.let { userId ->
                        val removeCommenter = commentersList.find { it.userId == userId }

                        if (removeCommenter!=null){
                            commentersList.remove(removeCommenter)
                        }
                    }
                }

                Constants.ListenerEmitType.Modify -> {
                    // Don't do anything
                }
            }
        }



        return Resource.Success(commentersList)
    }

    //endregion

    //region:: Update Online Status Of Creator

     fun updateMyOnlineStatus(postId: String, status:Boolean,post: Post?=null) = viewModelScope.myLaunch {
         repository.updateMyOnlineStatus(postId,status ,post)
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


    override fun onCleared() {
        MyLogger.w(tagComment, msg = "Comment ViewModel cleared for hashcode :- ${this.hashCode()}")
        super.onCleared()
    }

}