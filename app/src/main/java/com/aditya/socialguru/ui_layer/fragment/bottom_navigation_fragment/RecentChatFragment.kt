package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentAddPostBinding
import com.aditya.socialguru.databinding.FragmentRecentChatBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger


class RecentChatFragment : Fragment() {

    private var _binding: FragmentRecentChatBinding?=null
    private val binding get() = _binding!!

    private val tagChat=Constants.LogTag.Chats

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        MyLogger.v(isFunctionCall = true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentRecentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        binding.apply {
//            etSearch.setOnTouchListener { v, event ->
//                etSearch.focusable=EditText.FOCUSABLE
//                etSearch.requestFocus()
//                MyLogger.v(tagChat , msg = "Touch on search  view occurred !")
//
//                return@setOnTouchListener true
//            }
            icClose.setSafeOnClickListener {
                etSearch.text.clear()
                etSearch.clearFocus()
            }
            setListener()
        }
    }

    private fun FragmentRecentChatBinding.setListener(){

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }
}