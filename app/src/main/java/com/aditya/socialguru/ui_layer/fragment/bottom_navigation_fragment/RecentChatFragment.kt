package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.chat.UserRecentModel
import com.aditya.socialguru.databinding.FragmentRecentChatBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.SelectStartChatDialog
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.chat.StartChatDialogOption
import com.aditya.socialguru.ui_layer.adapter.chat.RecentChatAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.vanniktech.ui.hideKeyboardAndFocus
import com.vanniktech.ui.smoothScrollTo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class RecentChatFragment : Fragment(), StartChatDialogOption {

    private var _binding: FragmentRecentChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private var _recentChatAdapter: RecentChatAdapter? = null
    private var myLoader: MyLoader? = null
    private val recentChatAdatper get() = _recentChatAdapter!!

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val chatViewModel: ChatViewModel by navGraphViewModels(R.id.bottom_navigation_bar) {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MyLogger.v(isFunctionCall = true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRecentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _recentChatAdapter = RecentChatAdapter({
            onItemClick(it)
        }) {
            onItemLongClick(it)
        }
        initUi()
        subscribeToObserver()
        getData()

    }


    private fun subscribeToObserver() {
        viewLifecycleOwner.observeFlow {
            chatViewModel.recentChat.onEach {response->
                when(response){
                    is Resource.Success -> {
                        response.data?.let {
                            setData(it)
                        }
                    }
                    is Resource.Loading -> {

                    }
                    is Resource.Error -> {

                    }
                }

            }.launchIn(this)
            AppBroadcastHelper.homeFragmentBackToTopShow.onEach {
                if (it) {
                    MyLogger.w(tagChat, msg = "Back To Top show event come!")
                    showBackToTopView()
                } else {
                    binding.linearBackToTop.gone()
                }
            }.launchIn(this)

            AppBroadcastHelper.homeScrollBackToTopClick.onEach {
                if (it) {
                    MyLogger.i(tagChat, msg = "Back To Top click event come !")
                    binding.rvRecentChat.smoothScrollTo(0)
                }
            }.launchIn(this)
        }
    }



    private fun initUi() {
        binding.apply {
            rvRecentChat.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = recentChatAdatper
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }
    }



    private fun FragmentRecentChatBinding.setListener() {

        /* etSearch.setOnTouchListener { v, event ->
             etSearch.focusable = EditText.FOCUSABLE
             etSearch.requestFocus()
             MyLogger.v(tagChat, msg = "Touch on search  view occurred !")

             return@setOnTouchListener true
         }*/

        linearBackToTop.setSafeOnClickListener {
            AppBroadcastHelper.setHomeScrollBackToTopClick(true)
        }

        rvRecentChat.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            lifecycleScope.launch {
                val layoutManager = rvRecentChat.layoutManager as? LinearLayoutManager
                val firstVisibleItemPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0

                if (scrollY > oldScrollY) {
                    //Scroll Down
                    MyLogger.v(tagChat, msg = "Down scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(true)

                }
                if (scrollY < oldScrollY) {
                    //Scroll Up
                    MyLogger.v(tagChat, msg = "Up scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(true)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)

                }

                if (scrollY == 0 && firstVisibleItemPosition == 0) {
                    //Top Scroll
                    MyLogger.v(tagChat, msg = "Top Scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)
                }
            }

        }


        icClose.setSafeOnClickListener {
            etSearch.text.clear()
            etSearch.hideKeyboardAndFocus()
        }
        fBtnStartChat.setSafeOnClickListener {
            SelectStartChatDialog(this@RecentChatFragment).show(
                childFragmentManager,
                "My_Chat_Selected_Dialog"
            )
        }
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@RecentChatFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.setDataLoadedStatus(true)
            chatViewModel.getRecentChat()
        }
    }

    private fun setData(list: List<UserRecentModel>) {
        if (list.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
            recentChatAdatper.submitList(list)
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

    private fun showNoDataView() {
        binding.apply {
            rvRecentChat.gone()
            noDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvRecentChat.myShow()
            noDataView.gone()
        }
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

    //region:: Ui override function
    override fun onSingleChatClick() {
        val direction: NavDirections =
            RecentChatFragmentDirections.actionRecentChatFragmentToStartChatFragment()
        navController.safeNavigate(direction, Helper.giveAnimationNavOption())
    }

    override fun onGroupChatClick() {

    }

    private fun onItemClick(data: UserRecentModel) {
        data.user?.userId?.let {
            val direction: NavDirections =
                BottomNavigationBarDirections.actionGlobalChatFragment(it)
            navController.safeNavigate(direction, Helper.giveAnimationNavOption())
        }
    }

    private fun onItemLongClick(data: UserRecentModel) {
        //Show Pop or Dialog
    }

    //endregion
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}