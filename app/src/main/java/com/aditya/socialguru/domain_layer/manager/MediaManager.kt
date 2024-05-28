package com.aditya.socialguru.domain_layer.manager

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.aditya.socialguru.domain_layer.custom_class.player.MyExoplayer

@OptIn(UnstableApi::class)
object MediaManager {

    private val videoMap= mutableMapOf<String,MyExoplayer>()


    fun addVideo(uri: Uri ,context:Context):MyExoplayer{
        videoMap[uri.toString()] = MyExoplayer(context)
        return videoMap[uri.toString()]!!
    }

    fun clearVideos(){
        videoMap.forEach { (uri, player) ->
            player.release()
        }
        videoMap.clear()
    }

    fun getPlayer(uri: Uri): MyExoplayer? {
       return videoMap[uri.toString()]
    }

    fun isPlayerExistsForUri(uri: Uri):Boolean{
        return  videoMap[uri.toString()]!=null
    }

    fun stopAllPlayerExceptThis(uri:Uri){
        videoMap.filter {
            it.key!=uri.toString()
        }.forEach { (key, player) ->
            player.stop()
            player.resetPosition()
        }
    }

    fun stopAllPlayer(){
        videoMap.forEach { (key, player) ->
            player.stop()
            player.resetPosition()
        }
    }
}