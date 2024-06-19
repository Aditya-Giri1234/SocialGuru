package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aditya.socialguru.databinding.FragmentUserChatProfileBinding


class UserChatProfileFragment : Fragment() {

    private var _binding: FragmentUserChatProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserChatProfileBinding.inflate(layoutInflater)
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
            setListener()
        }
    }

    private fun FragmentUserChatProfileBinding.setListener() {

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }


}


