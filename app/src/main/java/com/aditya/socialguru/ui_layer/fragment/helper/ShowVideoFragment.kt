package com.aditya.socialguru.ui_layer.fragment.helper

import android.animation.Animator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentShowVideoBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hide
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.aditya.socialguru.ui_layer.viewmodel.VideoViewModel
import com.google.android.material.animation.AnimationUtils


@UnstableApi
class ShowVideoFragment : Fragment() {

    private var _binding: FragmentShowVideoBinding? = null
    private val binding get() = _binding!!

    private val args: ShowVideoFragmentArgs by navArgs()
    private lateinit var videoUri: Uri
    private var isShareAllow: Boolean =false

    private val myPlayer by lazy {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000,    // Minimum buffer before playback starts (in milliseconds)
                50000,    // Maximum buffer before stopping fetching more data
                2500,  // Buffer required for playback to resume
                5000 // Buffer required after rebuffering
            )
            .build()
        val trackSelector = DefaultTrackSelector(requireContext())
        val eventLogger = EventLogger(trackSelector)
        val player =ExoPlayer.Builder(requireContext())
            .setLoadControl(loadControl)
            .build()
        player.addAnalyticsListener(eventLogger)
        player
    }
    private val videoViewModel: VideoViewModel by viewModels()

    private val window by lazy {
        requireActivity().window
    }

    private val windowInsetsController by lazy {
        WindowCompat.getInsetsController(window,window.decorView)
    }


    private val navController get() = (requireActivity() as MainActivity).navController



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentShowVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        videoUri = args.videoUri
        isShareAllow = args.isShareAllow
        initUi()
    }


    private fun initUi() {
        binding.apply {
            icShare?.isGone = !isShareAllow
            playerView.apply {
                player = myPlayer
                val mediaItem = MediaItem.fromUri(videoUri)
                myPlayer.setMediaItem(mediaItem)
                myPlayer.seekTo(videoViewModel.playerPosition)
                myPlayer.prepare()
                myPlayer.playWhenReady=true
            }

            if (videoViewModel.isFirstTimeFragmentCreated){
                videoViewModel.setFalseToFragmentCreateFirstTime()
            }else{
                if (isFullScreen()){
                    hideSystemNavBar()
                }else{
                    showSystemNavBar()
                }
            }

            setListener()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun FragmentShowVideoBinding.setListener() {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            MyLogger.w(msg = "Full screen mode and window inset call !")

            if(isFullScreen()){
                MyLogger.w(msg = "Full screen mode and window inset call !")
                v.setPadding(
                    0,
                    -systemBars.top,
                    0,
                    -systemBars.bottom
                )
            }else{
                v.setPadding(
                    0,
                    0,
                    0,
                    0
                )
            }

            insets
        }

        icBack.setSafeOnClickListener {
            exitFromFullScreen()
            requireActivity().onBackPressed()
        }

        //This null check come because i remove this view from landscape mode
        icShare?.setSafeOnClickListener {
            ShareManager.shareVideo(requireContext(),videoUri)
        }

        icFullscreen.setSafeOnClickListener {
            if (isFullScreen()) {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            } else {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        playerView.setOnTouchListener { v, event ->
            if (isFullScreen()) {
                hideSystemNavBar()
                handleFullScreenClick()
            }
            return@setOnTouchListener false
        }


        root.setOnApplyWindowInsetsListener { v, insets ->
            // Apply the insets as padding to the view
            v.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
            insets
        }
        myPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    ExoPlayer.STATE_IDLE -> {
                        MyLogger.d(msg = "STATE_IDLE")
                    }

                    ExoPlayer.STATE_BUFFERING -> {
                        MyLogger.d(msg = "STATE_BUFFERING")
                        progressBar?.myShow()
                        playerView.hide()
                    }

                    ExoPlayer.STATE_READY -> {
                        MyLogger.d(msg = "STATE_READY")
                        progressBar?.gone()
                        playerView.myShow()
                    }
                    ExoPlayer.STATE_ENDED -> {
                        MyLogger.d(msg = "STATE_ENDED")
                    }
                }
            }
        })
    }

    private fun handleFullScreenClick() {
        binding.apply {
            if (linearHeader?.isGone == true) {
                showLinearHeader()
            } else {
                hideLinearHeader()
            }
        }

    }

    private fun hideLinearHeader() {
        binding.linearHeader?.let { header ->
            header.animate().apply {
                duration = 300
                translationY(-header.height.toFloat()) // Move the bottom navigation bar above the screen
                setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        header.gone()
                    }

                    override fun onAnimationCancel(p0: Animator) {

                    }

                    override fun onAnimationRepeat(p0: Animator) {

                    }

                })
            }

        }
    }

    private fun showLinearHeader() {
        binding.linearHeader?.let { header ->
            header.animate().apply {
                duration = 300
                translationY(-header.height.toFloat()) // Move the bottom navigation bar above the screen
                translationY(0f)
                setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        header.myShow()
                    }

                    override fun onAnimationCancel(p0: Animator) {

                    }

                    override fun onAnimationRepeat(p0: Animator) {

                    }

                })
            }

        }
    }

    private fun isFullScreen() =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun exitFromFullScreen(){
        if (isFullScreen()){
            showSystemNavBar()
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        }
    }


    private fun showSystemNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
    private fun hideSystemNavBar(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            requireActivity().window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

    }

    override fun onResume() {
        myPlayer.play()
        super.onResume()
    }

    override fun onStop() {
        myPlayer.pause()
        super.onStop()
    }

    override fun onDestroyView() {
        videoViewModel.setPlayerPosition(myPlayer.currentPosition)
        myPlayer.release()
        _binding = null
        super.onDestroyView()
    }



}


