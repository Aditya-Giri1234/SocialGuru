package com.aditya.socialguru.domain_layer.helper

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

    private val _uploadPost = MutableSharedFlow<Pair<Constants.PostUploadState, Int?>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )

    val uploadPost get() = _uploadPost.asSharedFlow()

    fun setStoryUploadState(state: Constants.StoryUploadState, percentage: Int? = null) {
        _uploadStories.tryEmit(Pair(state, percentage))
    }

    fun setPostUploadState(state: Constants.PostUploadState, percentage: Int? = null) {
        _uploadPost.tryEmit(Pair(state, percentage))
    }
}