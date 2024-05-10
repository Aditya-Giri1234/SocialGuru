package com.aditya.socialguru.ui_layer.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentProfileBinding
import com.aditya.socialguru.databinding.FragmentUpdateProfileBinding


class UpdateProfileFragment : Fragment() {

    private var _binding:FragmentUpdateProfileBinding?=null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentUpdateProfileBinding.inflate(layoutInflater)
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

    private fun FragmentUpdateProfileBinding.setListener(){

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}