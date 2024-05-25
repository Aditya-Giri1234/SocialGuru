package com.aditya.socialguru.data_layer.model.story

import com.aditya.socialguru.domain_layer.helper.Constants

data class StoryListenerEmissionType(
    val emitChangeType:Constants.StoryEmitType,
    val userStoryList: List<UserStories>? = null,
    val story: Stories? = null,
)
