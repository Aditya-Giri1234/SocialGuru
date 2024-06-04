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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = CustomLoaderLayoutBinding.inflate(layoutInflater)
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(isFunctionCall = true)
        handleInitialization()
    }


    private fun handleInitialization() {
        MyLogger.v(isFunctionCall = true)
        initUi()
    }

    private fun initUi() {
        MyLogger.v(isFunctionCall = true)
        binding.apply {

            dialog?.apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            tvMessage.text = message

            if (message != null) {
               hideCircularLoader()
            } else {
               showCircularLoader()
            }

            setListener()
        }
    }

    private fun CustomLoaderLayoutBinding.setListener() {

    }

    fun setLoadingStatus(message: String?=null,progress: Int=0,isDismiss: Boolean=false){
        MyLogger.v(
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
        MyLogger.v(isFunctionCall = true)
        binding.apply {
            circularProgress.show()
            linearMessageLoader.gone()
        }
    }

    private fun hideCircularLoader() {
        MyLogger.v(isFunctionCall = true)
        binding.apply {
            linearMessageLoader.myShow()
            circularProgress.gone()
            tvMessage.text = message
        }


    }

    override fun onDestroyView() {
        MyLogger.v(isFunctionCall = true)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MyLogger.v(isFunctionCall = true)
        super.onDestroy()
    }
}