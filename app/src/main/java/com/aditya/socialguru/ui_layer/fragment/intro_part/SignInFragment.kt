package com.aditya.socialguru.ui_layer.fragment.intro_part

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.activity.ContainerActivity
import com.aditya.socialguru.ui_layer.viewmodel.AuthViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch


class SignInFragment : Fragment() {

    private val tagLogin = Constants.LogTag.LogIn
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val navController get() = (requireActivity() as ContainerActivity).navController
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
        lifecycleScope.launch {
            authViewModel.loginStatus.collect {
                it?.let {
                    when (it) {
                        is Resource.Success -> {
                            Helper.hideLoader()
                            Helper.showSuccessSnackBar(
                                (requireActivity() as ContainerActivity).findViewById(
                                    R.id.coordLayout
                                ), "Login Successful !"
                            )
                            MyLogger.i(tagLogin, msg = it.data, isJson = true)
                            it.data?.let { data ->
                                pref.setPrefUser(data)
                            }
                            myDelay(200) {
                                Intent(
                                    requireActivity(),
                                    MainActivity::class.java
                                ).also(::startActivity)
                                requireActivity().overridePendingTransition(
                                    R.anim.slide_in_right,R.anim.slide_out_left
                                )
                                requireActivity().finish()
                            }
                        }

                        is Resource.Loading -> {
                            Helper.showLoader(requireActivity())
                        }

                        is Resource.Error -> {
                            Helper.hideLoader()
                            Helper.showSnackBar(
                                (requireActivity() as ContainerActivity).findViewById(R.id.coordLayout),
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
            MyLogger.v(tagLogin, msg = "User click login button !")
            if (validateData()) {
                MyLogger.i(tagLogin, msg = "All is ok now login to user request send !")
                authViewModel.loginUser(tiEtEmail.getStringText(), tiEtPassword.getStringText())
            }
        }

        tilEmail.removeErrorOnTextChanged()
        tilPassword.removeErrorOnTextChanged()


    }

    private fun validateData(): Boolean {
        with(binding) {

            return when {

                tiEtEmail.text.isNullOrEmpty() -> {
                    tilEmail.customError("Email must not null !")
                }

                !Helper.isEmailValid(tiEtEmail.text.toString()) -> {
                    tilEmail.customError("Please enter valid email address!")
                }

                tiEtPassword.text.isNullOrEmpty() -> {
                    tilPassword.customError("Password must not null !")
                }


                !Helper.isPasswordValid(tiEtPassword.text.toString()) -> {
                    tilPassword.customError(
                        "At least 8 characters long\n" +
                                "Contains at least one digit\n" +
                                "Contains at least one lowercase letter\n" +
                                "Contains at least one uppercase letter\n" +
                                "Contains at least one special character from @#\$%^&+=!\n" +
                                "Doesn't contain whitespace characters"
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