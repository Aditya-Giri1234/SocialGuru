package com.aditya.socialguru.domain_layer.remote_service

import com.aditya.socialguru.domain_layer.helper.Constants

interface StoryTypeOptions {
    fun onStoryTypeSelect(selectedStoryType:Constants.StoryTpye)
}