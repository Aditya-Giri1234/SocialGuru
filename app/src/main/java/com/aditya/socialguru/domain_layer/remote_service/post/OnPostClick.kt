package com.aditya.socialguru.domain_layer.remote_service.post

interface OnPostClick {
    fun onImageClick() : ()->Unit
    fun onVideoClick():()->Unit
    fun onLikeClick()
    fun onCommentClick()
    fun onSettingClick()
    fun onSendClick()
    fun onPostClick()
}