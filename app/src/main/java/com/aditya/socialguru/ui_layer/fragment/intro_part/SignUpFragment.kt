package com.aditya.socialguru.ui_layer.fragment.intro_part

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.databinding.FragmentSignUpBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.myDelay
import com.aditya.socialguru.domain_layer.helper.getStringText
import com.aditya.socialguru.domain_layer.helper.removeErrorOnTextChanged
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.AuthViewModel
import kotlinx.coroutines.launch


class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val tagLogin = Constants.LogTag.LogIn

    private val navController get() = (requireActivity() as MainActivity).navController

    private val authViewModel: AuthViewModel by viewModels()

    private val pref by lazy {
        SharePref(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserve()
    }

    private fun subscribeToObserve() {
        observeFlow {
            authViewModel.signUpStatus.collect{
                it?.let { response ->
                    when (response) {
                        is Resource.Success -> {
                            MyLogger.i(tagLogin, msg = "User created successfully !")
                            Helper.hideLoader()
                            Helper.showSuccessSnackBar(
                                (requireActivity() as MainActivity).findViewById(
                                    R.id.coordLayout
                                ), "Successfully account created !"
                            )
                            response.data?.let {data->
                                MyLogger.v(tagLogin, msg = "Data is not empty and data saved in preference !")
                                pref.setPrefUser(data)
                            }

                            Handler(Looper.getMainLooper()).postDelayed({
                                MyLogger.v(tagLogin, msg = "Now go to main activity !")
                                requireActivity().sendBroadcast(Intent(Constants.AppBroadCast.LogIn.name).apply { setPackage(requireContext().packageName) })
                                val directions: NavDirections =SignUpFragmentDirections.actionSignUpFragmentToHomeFragmentBar()
                                navController.safeNavigate(directions,Helper.giveAnimationNavOption(R.id.signUpFragment,true))
                            },200)
                        }

                        is Resource.Loading -> {
                            MyLogger.v(tagLogin, msg = "Create User process is ongoing..")
                            Helper.showLoader(requireActivity())
                        }

                        is Resource.Error -> {
                            MyLogger.e(
                                tagLogin,
                                msg = "Some error occurred during user creation :- ${response.message}"
                            )
                            Helper.hideLoader()
                            Helper.showSnackBar(
                                (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                response.message.toString()
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

    private fun FragmentSignUpBinding.setListener() {
        tvNavigateToSignIn.setOnClickListener {

            // Here dynamically pop because xml part is not work and keep in mind back stack is working on stack principal lifo.
            navController.safeNavigate(
                SignUpFragmentDirections.actionSignUpFragmentToSignInFragment(),
                Helper.giveAnimationNavOption(R.id.signUpFragment, true)
            )

        }

        btnSignUp.setOnClickListener {
            if (validateData()) {
                authViewModel.createUser(
                    User(
                        userId = null,
                        userName = tiEtName.getStringText(),
                        userNameLowerCase = tiEtName.getStringText().lowercase(),
                        userBio = tiEtBio.getStringText(),
                        userProfession = tiEtProfession.getStringText(),
                        userEmailId = tiEtEmail.getStringText(),
                        userPassword = tiEtPassword.getStringText()
                    )
                )
            }
        }

        tilName.removeErrorOnTextChanged()
        tilProfession.removeErrorOnTextChanged()
        tilBio.removeErrorOnTextChanged()
        tilEmail.removeErrorOnTextChanged()
        tilPassword.removeErrorOnTextChanged()
    }

    private fun validateData(): Boolean {
        with(binding) {

            return when {
                tiEtName.text.isNullOrEmpty() -> {
                    tilName.customError("Please enter your name.")
                }

                tiEtProfession.text.isNullOrEmpty() -> {
                    tilProfession.customError("Please specify your profession.")
                }

                tiEtBio.text.isNullOrEmpty() -> {
                    tilBio.customError("A short bio is required.")
                }

                tiEtEmail.text.isNullOrEmpty() -> {
                    tilEmail.customError("Please provide your email address.")
                }

                !Helper.isEmailValid(tiEtEmail.text.toString()) -> {
                    tilEmail.customError("The email address you entered is invalid. Please check and try again.")
                }

                tiEtPassword.text.isNullOrEmpty() -> {
                    tilPassword.customError("Please enter a password.")
                }

                !Helper.isPasswordValid(tiEtPassword.text.toString()) -> {
                    tilPassword.customError(
                        "Your password must meet the following criteria:\n" +
                                "- At least 8 characters long\n" +
                                "- Includes at least one digit\n" +
                                "- Includes at least one lowercase letter\n" +
                                "- Includes at least one uppercase letter\n" +
                                "- Includes at least one special character (@#\$%^&+=!)\n" +
                                "- No whitespace characters allowed"
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
        super.onDestroyView()
    }
}