package com.aditya.socialguru.domain_layer.custom_class.dialog.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.SampleSendAttachmentLayoutBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.remote_service.chat.OnAttachmentItemListener

class AttachmentDialog(val attachmentItemListener: OnAttachmentItemListener) : DialogFragment() {

    private var _binding: SampleSendAttachmentLayoutBinding?=null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=SampleSendAttachmentLayoutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
    }

    private fun initUi() {
        binding.apply {
            dialog?.apply {
                setCancelable(true)
                setCanceledOnTouchOutside(true)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            setListener()
        }
    }

    private fun SampleSendAttachmentLayoutBinding.setListener() {
        ivCamera.setSafeOnClickListener {
            attachmentItemListener.onAttachmentImageClick()
            dismiss()
        }
        ivGallery.setSafeOnClickListener {
            attachmentItemListener.onAttachmentGalleryClick()
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}