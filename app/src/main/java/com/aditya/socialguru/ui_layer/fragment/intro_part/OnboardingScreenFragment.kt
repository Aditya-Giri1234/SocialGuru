package com.aditya.socialguru.ui_layer.fragment.intro_part

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentOnboardingScreenBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.activity.IntroActivity
import com.aditya.socialguru.ui_layer.adapter.OnBoardingPagerAdapter
import com.aditya.socialguru.ui_layer.fragment.intro_part.onboarding_screen.OnBaordingStoryIntroFragment
import com.aditya.socialguru.ui_layer.fragment.intro_part.onboarding_screen.OnBoardingAppIntroFragment
import com.aditya.socialguru.ui_layer.fragment.intro_part.onboarding_screen.OnBoardingChatIntroFragment


class OnboardingScreenFragment : Fragment() {

    private var _binding: FragmentOnboardingScreenBinding? = null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as IntroActivity).navController

    private val pagerAdapter: OnBoardingPagerAdapter by lazy {
        OnBoardingPagerAdapter(
            listOf(
                OnBoardingAppIntroFragment(),
                OnBaordingStoryIntroFragment(),
                OnBoardingChatIntroFragment()
            ), requireActivity().supportFragmentManager,
            lifecycle
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.w(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingScreenBinding.inflate(layoutInflater)
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(isFunctionCall = true)
        handleInitialization()
    }

    private fun handleInitialization() {
        binding.apply {

            viewPager.apply {
                adapter = pagerAdapter
            }
            dotsIndicator.attachTo(viewPager)


            setListener()
        }
    }

    private fun FragmentOnboardingScreenBinding.setListener() {
        btnFinished.setOnClickListener {
            navController?.safeNavigate(
                OnboardingScreenFragmentDirections.actionOnboardingScreenFragmentToSignInFragment(),
                Helper.giveAnimationNavOption(R.id.onboardingScreenFragment,true)
            )
        }
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        _binding = null
        MyLogger.v(isFunctionCall = true)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLogger.w(isFunctionCall = true)
    }

}