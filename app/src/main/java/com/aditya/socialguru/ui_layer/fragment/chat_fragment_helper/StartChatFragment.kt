package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentChatBinding
import com.aditya.socialguru.databinding.FragmentStartChatBinding


class StartChatFragment : Fragment() {
    private var _binding: FragmentStartChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartChatBinding.inflate(layoutInflater)
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

    private fun FragmentStartChatBinding.setListener() {

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }
}