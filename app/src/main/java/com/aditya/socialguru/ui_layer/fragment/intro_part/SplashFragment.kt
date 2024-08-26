package com.aditya.socialguru.ui_layer.fragment.intro_part

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentSplashBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.isResume
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.service.FirebaseManager


class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as MainActivity).navController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashBinding.inflate(layoutInflater)
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
        Handler(Looper.getMainLooper()).postDelayed({
            if (this@SplashFragment.isResume()){
                if (FirebaseManager.isUserLogin()) {
                    val directions: NavDirections =
                        SplashFragmentDirections.actionSplashFragmentToHomeFragmentBar()
                    navController.safeNavigate(
                        directions,
                        Helper.giveAnimationNavOption(R.id.splashFragment, true)
                    )
                } else {
                    val directions: NavDirections =
                        SplashFragmentDirections.actionSplashFragmentToOnboardingScreenFragment()
                    navController.safeNavigate(
                        directions,
                        Helper.giveAnimationNavOption(R.id.splashFragment, true)
                    )
                }
            }
        }, 1000)

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}