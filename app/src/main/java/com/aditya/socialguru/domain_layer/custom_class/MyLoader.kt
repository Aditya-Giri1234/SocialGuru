package com.aditya.socialguru.domain_layer.custom_class

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.CustomLoaderLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.StoryUploadState
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.manager.MyLogger

class MyLoader(val message: String? = null) : DialogFragment() {
    private var _binding: CustomLoaderLayoutBinding? = null
    private val binding get() = _binding!!

    private val tagStory = Constants.LogTag.Story
    private val tagPost = Constants.LogTag.Post


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(tagPost, isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = CustomLoaderLayoutBinding.inflate(layoutInflater)
        MyLogger.v(tagPost, isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(tagPost, isFunctionCall = true)
        handleInitialization()
    }


    private fun handleInitialization() {
        MyLogger.v(tagPost, isFunctionCall = true)
        initUi()
    }

    private fun initUi() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {

            dialog?.apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            if (message != null) {
                linearMessageLoader.myShow()
                circularProgress.gone()
                tvMessage.text = message

            } else {
                circularProgress.show()
                linearMessageLoader.gone()
            }

            setListener()
        }
    }

    private fun CustomLoaderLayoutBinding.setListener() {

    }


    fun updateUiByStory(state: StoryUploadState=StoryUploadState.StartUploading, progress: Int=0) {
        MyLogger.v(tagStory, isFunctionCall = true)
        binding.apply {
            if (circularProgress.isVisible) hideCircularLoader()

            tvPercentage.isGone = progress == 0

            val text = when (state) {
                StoryUploadState.StartUploading -> {
                    MyLogger.d(tagStory, msg = "Story uploading starting ....")
                    "Starting uploading..."
                }

                StoryUploadState.Uploading -> {
                    MyLogger.d(tagStory, msg = "Story uploading  ....")
                    "Uploading"
                }


                StoryUploadState.SavingStory -> {
                    MyLogger.d(tagStory, msg = "Story saving   ....")
                    "Saving story..."
                }

                else-> {
                    ""
                }

            }

            tvMessage.text = text
            tvPercentage.text = progress.toString()

        }
    }


    fun updateUiByPostUpload(
        message: String? = null,
        progress: Int = 0,
        isDismiss: Boolean = false
    ) {
        MyLogger.v(
            tagPost,
            msg = "Now loader is dismiss := $isDismiss , message is $message , progress := $progress"
        )
        if (isDismiss) {
            dismiss()
            return
        }
        if (message == null) {
            showCircularLoader()
        } else {
            hideCircularLoader()
        }



        binding.apply {
            tvPercentage.isGone = progress == 0
            tvMessage.text = message
            tvPercentage.text = "$progress%"
        }


    }

    private fun showCircularLoader() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            circularProgress.show()
            linearMessageLoader.gone()
        }
    }

    private fun hideCircularLoader() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            linearMessageLoader.myShow()
            circularProgress.gone()
            tvMessage.text = message
        }


    }

    override fun onDestroyView() {
        MyLogger.v(tagPost, isFunctionCall = true)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MyLogger.v(tagPost, isFunctionCall = true)
        super.onDestroy()
    }
}