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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.w(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(layoutInflater)
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
        subscribeToObserve()
    }

    private fun subscribeToObserve() {
        viewLifecycleOwner.lifecycleScope.launch {
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
            navController?.safeNavigate(
                SignUpFragmentDirections.actionSignUpFragmentToSignInFragment(),
                Helper.giveAnimationNavOption(R.id.signUpFragment, true)
            )

        }

        btnSignUp.setOnClickListener {
            MyLogger.d(tagLogin, msg = "User click sign up button !")
            if (validateData()) {
                MyLogger.i(
                    tagLogin,
                    msg = "All field is right so that now user creation request send !"
                )
                authViewModel.createUser(
                    User(
                        null,
                        tiEtName.getStringText(),
                        tiEtBio.getStringText(),
                        tiEtProfession.getStringText(),
                        tiEtEmail.getStringText(),
                        tiEtPassword.getStringText()
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
                    tilName.customError("Name must not null !")
                }

                tiEtProfession.text.isNullOrEmpty() -> {
                    tilProfession.customError("Profession must not null !")
                }

                tiEtBio.text.isNullOrEmpty() -> {
                    tilBio.customError("Bio must not null !")
                }

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