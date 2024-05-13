package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.databinding.FragmentSupportHomeBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger

class SupportHomeFragment : Fragment() {

    private var _binding: FragmentSupportHomeBinding? = null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as MainActivity).navController?.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSupportHomeBinding.inflate(layoutInflater)
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        navController?.safeNavigate(SupportHomeFragmentDirections.actionSupportHomeFragmentToHomeFragment(),Helper.givePopUpNavOption(R.id.supportHomeFragment,true))
    }


}