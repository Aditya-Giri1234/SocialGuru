package com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentShowMyCommentPostBinding
import com.aditya.socialguru.databinding.FragmentShowMyLikedPostBinding


class ShowMyCommentPostFragment(val userId:String) : Fragment() {
    private var _binding: FragmentShowMyCommentPostBinding?=null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding= FragmentShowMyCommentPostBinding.inflate(layoutInflater)
        return  binding.root
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

    private fun FragmentShowMyCommentPostBinding.setListener() {

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}