package com.aditya.socialguru.domain_layer.remote_service.post

import android.net.Uri
import com.aditya.socialguru.data_layer.model.post.Post

interface OnPostClick {
    fun onImageClick() : (Uri)->Unit
    fun onVideoClick():(Uri)->Unit
    fun onLikeClick(post: Post)
    fun onCommentClick(postId: String)
    fun onSettingClick(post:String)
    fun onSendClick(post: Post)
    fun onPostClick(postId:String)

}