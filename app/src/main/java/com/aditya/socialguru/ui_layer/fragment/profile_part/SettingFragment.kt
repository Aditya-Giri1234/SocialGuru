package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentSettingBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener


class SettingFragment : Fragment() {

    private var _binding:FragmentSettingBinding?=null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as MainActivity).navController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentSettingBinding.inflate(layoutInflater)
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
            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text="Setting Fragment"
            }
            setListener()
        }
    }

    private fun FragmentSettingBinding.setListener() {

        myToolbar.icBack.setSafeOnClickListener {
            navController?.navigateUp()
        }

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}


