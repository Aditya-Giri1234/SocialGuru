package com.aditya.socialguru.domain_layer.custom_class.player

import android.content.Context
import android.net.Uri
import android.widget.ProgressBar
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.OnVideoStateChange
import java.io.File

@UnstableApi
class MyExoplayer(val context: Context) {

    private val tagStory = Constants.LogTag.Story
    private val exoplayer by lazy {
        // Now add to media source to our player
        initializeCacheDataSource()
        val mediaSourceFactory: MediaSource.Factory =
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(cacheDataSourceFactory!!)
        val player = ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
        player.playWhenReady=true
        player
    }

    //This is for cache video data
    private fun initializeCacheDataSource() {
        if (cacheDataSourceFactory==null){
            val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
            val downloadContentDirectory =
                File(context.cacheDir,DOWNLOAD_CONTENT_DIRECTORY)
            val downloadCache =
                SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
            val cacheSink = CacheDataSink.Factory()
                .setCache(downloadCache)
            val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())
            val downStreamFactory = FileDataSource.Factory()
            cacheDataSourceFactory=CacheDataSource.Factory()
                .setCache(downloadCache)
                .setCacheWriteDataSinkFactory(cacheSink)
                .setCacheReadDataSourceFactory(downStreamFactory)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }

    }

    companion object{
        private var cacheDataSourceFactory:CacheDataSource.Factory?=null
    }




//    private var playWhenReady : Boolean =false
//        set(value) {
//            exoplayer.playWhenReady = field
//            field = value
//        }

    fun addMediaItem(uri: Uri) {
        exoplayer.addMediaItem(MediaItem.fromUri(uri))
    }

    fun subscribeToOnPlaybackStateChange(videoState:OnVideoStateChange) {
        exoplayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    ExoPlayer.STATE_IDLE -> {
                        MyLogger.d(tagStory, msg = "STATE_IDLE")
                    }

                    ExoPlayer.STATE_BUFFERING -> {
                        MyLogger.d(tagStory, msg = "STATE_BUFFERING")
                        videoState.onLoad()
                    }

                    ExoPlayer.STATE_READY -> {
                        MyLogger.d(tagStory, msg = "STATE_READY")
                          videoState.onReady()
                    }

                    ExoPlayer.STATE_ENDED -> {
                        MyLogger.d(tagStory, msg = "STATE_ENDED")
                        videoState.onComplete()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                videoState.onError()
            }
        })
    }

    fun prepare(){
        exoplayer.prepare()
    }
    fun stop(){
        exoplayer.stop()
    }

    fun release(){
        exoplayer.release()
    }

    fun clearMediaItem(){
        exoplayer.clearMediaItems()
    }
    fun setMediaItem(uri: Uri){
        exoplayer.setMediaItem(MediaItem.fromUri(uri))
    }


    fun resetPosition(){
        exoplayer.seekTo(0)
    }

    fun getPlayer()=exoplayer



}