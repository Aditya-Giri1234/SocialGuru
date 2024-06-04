package com.aditya.socialguru.domain_layer.custom_class.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.DialogEditProfilePicBinding
import com.aditya.socialguru.databinding.DialogStoryTypeViewBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.remote_service.StoryTypeOptions
import com.aditya.socialguru.domain_layer.remote_service.profile.ProfilePicEditOption

class ProfilePicEditDialog(private val profilePicEditOption: ProfilePicEditOption) : DialogFragment() {

    private var _binding: DialogEditProfilePicBinding?=null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= DialogEditProfilePicBinding.inflate(layoutInflater)
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

    private fun DialogEditProfilePicBinding.setListener() {
        linearText.setOnClickListener {
            profilePicEditOption.onDelete()
            dismiss()
        }
        linearImage.setOnClickListener {
            profilePicEditOption.onUpload()
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}