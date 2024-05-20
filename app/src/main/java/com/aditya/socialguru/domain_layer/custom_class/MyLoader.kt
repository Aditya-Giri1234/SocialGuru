package com.aditya.socialguru.domain_layer.custom_class

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.CustomLoaderLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.IntentTable
import com.aditya.socialguru.domain_layer.helper.Constants.StoryUploadState
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.manager.MyLogger

class MyLoader(val message: String? = null) : DialogFragment() {
    private var _binding: CustomLoaderLayoutBinding? = null
    private val binding get() = _binding!!

    private val tagStory = Constants.LogTag.Story
    private val tagPost=Constants.LogTag.Post

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.getStringExtra(Constants.TYPE)) {
                Constants.BroadcastType.StoryUploading.name -> {
                    p1.let {
                        val progress = it.getIntExtra(IntentTable.UploadProgress.name, 0)
                        val message = it.getStringExtra(IntentTable.UploadMessage.name)
                        val state = it.getStringExtra(IntentTable.UploadState.name)
                        MyLogger.d(
                            tagStory,
                            msg = "state:=$state , message:- $message , progress:- $progress"
                        )
                        updateContent(progress, message, state)
                    }

                }

            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeToBroadcast()
        MyLogger.v(tagPost , isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = CustomLoaderLayoutBinding.inflate(layoutInflater)
        MyLogger.v(tagPost , isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(tagPost , isFunctionCall = true)
        handleInitialization()
    }


    private fun handleInitialization() {
        MyLogger.v(tagPost , isFunctionCall = true)
        initUi()
    }

    private fun initUi() {
        MyLogger.v(tagPost , isFunctionCall = true)
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

    private fun subscribeToBroadcast() {
        MyLogger.v(tagPost , isFunctionCall = true)
        val filter = IntentFilter()
        filter.addAction(Constants.BroadcastType.StoryUploading.name)
        ContextCompat.registerReceiver(
            requireContext(),
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun updateContent(progress: Int, message: String?, state: String?) {
        MyLogger.v(tagPost , isFunctionCall = true)
        binding.apply {
            when (state) {
                StoryUploadState.StartUploading.name -> {
                    tvMessage.text = "Starting uploading..."
                }

                StoryUploadState.Uploading.name -> {
                    tvPercentage.myShow()
                    tvMessage.text = "Uploading"
                    tvPercentage.text = "$progress%"

                }

                StoryUploadState.UploadingFail.name -> {
                    tvMessage.text = "Uploading failed"
                }

                StoryUploadState.UrlNotGet.name -> {
                    tvMessage.text = "Some Error occurred during Saving File."
                }

                StoryUploadState.SavingStory.name -> {
                    tvMessage.text = "Saving story..."
                }

                StoryUploadState.StoryUploadedSuccessfully.name -> {
                    tvMessage.text = ""
                }

                else -> {

                }
            }
        }
    }

    fun updateUiState(
        message: String? = null,
        progress: Int = 0,
        isDismiss: Boolean = false
    ) {
        MyLogger.v(tagPost , msg = "Now loader is dismiss := $isDismiss , message is $message , progress := $progress")
        if (isDismiss) {
            dismiss()
            return
        }
        if (message==null) {
            showCircularLoader()
        } else {
            hideCircularLoader()
        }



        binding.apply {
            if (progress == 0) {
                tvPercentage.gone()
            } else {
                tvPercentage.myShow()
            }
            tvMessage.text = message
            tvPercentage.text = "$progress%"
        }


    }

    private fun showCircularLoader() {
        MyLogger.v(tagPost , isFunctionCall = true)
        binding.apply {
            circularProgress.show()
            linearMessageLoader.gone()
        }
    }

    private fun hideCircularLoader() {
        MyLogger.v(tagPost , isFunctionCall = true)
        binding.apply {
            linearMessageLoader.myShow()
            circularProgress.gone()
            tvMessage.text = message
        }


    }

    override fun onDestroyView() {
        MyLogger.v(tagPost , isFunctionCall = true)
        _binding=null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MyLogger.v(tagPost , isFunctionCall = true)
        requireContext().unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}