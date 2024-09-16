package com.aditya.socialguru.ui_layer.fragment.helper

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.adapters.ViewGroupBindingAdapter.setListener
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentVideoTrimmingBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment.HomeFragment
import com.redevrx.video_trimmer.event.OnVideoEditedEvent
import kotlinx.coroutines.delay
import java.io.File


class VideoTrimmingFragment : Fragment(), OnVideoEditedEvent {
    private var _binding: FragmentVideoTrimmingBinding? = null
    private val binding get() = _binding!!

    private val tagStory = Constants.LogTag.Story
    private lateinit var videoUri: String
    private var myLoader: MyLoader? = null
    private val args by navArgs<VideoTrimmingFragmentArgs>()
    private val navController get() = (requireActivity() as MainActivity).navController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVideoTrimmingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        videoUri = args.videoUri
        initUi()
        subscribeToObserver()
    }

    private fun subscribeToObserver() {
        observeFlow {

        }
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                this.profileImage.gone()
                this.tvHeaderUserName.text = "Trim Video"
                this.icBack.myShow()
            }
            btnSaveVideo.disabled()
            videoTrimmer.apply {
                setVideoBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))
                setOnTrimVideoListener(this@VideoTrimmingFragment)
                setVideoURI(Uri.parse(videoUri))
                setDestinationPath(requireContext().cacheDir.absolutePath)
                setVideoInformationVisibility(true)
                setMaxDuration(30)
                setMinDuration(0)
            }
            setListener()
        }
    }

    private fun FragmentVideoTrimmingBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        btnSaveVideo.setOnClickListener {
            binding.videoTrimmer.saveVideo()
            showLoader()
        }
    }

    private fun showLoader() {
        MyLogger.v(tagStory, isFunctionCall = true)
        if (myLoader == null) {
            myLoader = MyLoader()
        }
        myLoader?.show(childFragmentManager, "My Loader")
    }


    private fun hideLoader() {
        MyLogger.w(tagStory, isFunctionCall = true)
            myLoader?.dismissNow()
            myLoader = null
    }

    private fun showSnackBar(message: String, isSuccess: Boolean) {
        if (isSuccess) {
            Helper.showSuccessSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(R.id.coordLayout),
                message
            )
        } else {
            Helper.showSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(R.id.coordLayout),
                message
            )
        }
    }


    override fun getResult(uri: Uri , fileName:String) {
        hideLoader()
        setFragmentResult(HomeFragment.VideoTrimRequest, bundleOf(HomeFragment.VideoTrimResult to uri.toString() , HomeFragment.VideoTrimRequest to fileName))
        navController.popBackStack()
    }

    override fun onError(message: String) {
        hideLoader()
        showSnackBar(message , false)
        navController.popBackStack()
    }

    override fun onVideoPrepared() {
        binding.btnSaveVideo.enabled()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}