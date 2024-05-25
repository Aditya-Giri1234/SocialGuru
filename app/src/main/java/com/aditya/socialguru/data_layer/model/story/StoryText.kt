package com.aditya.socialguru.data_layer.model.story

import java.io.Serializable


data class StoryText(
    val text:String,
    val textFontFamily:Int,
    val textBackGroundColor:Int
) : Serializable
