package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentAddPostBinding
import com.aditya.socialguru.databinding.FragmentNotificationBinding
import com.aditya.socialguru.domain_layer.manager.MyLogger


class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding?=null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentNotificationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        binding.apply {
            setListener()
        }
    }

    private fun FragmentNotificationBinding.setListener(){


    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}