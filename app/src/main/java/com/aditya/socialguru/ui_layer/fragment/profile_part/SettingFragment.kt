package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.UserSetting
import com.aditya.socialguru.databinding.FragmentSettingBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.DeleteAccountPasswordDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.DeleteAccountPasswordResult
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.profile.SettingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class SettingFragment : Fragment(), AlertDialogOption, DeleteAccountPasswordResult {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val tagSetting = Constants.LogTag.Setting
    private var defaultDialogOption: SettingDialogOption = SettingDialogOption.OnResetPassword

    private var myLoader: MyLoader? = null

    private val settingViewModel by viewModels<SettingViewModel>()


    private val navController get() = (requireActivity() as MainActivity).navController
    private val pref by lazy {
        SharePref(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
        loadDefaultSetting()
    }


    private fun subscribeToObserver() {
        observeFlow {
            settingViewModel.deleteAccount.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar("Account Deleted Successfully", isSuccess = true)
                        Helper.setLogout(requireContext())
                        navigateToOnboardingScreen()
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

            settingViewModel.settingUpdate.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar("Setting Update Successfully !", isSuccess = true)
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
            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text = "Setting"
            }
            setListener()
        }
    }

    private fun FragmentSettingBinding.setListener() {

        myToolbar.icBack.setSafeOnClickListener {
            navController?.navigateUp()
        }

        btnDelete.setSafeOnClickListener {
            handleDeleteAccount()
        }

        cardUpdateEmailSetting.setSafeOnClickListener {
            openBottomSheet(Constants.BottomSheetOpenFor.EmailUpdate)
        }
        cardChangePasswordSetting.setSafeOnClickListener {
            openBottomSheet(Constants.BottomSheetOpenFor.PasswordChange)
        }
        cardResetPasswordSetting.setSafeOnClickListener {
            handleResetPassword()
        }
        cardNotificationSetting.setSafeOnClickListener {
            openSettingDialog(it.x.toLong() , it.y.toLong())
        }

        switchRemoveStory.setSafeOnClickListener {

            // This help to handle or change work manager status
            val intValueOfChecked = if (switchRemoveStory.isChecked) 1 else 0
            requireActivity().sendBroadcast(
                Intent(Constants.AppBroadCast.StoryChange.name).putExtra(
                    Constants.DATA,
                    intValueOfChecked
                ).apply { setPackage(requireContext().packageName) }
            )
            updateUserSetting(switchRemoveStory.isChecked)
        }

        switchLastSeen.setSafeOnClickListener {
            updateUserSetting(isMyOnlineStatusHideEnable = switchLastSeen.isChecked)
        }


    }


    private fun handleDeleteAccount() {
        defaultDialogOption = SettingDialogOption.OnAccountDelete
        AlertDialog(
            "Are you sure delete your account ?",
            this@SettingFragment,
            isForShowDelete = false
        ).show(
            childFragmentManager,
            "MyAlertDialog"
        )

    }

    private fun handleResetPassword() {
        defaultDialogOption = SettingDialogOption.OnResetPassword
        AlertDialog(
            "A password reset link will be sent to your current email address. Are you sure you want to proceed with resetting your password?",
            this@SettingFragment,
            isForShowDelete = false
        ).show(
            childFragmentManager,
            "MyAlertDialog"
        )

    }

    private fun loadDefaultSetting() {
        lifecycleScope.launch {
            pref.getPrefUser().first()?.let {
                it.userSetting?.let { setting ->
                    binding.apply {
                        switchRemoveStory.isChecked =
                            setting.isStoryRemoveAfter24HourActive ?: false
                        switchLastSeen.isChecked = setting.isMyOnlineStatusHideEnable ?: false
                    }
                }
            }
        }
    }

    private fun navigateToOnboardingScreen() {
        /*  val directions: NavDirections =
              BottomNavigationBarDirections.actionGlobalOnboardingScreenFragment()
          navController.safeNavigate(
              directions,
              Helper.giveAnimationNavOption(R.id.homeFragment, true)
          )*/
        requireActivity().sendBroadcast(Intent(Constants.AppBroadCast.LogOut.name).apply { setPackage(requireContext().packageName) })
    }

    private fun updateUserSetting(
        isStoryRemoveAfter24HourActive: Boolean? = null,
        isMyOnlineStatusHideEnable: Boolean? = null
    ) {
        lifecycleScope.launch {
            pref.getPrefUser().first()?.let { user ->
                val updatedSettings = user.userSetting?.copy(
                    isStoryRemoveAfter24HourActive = isStoryRemoveAfter24HourActive
                        ?: user.userSetting?.isStoryRemoveAfter24HourActive,
                    isMyOnlineStatusHideEnable = isMyOnlineStatusHideEnable
                        ?: user.userSetting?.isMyOnlineStatusHideEnable
                ) ?: UserSetting(
                    isStoryRemoveAfter24HourActive = isStoryRemoveAfter24HourActive ?: false,
                    isMyOnlineStatusHideEnable = isMyOnlineStatusHideEnable ?: false
                )
                updatedSettings.let { settingViewModel.updateUserSetting(it) }
            }
        }
    }

    private fun openBottomSheet(reason: Constants.BottomSheetOpenFor) {
        val direction: NavDirections =
            BottomNavigationBarDirections.actionGlobalEmailPasswordChangeDialog(reason.name)
        navController.safeNavigate(direction, Helper.giveUpAndBottomAnimationNavOption())
    }

    private fun openSettingDialog(x: Long, y:Long) {
        val direction: NavDirections =
            BottomNavigationBarDirections.actionGlobalNotificationSettingDialog(startX = x, startY = y)
        navController.safeNavigate(direction)
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

    override fun onResult(isYes: Boolean) {
        when (defaultDialogOption) {
            SettingDialogOption.OnAccountDelete -> {
                if (isYes) {
                    DeleteAccountPasswordDialog(
                        this@SettingFragment
                    ).show(
                        childFragmentManager,
                        "MyAlertDialog"
                    )
                }
            }

            SettingDialogOption.OnResetPassword -> {
               /* showSnackBar(
                    "A password reset link has been sent to your current email address.",
                    isSuccess = true
                )*/
                if (isYes){
                    settingViewModel.resetPassword(null)
                }
            }
        }
    }


    override fun onResult(isSuccess: Boolean, errorMessage: String?) {
        runOnUiThread {
            if (isSuccess) {
                settingViewModel.deleteAccount()
            } else {
                if (errorMessage != null) {
                    showSnackBar(errorMessage)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}

private enum class SettingDialogOption {
    OnAccountDelete,
    OnResetPassword
}


