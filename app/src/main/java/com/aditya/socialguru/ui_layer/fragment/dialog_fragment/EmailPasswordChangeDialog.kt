package com.aditya.socialguru.ui_layer.fragment.dialog_fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.databinding.PasswordEmailChangeBottomLayoutBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.removeErrorOnTextChanged
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.profile.SettingViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class EmailPasswordChangeDialog : BottomSheetDialogFragment() {
    private var _binding: PasswordEmailChangeBottomLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheeOpenFor: String

    private var myLoader: MyLoader? = null

    private val settingViewModel by viewModels<SettingViewModel>()
    private val args by navArgs<EmailPasswordChangeDialogArgs>()


    private val navController get() = (requireActivity() as MainActivity).navController
    private val pref by lazy {
        SharePref(requireContext())
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = PasswordEmailChangeBottomLayoutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext() , R.style.MyTransparentBottomSheetDialogTheme)
    }

    private fun handleInitialization() {
        bottomSheeOpenFor = args.bottomSheetOpenFor
        initUi()
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        observeFlow {

            settingViewModel.changePassword.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Password Changed Successfully", isSuccess = true)
                        navController.navigateUp()
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            settingViewModel.updateEmail.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Update Email Send Successfully !", isSuccess = true)
                        navController.navigateUp()
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }
            }.launchIn(this)

            settingViewModel.resetPassword.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar("A password reset link has been sent to your current email address.", isSuccess = true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(response.message.toString())
                    }
                }

            }.launchIn(this)

        }
    }

    private fun initUi() {
        binding.apply {
            when(bottomSheeOpenFor){
                Constants.BottomSheetOpenFor.EmailUpdate.name ->{
                    linearUpdateEmail.myShow()
                }
                Constants.BottomSheetOpenFor.PasswordChange.name ->{
                    linearChangePassword.myShow()
                }
                Constants.BottomSheetOpenFor.PasswordReset.name -> {
                    linearResetPassword.myShow()
                }

            }

            dialog?.let {
                it.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            setListener()
        }
    }


    private fun PasswordEmailChangeBottomLayoutBinding.setListener() {
        icClose.setSafeOnClickListener {
            navController.navigateUp()
        }

        btnUpdateEmail.setSafeOnClickListener {
            handleUpdateEmail()
        }
        btnChangePassword.setSafeOnClickListener {
            handleChangePassword()
        }
        btnResetPassword.setSafeOnClickListener {
            handleResetPassword()
        }

        tilEmail.removeErrorOnTextChanged()
        tilPassword.removeErrorOnTextChanged()
        tilCurPassword.removeErrorOnTextChanged()
        tilNewPassword.removeErrorOnTextChanged()
        tilResetEmail.removeErrorOnTextChanged()
    }

    private fun handleResetPassword() {
        if (validateResetPassword()){
            settingViewModel.resetPassword(binding.tiEtResetEmail.text.toString())
        }
    }

    private fun handleChangePassword() {
        if (validateChangePassword()){
            settingViewModel.changePassword(binding.tiEtCurPassword.text.toString() ,binding.tiEtNewPassword.text.toString())
        }
    }

    private fun handleUpdateEmail() {
        if(validateUpdateEmail()){
            settingViewModel.updateEmail(binding.tiEtEmail.text.toString() , binding.tiEtPassword.text.toString())
        }
    }

    private fun validateUpdateEmail(): Boolean {
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

    private fun validateChangePassword(): Boolean {
        with(binding) {
            return when {

                tiEtCurPassword.text.isNullOrEmpty() -> {
                    tilCurPassword.customError("Current Password must not null !")
                }

                !Helper.isPasswordValid(tiEtCurPassword.text.toString()) -> {
                    tilCurPassword.customError(
                        "At least 8 characters long\n" +
                                "Contains at least one digit\n" +
                                "Contains at least one lowercase letter\n" +
                                "Contains at least one uppercase letter\n" +
                                "Contains at least one special character from @#\$%^&+=!\n" +
                                "Doesn't contain whitespace characters"
                    )
                }
                tiEtNewPassword.text.isNullOrEmpty() -> {
                    tilNewPassword.customError("Current Password must not null !")
                }

                !Helper.isPasswordValid(tiEtNewPassword.text.toString()) -> {
                    tilNewPassword.customError(
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

    private fun validateResetPassword(): Boolean {
        with(binding) {
            return when {

                tiEtResetEmail.text.isNullOrEmpty() -> {
                    tilResetEmail.customError("Email must not null !")
                }

                !Helper.isEmailValid(tiEtResetEmail.text.toString()) -> {
                    tilResetEmail.customError("Please enter valid email address!")
                }

                else -> {
                    true
                }

            }
        }
    }

    private fun showDialog() {
        myLoader?.dismiss()
        myLoader = MyLoader()
        myLoader?.show(childFragmentManager, "My_Loader")
    }

    private fun hideDialog() {
        myLoader?.dismiss()
        myLoader = null
    }


    private fun showSnackBar(message: String?, isSuccess: Boolean = false) {
        if (isSuccess) {
            Helper.showSuccessSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                    R.id.coordLayout
                ), message.toString()
            )
        } else {
            Helper.showSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                    R.id.coordLayout
                ), message.toString()
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

