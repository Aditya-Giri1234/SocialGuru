package com.aditya.socialguru.domain_layer.custom_class.dialog

import android.app.Dialog
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.SampleStartChatDialogBinding
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.helper.setWidthPercent
import com.aditya.socialguru.domain_layer.remote_service.chat.StartChatDialogOption


@Suppress("DEPRECATION")
class SelectStartChatDialog(val option:StartChatDialogOption) : DialogFragment() {

    private var _binding:SampleStartChatDialogBinding?=null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=SampleStartChatDialogBinding.inflate(layoutInflater)
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
            dialog?.let {
                it.setCancelable(true)
                it.setCanceledOnTouchOutside(true)
                it.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            setListener()
        }
    }

    private fun SampleStartChatDialogBinding.setListener() {
        tvSingleChat.setSafeOnClickListener {
            option.onSingleChatClick()
            dismiss()
        }
        tvGroupChat.setSafeOnClickListener {
            option.onGroupChatClick()
            dismiss()
        }
    }


    override fun onResume() {
        super.onResume()
        this.setWidthPercent(75)
    }


    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }
}


