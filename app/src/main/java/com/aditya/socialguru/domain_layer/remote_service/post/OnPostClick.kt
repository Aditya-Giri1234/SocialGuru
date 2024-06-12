package com.aditya.socialguru.domain_layer.remote_service.post

import android.net.Uri

interface OnPostClick {
    fun onImageClick() : (Uri)->Unit
    fun onVideoClick():(Uri)->Unit
    fun onLikeClick()
    fun onCommentClick(postId: String)
    fun onSettingClick()
    fun onSendClick()
    fun onPostClick(postId:String)
}