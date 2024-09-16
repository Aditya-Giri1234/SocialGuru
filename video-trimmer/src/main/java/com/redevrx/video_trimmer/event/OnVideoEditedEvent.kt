package com.redevrx.video_trimmer.event

import android.net.Uri

interface OnVideoEditedEvent {
    fun getResult(uri: Uri , fileName:String)
    fun onError(message: String)
    fun onVideoPrepared()
}