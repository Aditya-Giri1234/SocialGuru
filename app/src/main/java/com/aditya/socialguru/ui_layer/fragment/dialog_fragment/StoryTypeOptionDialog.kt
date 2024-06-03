package com.aditya.socialguru.ui_layer.fragment.dialog_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.DialogStoryTypeViewBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.remote_service.StoryTypeOptions

class StoryTypeOptionDialog(private val storySelect:StoryTypeOptions) : DialogFragment() {

    private var _binding:DialogStoryTypeViewBinding?=null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=DialogStoryTypeViewBinding.inflate(layoutInflater)
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

    private fun DialogStoryTypeViewBinding.setListener() {
        linearText.setOnClickListener {
            storySelect.onStoryTypeSelect(Constants.StoryType.Text)
            dismiss()
        }
        linearImage.setOnClickListener {
            storySelect.onStoryTypeSelect(Constants.StoryType.Image)
            dismiss()
        }
        linearVideo.setOnClickListener {
            storySelect.onStoryTypeSelect(Constants.StoryType.Video)
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}