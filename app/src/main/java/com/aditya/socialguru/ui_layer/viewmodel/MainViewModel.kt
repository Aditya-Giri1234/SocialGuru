package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import android.app.DownloadManager
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.git.GitHubRelease
import com.aditya.socialguru.data_layer.model.post.post_meta_data.LikedPostModel
import com.aditya.socialguru.data_layer.model.post.post_meta_data.SavedPostModel
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.MainRepository
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import retrofit2.Response
import java.io.File

class MainViewModel(val app: Application) : AndroidViewModel(app) {
    val repository = MainRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private var _isListenerSet = false
    val isListenerSet get() = _isListenerSet

    private val jobList = mutableListOf<Job>()


    private val _user = MutableSharedFlow<Resource<User>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val user: SharedFlow<Resource<User>> get() = _user.asSharedFlow()

    // This variable not access by ui
    private val savedPost = mutableListOf<SavedPostModel>()
    private val likedPost = mutableListOf<LikedPostModel>()

    private val _fcmToken = MutableSharedFlow<Resource<UpdateResponse>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val fcmToken: SharedFlow<Resource<UpdateResponse>> get() = _fcmToken.asSharedFlow()

    private val _appUpdating = MutableSharedFlow<Resource<File?>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val appUpdating get() = _appUpdating.asSharedFlow()

    private val _appUpdate = MutableSharedFlow<Resource<GitHubRelease>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val appUpdate get() = _appUpdate.asSharedFlow()


    fun getUser() {
        val job = viewModelScope.myLaunch {
            _user.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {

                AuthManager.currentUserId()?.let {
                    repository.subscribeToCurrentUser(it).onEach {

                        if (it == null && AppBroadcastHelper.isAccountDeleted.first()) {
                            return@onEach
                        }
                        _user.tryEmit(
                            if (it == null) {
                                Resource.Error("No  current user found !")
                            } else {
                                Resource.Success(it)
                            }
                        )


                    }.launchIn(this)
                } ?: run {
                    _user.tryEmit(Resource.Error("User Not found !"))
                }


            } else {
                _user.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
            }
        }
        jobList.add(job)
    }

    fun listenMySavedPost() {
        val job = viewModelScope.myLaunch {
            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.listenMySavedPost().onEach {
                    handleSavedPostResponse(it)
                }.launchIn(this)
            }
        }
        jobList.add(job)
    }

    private fun handleSavedPostResponse(response: List<ListenerEmissionType<SavedPostModel, SavedPostModel>>) {

        response.forEach {
            when (it.emitChangeType) {
                Constants.ListenerEmitType.Starting -> {
                    // Don't do any thing here
                }

                Constants.ListenerEmitType.Added -> {
                    it.singleResponse?.let {
                        savedPost.add(it)
                    }
                }

                Constants.ListenerEmitType.Removed -> {
                    it.singleResponse?.postId?.let { postId ->
                        val removePost = savedPost.find { it.postId == postId }

                        if (removePost != null) {
                            savedPost.remove(removePost)
                        }
                    }
                }

                Constants.ListenerEmitType.Modify -> {
                    // Don't do anything
                }
            }
        }


        // Now whoever listen this flow , get updated List
        AppBroadcastHelper.setSavedPostList(savedPost)
    }


    fun listenMyLikedPost() {
        val job = viewModelScope.myLaunch {
            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.listenMyLikedPost().onEach {
                    handleLikedPostResponse(it)
                }.launchIn(this)
            }
        }
        jobList.add(job)
    }

    fun listenAuthOfUser() = viewModelScope.myLaunch {
        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.listenAuthOfUser()
        }
    }

    private fun handleLikedPostResponse(response: List<ListenerEmissionType<LikedPostModel, LikedPostModel>>) {

        response.forEach {
            when (it.emitChangeType) {
                Constants.ListenerEmitType.Starting -> {
                    // Don't do any thing here
                }

                Constants.ListenerEmitType.Added -> {
                    it.singleResponse?.let {
                        likedPost.add(it)
                    }
                }

                Constants.ListenerEmitType.Removed -> {
                    it.singleResponse?.postId?.let { postId ->
                        val removePost = likedPost.find { it.postId == postId }

                        if (removePost != null) {
                            likedPost.remove(removePost)
                        }
                    }
                }

                Constants.ListenerEmitType.Modify -> {
                    // Don't do anything
                }
            }
        }


        // Now whoever listen this flow , get updated List
        AppBroadcastHelper.setLikedPostList(likedPost)
    }

    fun setFcmToken(token: String) = viewModelScope.myLaunch {
        _fcmToken.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.setFcmToken(token).onEach {
                if (it.isSuccess) {
                    _fcmToken.tryEmit(Resource.Success(it))
                } else {
                    _fcmToken.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _fcmToken.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    fun updateUserAvailability(isUserAvailable: Boolean) = viewModelScope.myLaunch {
        repository.updateUserAvailability(isUserAvailable)
    }

    fun getLatestUpdate(packageName: String) = viewModelScope.myLaunch {
        _appUpdate.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)) {
            _appUpdate.tryEmit(handleAppUpdate(repository.getLatestAppUpdate(packageName)))
        } else {
            _appUpdate.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleAppUpdate(latestAppUpdate: Response<GitHubRelease>): Resource<GitHubRelease> {
        if (latestAppUpdate.isSuccessful) {
            latestAppUpdate.body()?.let {
                return Resource.Success(it)
            }
        }
        return Resource.Error(latestAppUpdate.message())
    }

    /*     fun updateApp(release: GitHubRelease) =  viewModelScope.myLaunch {
            _appUpdating.tryEmit(Resource.Loading())
            if(SoftwareManager.isNetworkAvailable(app)){
                val downloadRequest = release.getDownloadRequest(app)
                if (downloadRequest != null) {
                    val downloadManager = app.getSystemService<DownloadManager>()
                    if (downloadManager != null) {
                        val pref = SharePref(app)
                        val lastUpdateId = pref.getPrefLong(SharePref.PreferencesKeys.PREFERENCE_LAST_UPDATE_ID).first()
                        if (lastUpdateId != 0L) {
                            downloadManager.remove(lastUpdateId)
                        }
                        pref.setPrefLong(SharePref.PreferencesKeys.PREFERENCE_LAST_UPDATE_ID, downloadManager.enqueue(downloadRequest))
                    }
                }
            }else{
                _appUpdating.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
            }
        }*/
    fun updateApp(release: GitHubRelease, onUpdate: (progress: Int?) -> Unit) =
        viewModelScope.myLaunch {
            _appUpdating.tryEmit(Resource.Loading())
            if (SoftwareManager.isNetworkAvailable(app)) {
                val apkFiles = release.getDownloadRequest(app, onUpdate)
                _appUpdating.tryEmit(Resource.Success(apkFiles))
            } else {
                _appUpdating.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
            }
        }

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

    fun setListenerSetStatus(status: Boolean) {
        _isListenerSet = status
    }


    fun removeAllListener() {
        _isListenerSet = false
        jobList.forEach {
            it.cancel()
        }
    }
}