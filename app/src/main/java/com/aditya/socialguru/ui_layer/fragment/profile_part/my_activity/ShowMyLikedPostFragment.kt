package com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.databinding.FragmentShowMyLikedPostBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener


class ShowMyLikedPostFragment : Fragment() {

    private var _binding: FragmentShowMyLikedPostBinding?=null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentShowMyLikedPostBinding.inflate(layoutInflater)
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

    private fun FragmentShowMyLikedPostBinding.setListener() {

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }


}