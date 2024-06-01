package com.aditya.socialguru.domain_layer.helper

import com.aditya.socialguru.data_layer.model.post.PostUploadingResponse
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


object AppBroadcastHelper {

    private val _uploadStories = MutableSharedFlow<Pair<Constants.StoryUploadState, Int?>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )

    val uploadStories get() = _uploadStories.asSharedFlow()

    private val _uploadPost = MutableSharedFlow<PostUploadingResponse>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )

    val uploadPost get() = _uploadPost.asSharedFlow()

    private val _mainActivityBottomNavHideByScroll = MutableSharedFlow<Boolean>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )

    val mainActivityBottomNavHideByScroll get() = _mainActivityBottomNavHideByScroll.asSharedFlow()

    private val _homeFragmentBackToTopShow = MutableSharedFlow<Boolean>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )

    val homeFragmentBackToTopShow get() = _homeFragmentBackToTopShow.asSharedFlow()

    private val _homeScrollBackToTopClick = MutableSharedFlow<Boolean>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )

    val homeScrollBackToTopClick get() = _homeScrollBackToTopClick.asSharedFlow()

    fun setStoryUploadState(state: Constants.StoryUploadState, percentage: Int? = null) {
        _uploadStories.tryEmit(Pair(state, percentage))
    }

    fun setPostUploadState(response:PostUploadingResponse) {
        _uploadPost.tryEmit(response)
    }

    fun setMainActivityBottomNavHideByScroll(isHide:Boolean){
        _mainActivityBottomNavHideByScroll.tryEmit(isHide)
    }

    fun setHomeFragmentBackToTopShow(isShow:Boolean){
        _homeFragmentBackToTopShow.tryEmit(isShow)
    }

    fun setHomeScrollBackToTopClick(isClick:Boolean){
        _homeScrollBackToTopClick.tryEmit(isClick)
    }
}