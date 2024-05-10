package com.aditya.socialguru.domain_layer.helper

import android.health.connect.datatypes.units.Percentage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppBroadcastHelper {

    private val _uploadStories= MutableSharedFlow<Pair<Constants.StoryUploadState,Int?>>(0)

    val uploadStories get() = _uploadStories.asSharedFlow()

    fun setUploadState(state:Constants.StoryUploadState,percentage: Int?=null){
        _uploadStories.tryEmit(Pair(state,percentage))
    }
}