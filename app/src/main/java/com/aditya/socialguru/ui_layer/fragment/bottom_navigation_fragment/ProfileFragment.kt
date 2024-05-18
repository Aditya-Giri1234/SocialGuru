package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.databinding.FragmentProfileBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.manager.MyLogger


class ProfileFragment : Fragment() {


    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            handleInitialization()
    }

    private fun handleInitialization() {

        initUI()

    }

    private fun initUI() {
        binding.apply {
            setListener()
        }
    }

    private fun FragmentProfileBinding.setListener() {
        tvClickMe.setOnClickListener {
            val directions: NavDirections =
                ProfileFragmentDirections.actionProfileFragmentToUpdateProfileFragment2()
            navController?.value?.navigate(directions, Helper.giveAnimationNavOption())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}