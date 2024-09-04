package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.model.notification.UserNotificationModel
import com.aditya.socialguru.databinding.FragmentNotificationBinding
import com.aditya.socialguru.databinding.PopUpNotificationFragmentBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.ui_layer.adapter.NotificationAdapter
import com.aditya.socialguru.ui_layer.fragment.post.DetailPostFragmentArgs
import com.aditya.socialguru.ui_layer.viewmodel.notification.NotificationViewModel
import com.vanniktech.ui.smoothScrollTo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class NotificationFragment : Fragment()  , AlertDialogOption{

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private val tagNotification = Constants.LogTag.Notification

    private var myLoader: MyLoader? = null
    private val navController get() = (requireActivity() as MainActivity).navController

    private var _notificationAdapter: NotificationAdapter? = null
    private val notificationAdapter get() = _notificationAdapter!!


    // This give me viewmodel which scope till nav back stack
    private val notificationViewModel: NotificationViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNotificationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _notificationAdapter = NotificationAdapter({
            onNotificationDelete(it)
        }) { data, isProfileView ->
            onNotificationClick(data, isProfileView)
        }
        initUi()
        subscribeToObserver()
        getData()

    }


    private fun subscribeToObserver() {
        observeFlow {
            notificationViewModel.notificationList.onEach { response ->
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
                        showSnackBar(response.message.toString(), false)
                    }
                }
            }.launchIn(this)

            notificationViewModel.singleNotificationDelete.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Notification Delete Successfully !", true)
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

            notificationViewModel.deleteAllNotification.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("All Notification Delete Successfully !", true)
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

            AppBroadcastHelper.homeFragmentBackToTopShow.onEach {
                if (it) {
                    MyLogger.w(tagNotification, msg = "Back To Top show event come!")
                    showBackToTopView()
                } else {
                    binding.linearBackToTop.gone()
                }
            }.launchIn(this)

            AppBroadcastHelper.homeScrollBackToTopClick.onEach {
                if (it) {
                    MyLogger.i(tagNotification, msg = "Back To Top click event come !")
                    binding.rvFriendRequest.smoothScrollTo(0)
                }
            }.launchIn(this)
        }
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                profileImage.gone()
                tvHeaderUserName.text = "Notification"
                icSetting.myShow()
            }

            rvFriendRequest.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = notificationAdapter
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        LinearLayoutManager.VERTICAL
                    )
                )
            }


            setListener()
        }
    }

    private fun FragmentNotificationBinding.setListener() {
        myToolbar.icSetting.setSafeOnClickListener {
            showPopupMenu()
        }

        linearBackToTop.setSafeOnClickListener {
            AppBroadcastHelper.setHomeScrollBackToTopClick(true)
        }

        rvFriendRequest.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            lifecycleScope.launch {
                val layoutManager = rvFriendRequest.layoutManager as? LinearLayoutManager
                val firstVisibleItemPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0

                if (scrollY > oldScrollY) {
                    //Scroll Down
                    MyLogger.v(tagNotification, msg = "Down scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(true)

                }
                if (scrollY < oldScrollY) {
                    //Scroll Up
                    MyLogger.v(tagNotification, msg = "Up scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(true)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)

                }

                if (scrollY == 0 && firstVisibleItemPosition == 0) {
                    //Top Scroll
                    MyLogger.v(tagNotification, msg = "Top Scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)
                }
            }

        }


    }

    private fun setData(list: List<UserNotificationModel> = emptyList()) {
        if (list.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
        }
        notificationAdapter.submitList(list)
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@NotificationFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getData() {
        MyLogger.w(tagNotification, msg = "Data is loaded ${notificationViewModel.isDataLoaded}")
        if (!notificationViewModel.isDataLoaded) {
            notificationViewModel.getMyNotificationAndListen()
            notificationViewModel.setDataLoadedStatus(true)
        }
    }

    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpNotificationFragmentBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.myToolbar.icSetting)

        if (notificationAdapter.isListIsEmpty()){
            bindingPopUp.viewDevider1.gone()
            bindingPopUp.linearItemDeleteAll.gone()
        }

        bindingPopUp.linearItemFriendRequest.setSafeOnClickListener {
            navigateToFriendRequest()
            popUp.dismiss()
        }

        bindingPopUp.linearItemDeleteAll.setSafeOnClickListener {
           AlertDialog("Are your sure delete All Notification ?",this@NotificationFragment,true).show(childFragmentManager,"MY_Dialog")
            popUp.dismiss()
        }


    }


    private fun onNotificationDelete(it: NotificationData) {
        it.notificationId?.let {
            notificationViewModel.deleteSingleNotification(it)
        }
    }

    private fun navigateToFriendRequest() {
        val directions: NavDirections =
            NotificationFragmentDirections.actionNotificationFragmentToFriendRequestFragment()
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }


    private fun navigateToProfileView(userId: String) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalProfileViewFragment(userId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun navigateToDetailPost(postId: String) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalDetailPostFragment(postId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun onNotificationClick(data: NotificationData, isProfileView: Boolean) {
        if (isProfileView) {
            data.friendOrFollowerId?.let {
                navigateToProfileView(it)
            }
        } else {
            data.postId?.let {
                navigateToDetailPost(it)
            }

        }
    }

    private fun showNoDataView() {
        binding.apply {
            rvFriendRequest.gone()
            noDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
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

    override fun onDestroyView() {
        _notificationAdapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onResult(isYes: Boolean) {
        if (isYes){
            notificationViewModel.deleteAllNotification()
        }
    }

}