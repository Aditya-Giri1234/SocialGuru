package com.aditya.socialguru.ui_layer.fragment.intro_part

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.databinding.FragmentSignInBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.myDelay
import com.aditya.socialguru.domain_layer.helper.getStringText
import com.aditya.socialguru.domain_layer.helper.removeErrorOnTextChanged
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.AuthViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch


class SignInFragment : Fragment() {

    private val tagLogin = Constants.LogTag.LogIn
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val navController get() = (requireActivity() as MainActivity).navController
    private val pref by lazy {
        SharePref(requireContext())
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
        _binding = FragmentSignInBinding.inflate(layoutInflater)
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(isFunctionCall = true)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()

    }

    private fun subscribeToObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.loginStatus.collect {
                it?.let {
                    when (it) {
                        is Resource.Success -> {
                            Helper.hideLoader()
                            Helper.showSuccessSnackBar(
                                (requireActivity() as MainActivity).findViewById(
                                    R.id.coordLayout
                                ), "Login Successful !"
                            )
                            MyLogger.i(tagLogin, msg = it.data, isJson = true)
                            it.data?.let { data ->
                                pref.setPrefUser(data)
                            }
                            Handler(Looper.getMainLooper()).postDelayed({
                                requireActivity().sendBroadcast(Intent(Constants.AppBroadCast.LogIn.name).apply { setPackage(requireContext().packageName) })
                                val directions:NavDirections=SignInFragmentDirections.actionSignInFragmentToHomeFragmentBar()
                                navController.safeNavigate(directions,Helper.giveAnimationNavOption(R.id.signInFragment,true))
                            },200)

                        }

                        is Resource.Loading -> {
                            Helper.showLoader(requireActivity())
                        }

                        is Resource.Error -> {
                            Helper.hideLoader()
                            Helper.showSnackBar(
                                (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                it.message.toString()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initUi() {
        binding.apply {
            val originalText = tvForgetPassword.text.toString()
            val spannableString = SpannableString(originalText).apply {
                setSpan(UnderlineSpan(), 0, originalText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            tvForgetPassword.text = spannableString
            setListener()
        }
    }

    @OptIn(FlowPreview::class)
    private fun FragmentSignInBinding.setListener() {
        tvNavigateToSignUp.setOnClickListener {
            navController?.safeNavigate(
                SignInFragmentDirections.actionSignInFragmentToSignUpFragment(),
                Helper.giveAnimationNavOption(R.id.signInFragment, true)
            )
        }
        btnLogin.setOnClickListener {
            if (validateData()) {
                authViewModel.loginUser(tiEtEmail.getStringText(), tiEtPassword.getStringText())
            }
        }

        tvForgetPassword.setSafeOnClickListener {
            val direction: NavDirections =
                BottomNavigationBarDirections.actionGlobalEmailPasswordChangeDialog(Constants.BottomSheetOpenFor.PasswordReset.name)
            navController.safeNavigate(direction, Helper.giveUpAndBottomAnimationNavOption())
        }
        tilEmail.removeErrorOnTextChanged()
        tilPassword.removeErrorOnTextChanged()


    }

    private fun validateData(): Boolean {
        with(binding) {
            return when {

                tiEtEmail.text.isNullOrEmpty() -> {
                    tilEmail.customError("Please provide an email address.")
                }

                !Helper.isEmailValid(tiEtEmail.text.toString()) -> {
                    tilEmail.customError("The email address you entered is invalid. Please try again.")
                }

                tiEtPassword.text.isNullOrEmpty() -> {
                    tilPassword.customError("Please enter your password.")
                }

                !Helper.isPasswordValid(tiEtPassword.text.toString()) -> {
                    tilPassword.customError(
                        "Your password must meet the following criteria:\n" +
                                "- At least 8 characters in length\n" +
                                "- Include at least one digit\n" +
                                "- Include at least one lowercase letter\n" +
                                "- Include at least one uppercase letter\n" +
                                "- Include at least one special character (@#\$%^&+=!)\n" +
                                "- No whitespace characters"
                    )
                }

                else -> {
                    true
                }

            }
        }
    }


    override fun onDestroyView() {
        _binding = null
        MyLogger.v(isFunctionCall = true)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLogger.w(isFunctionCall = true)
    }
}