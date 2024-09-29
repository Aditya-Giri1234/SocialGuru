package com.aditya.socialguru.ui_layer.fragment.story_helper

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.databinding.FragmentStoryShowBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.manager.MediaManager
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.OnVideoStateChange
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class StoryShowFragment : Fragment() {

    private var _binding: FragmentStoryShowBinding? = null
    private val binding: FragmentStoryShowBinding get() = _binding!!

    private val args: StoryShowFragmentArgs by navArgs()
    private lateinit var userStories: UserStories
    private var _user: User? = null
    private val user get() = _user!!

    private var _stories: List<Stories>? = null
    private val stories get() = _stories!!

    private var currentStatusIndex = 0
    private val tagStory = Constants.LogTag.Story

    private val navController get() = (requireActivity() as MainActivity).navController
    private val mediaController: MediaController by lazy {
        MediaController(requireContext())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStoryShowBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        userStories = args.userStory
        _user = userStories.user
        _stories = userStories.stories

        initUi()
    }

    private fun initUi() {
        binding.apply {
            videoStatus.useController = false
            linearHeader.setProgressBarCount(stories.size){
                MyLogger.w(tagStory, msg = "Next story  is going to show  event come !")
                handleNextStoryChange(false)
            }
            setListener()
        }
        handleStoryChange(stories[currentStatusIndex])
    }



    private fun FragmentStoryShowBinding.setListener() {

        previous.setOnClickListener {
          handlePreviousStoryChange()
        }

        previous.setOnTouchListener(object : View.OnTouchListener {
            private var isLongPressed = false
            private val longPressRunnable = Runnable {
                isLongPressed = true
                linearHeader.pause()
                videoStatus.player?.pause()
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isLongPressed = false
                        v?.postDelayed(longPressRunnable, 500)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v?.removeCallbacks(longPressRunnable)
                        if (isLongPressed) {
                            videoStatus.player?.play()
                            linearHeader.resume()
                        } else {
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        next.setOnTouchListener(object : View.OnTouchListener {
            private var isLongPressed = false
            private val longPressRunnable = Runnable {
                isLongPressed = true
                linearHeader.pause()
                videoStatus.player?.pause()
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isLongPressed = false
                        v?.postDelayed(longPressRunnable, 500)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v?.removeCallbacks(longPressRunnable)
                        if (isLongPressed) {
                            videoStatus.player?.play()
                            linearHeader.resume()
                        } else {
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })



        next.setOnClickListener {
          handleNextStoryChange()
        }
        videoStatus.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            MyLogger.w(tagStory, msg = "Video view layout change !")
            if (_binding != null) {
                closeVideoIfPlay()
            }
        }

    }



    private fun handleStoryChange(story: Stories) {
        when (story.storyType) {
            Constants.StoryType.Text.name -> {
                binding.linearHeader.setProgressTime(currentStatusIndex,5000)
                handleTextStory(story)
                binding.linearHeader.startProgress()
            }

            Constants.StoryType.Image.name -> {
                binding.linearHeader.setProgressTime(currentStatusIndex,5000)
                handleImageStory(story)
            }

            Constants.StoryType.Video.name -> {
                handleVideoStory(story)
            }

            else -> {

            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun handleVideoStory(story: Stories) {
        binding.apply {
            ivStatus.gone()
            tvStatus.gone()
            videoStatus.myShow()
            videoStatus.apply {
                visibility = View.VISIBLE
                story.storyUri?.let {uri->
                    MediaManager.stopAllPlayerExceptThis(uri.convertParseUri())
                   val exoPlayer= if (MediaManager.isPlayerExistsForUri(uri.convertParseUri())){
                       MediaManager.getPlayer(uri.convertParseUri())
                    }else{
                       val temp= MediaManager.addVideo(uri.convertParseUri(),requireContext())
                       temp.addMediaItem(uri.convertParseUri())
                       temp.subscribeToOnPlaybackStateChange(object : OnVideoStateChange {
                           override fun onLoad() {
                               progressBar.myShow()
                           }

                           override fun onReady() {
                               progressBar.gone()
                               MyLogger.i(tagStory , msg = "Current story is $currentStatusIndex  which is video and  now to play  and duration is ${temp.getPlayer().duration.toInt() } !")
                               linearHeader.setProgressTime(currentStatusIndex , temp.getPlayer().duration.toInt() )
                               linearHeader.startProgress()
                           }

                           override fun onError() {
                               progressBar.gone()
                               Helper.customToast(requireContext(),"Some error occurred during video loading !" ,
                                   Toast.LENGTH_SHORT,true)

                               handleNextStoryChange()
                           }

                           override fun onComplete() {
                               handleNextStoryChange()
                           }
                       })
                       temp
                   }
                    player=exoPlayer?.getPlayer()
                    exoPlayer?.prepare()
                }

            }
        }

    }


    private fun handleImageStory(story: Stories) {
        binding.apply {
            progressBar.myShow()
            ivStatus.myShow()
            tvStatus.gone()
            videoStatus.gone()
            Glide.with(this@StoryShowFragment)
                .load(story.storyUri)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.gone()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        MyLogger.i(tagStory , msg = "Current story is $currentStatusIndex  which is image and  now to play !")
                        progressBar.gone()
                        linearHeader.startProgress()
                        return false
                    }
                })
                .into(ivStatus)
        }

    }

    private fun handleTextStory(story: Stories) {
        binding.apply {
            progressBar.gone()
            ivStatus.gone()
            tvStatus.myShow()
            videoStatus.gone()
            story.apply {
                tvStatus.setBackgroundColor(textBackGroundColor?.toInt() ?: Color.BLACK)
                tvStatus.typeface = ResourcesCompat.getFont(
                    requireContext(),
                    textFontFamily ?: R.font.roboto_medium
                )
                tvStatus.text = text
            }
        }

    }


    private fun closeVideoIfPlay() {
        binding.apply {
            if (videoStatus.isGone) {
                MediaManager.stopAllPlayer()
            }
        }
    }

    private fun handlePreviousStoryChange(){
        MyLogger.d(tagStory, msg = "Previous is click and currentStatusIndex :- $currentStatusIndex")

        if (currentStatusIndex==0) return
        currentStatusIndex -= 1
        binding.linearHeader.previous()
        handleStoryChange(stories[currentStatusIndex])

    }

    private fun handleNextStoryChange(isUserClick:Boolean=true){
        MyLogger.d(tagStory, msg = "Next is click and currentStatusIndex :- $currentStatusIndex")

        if (currentStatusIndex == stories.size - 1){
            navController?.navigateUp()
            return
        }

        currentStatusIndex++
        if (isUserClick){
            MyLogger.d(tagStory, msg = "This is user next click !")
            binding.linearHeader.next()
        }

        handleStoryChange(stories[currentStatusIndex])

    }

    override fun onDestroyView() {
        MediaManager.clearVideos()
        binding.linearHeader.removeCallback()
        _binding = null
        super.onDestroyView()
    }
}


