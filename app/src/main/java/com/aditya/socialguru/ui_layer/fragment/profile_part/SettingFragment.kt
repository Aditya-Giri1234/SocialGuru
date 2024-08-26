package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
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
import com.aditya.socialguru.ui_layer.viewmodel.profile.SettingViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class SettingFragment : Fragment() , AlertDialogOption  , DeleteAccountPasswordResult{

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val tagSetting = Constants.LogTag.Setting

    private var myLoader: MyLoader? = null
    private val settingViewModel by viewModels<SettingViewModel>()


    private val navController get() = (requireActivity() as MainActivity).navController
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
    }

    private fun handleDeleteAccount() {
        AlertDialog(
            "Are you sure delete your account ?",
            this@SettingFragment,
            isForShowDelete = false
        ).show(
            childFragmentManager,
            "MyAlertDialog"
        )

    }

    private fun navigateToOnboardingScreen() {
      /*  val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalOnboardingScreenFragment()
        navController.safeNavigate(
            directions,
            Helper.giveAnimationNavOption(R.id.homeFragment, true)
        )*/
        requireActivity().sendBroadcast(Intent(Constants.AppBroadCast.LogOut.name))
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
        if (isYes) {
            DeleteAccountPasswordDialog(
                this@SettingFragment
            ).show(
                childFragmentManager,
                "MyAlertDialog"
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResult(isSuccess: Boolean, errorMessage: String?) {
        runOnUiThread {
            if (isSuccess){
                settingViewModel.deleteAccount()
            }else{
                if (errorMessage!=null){
                    showSnackBar(errorMessage)
                }
            }
        }
    }

}


