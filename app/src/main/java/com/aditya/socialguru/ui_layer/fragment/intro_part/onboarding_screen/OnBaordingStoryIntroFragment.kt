package com.aditya.socialguru.ui_layer.fragment.intro_part.onboarding_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.databinding.FragmentOnBaordingStoryIntroBinding


class OnBaordingStoryIntroFragment : Fragment() {

    private var _binding: FragmentOnBaordingStoryIntroBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentOnBaordingStoryIntroBinding.inflate(layoutInflater)
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

    private fun FragmentOnBaordingStoryIntroBinding.setListener(){

    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }
}