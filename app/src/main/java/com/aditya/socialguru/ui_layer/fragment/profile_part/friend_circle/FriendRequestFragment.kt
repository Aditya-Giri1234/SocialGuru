package com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentFriendRequestBinding
import com.aditya.socialguru.databinding.PopUpFriendRequestBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.monitorInternet
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.ui_layer.adapter.profile.friend_circle.FriendRequestAdapter
import com.aditya.socialguru.ui_layer.viewmodel.profile.friend_circle.FriendViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class FriendRequestFragment : Fragment(), AlertDialogOption {

    private var _binding: FragmentFriendRequestBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null
    private var dialogInvokeType = FriendRequestDialogInvokeType.Accept_All
    private val navController get() = (requireActivity() as MainActivity).navController

    private var _friendRequestAdapter: FriendRequestAdapter? = null
    private val friendRequestAdapter get() = _friendRequestAdapter!!
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private val friendViewModel by viewModels<FriendViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendRequestBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _friendRequestAdapter = FriendRequestAdapter({
            navigateToProfileView(it)
        }) { it, isDecline ->
            onResultOfDeclineOrAccept(it, isDecline)
        }
        initUi()
        subscribeToObserver()
        getData()
    }


    private fun subscribeToObserver() {
        observeFlow {
            friendViewModel.friendRequestList.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            setData(it)
                        } ?: run {
                            setData()
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.add {
                                        friendViewModel.setDataLoadedStatus(false)
                                        getData()
                                    }
                                }

                                else -> {
                                    showSnackBar(response.message.toString(), false)
                                }
                            }
                        }
                    }
                }
            }.launchIn(this)
            friendViewModel.declineFriendRequest.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Friend Request Decline Successfully!", true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message.toString(), false)
                    }
                }
            }.launchIn(this)
            friendViewModel.acceptFriendRequest.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Friend Request Accept Successfully!", true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message.toString(), false)
                    }
                }
            }.launchIn(this)
            requireContext().monitorInternet().onEach { isInternetAvailable ->
                if (isInternetAvailable) {
                    jobQueue.forEach {
                        it.invoke()
                    }
                    jobQueue.clear()
                }
            }.launchIn(this)
        }
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                profileImage.gone()
                tvHeaderUserName.text = "Friend Request"
                icSetting.gone()
                icBack.myShow()
            }

            rvFriendRequest.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = friendRequestAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }

            setListener()
        }
    }

    private fun FragmentFriendRequestBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        myToolbar.icSetting.setSafeOnClickListener {
            showPopUp()
        }
    }


    private fun getData() {
        if (!friendViewModel.isDataLoaded) {
            friendViewModel.setDataLoadedStatus(true)
            friendViewModel.getAndListenFriendRequestComeEvent()
        }

    }

    private fun setData(friendCircleData: List<FriendCircleData> = emptyList()) {
        if (friendCircleData.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
        }
        friendRequestAdapter.submitList(friendCircleData)
    }

    private fun onResultOfDeclineOrAccept(userId: String, decline: Boolean) {
        if (decline) {
            friendViewModel.declineFriendRequest(userId)
        } else {
            friendViewModel.acceptFriendRequest(userId)
        }
    }

    private fun showPopUp() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpFriendRequestBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.myToolbar.icSetting)



        bindingPopUp.linearItemAcceptAllFriendRequest.setSafeOnClickListener {
            dialogInvokeType = FriendRequestDialogInvokeType.Accept_All
            AlertDialog(
                "Are your sure accept all friend request ?",
                this@FriendRequestFragment,
                true
            ).show(childFragmentManager, "MY_Dialog")
            popUp.dismiss()
        }

        bindingPopUp.linearItemDeleteAllFriendRequest.setSafeOnClickListener {
            dialogInvokeType = FriendRequestDialogInvokeType.Decline_All
            AlertDialog(
                "Are your sure decline all friend request ?",
                this@FriendRequestFragment,
                true
            ).show(childFragmentManager, "MY_Dialog")
            popUp.dismiss()
        }
    }

    private fun navigateToProfileView(userId: String) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalProfileViewFragment(userId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun showNoDataView() {
        binding.apply {
            myToolbar.icSetting.gone()
            rvFriendRequest.gone()
            noDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            myToolbar.icSetting.myShow()
            rvFriendRequest.myShow()
            noDataView.gone()
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

    private fun showSnackBar(message: String, isSuccess: Boolean) {
        if (isSuccess) {
            Helper.showSuccessSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(R.id.coordLayout),
                message
            )
        } else {
            Helper.showSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(R.id.coordLayout),
                message
            )
        }
    }

    override fun onResult(isYes: Boolean) {
        if (isYes) {
            when (dialogInvokeType) {
                FriendRequestDialogInvokeType.Accept_All -> {
                    friendViewModel.acceptAllFriendRequest(
                        friendRequestAdapter.getData().mapNotNull { it.userId })
                }

                FriendRequestDialogInvokeType.Decline_All -> {
                    friendViewModel.declineAllFriendRequest(
                        friendRequestAdapter.getData().mapNotNull { it.userId })
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.rvFriendRequest.adapter = null
        _friendRequestAdapter = null
        _binding = null
        super.onDestroyView()
    }

}

private enum class FriendRequestDialogInvokeType {
    Accept_All,
    Decline_All
}