package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
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
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.SelectStartChatDialog
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.helper.showKeyboard
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.StartChatDialogOption
import com.aditya.socialguru.ui_layer.adapter.chat.RecentChatAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.vanniktech.ui.hideKeyboardAndFocus
import com.vanniktech.ui.smoothScrollTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class RecentChatFragment : Fragment(), StartChatDialogOption, AlertDialogOption {

    private var _binding: FragmentRecentChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats
    private var isFilterModeOn = false
    private var lastSearch =""

    private var _recentChatAdapter: RecentChatAdapter? = null
    private val recentChatAdaptper get() = _recentChatAdapter!!

    private var myLoader: MyLoader? = null
    private var currentRecentChatDelete: UserRecentModel? = null

    private val recentChatList = mutableListOf<UserRecentModel>()

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


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun subscribeToObserver() {
        viewLifecycleOwner.observeFlow {
            chatViewModel.recentChat.onEach { response ->
                when (response) {
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
            chatViewModel.deleteRecentChat.onEach { response->

                when(response){
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Deleted Successfully !" , isSuccess = true)
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

            binding.etSearch.getQueryTextChangeStateFlow().debounce(100).distinctUntilChanged()
                .flatMapLatest { query ->
                    return@flatMapLatest flowOf(query.trim())
                }.onEach {
                val filteredList = recentChatAdaptper.giveMeFilterList(it, recentChatList)
                runOnUiThread {
                    lastSearch=it
                    if (it.isEmpty()) {
                        binding.icClose.gone()
                        isFilterModeOn = false
                    } else {
                        binding.icClose.myShow()
                        isFilterModeOn = true
                    }

                    if (filteredList.isEmpty()) {
                        showNoDataView()
                    } else {
                        hideNoDataView()
                        recentChatAdaptper.submitList(filteredList)
                    }
                }
            }.launchIn(this)
        }
    }


    private fun initUi() {
        binding.apply {
            if(lastSearch.isNotEmpty()){
                initialStateLayout.gone()
                expandedStateLayout.myShow()
                etSearch.setText(lastSearch)
                etSearch.requestFocus()
            }
            rvRecentChat.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = recentChatAdaptper
//                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }
    }


    private fun FragmentRecentChatBinding.setListener() {

        root.setOnTouchListener { v, event ->
            etSearch.hideKeyboardAndFocus()
            false
        }

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
                    if(!etSearch.isFocused){
//                        reduceMarginFromRoot()
                        AppBroadcastHelper.setMainActivityBottomNavHideByScroll(true)
                    }

                }
                if (scrollY < oldScrollY) {
                    //Scroll Up
                    MyLogger.v(tagChat, msg = "Up scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(true)
                    if(!etSearch.isFocused){
//                        resetMarginFromRoot()
                        AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)
                    }

                }

                if (scrollY == 0 && firstVisibleItemPosition == 0) {
                    //Top Scroll
                    MyLogger.v(tagChat, msg = "Top Scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                    if(!etSearch.isFocused){
//                        resetMarginFromRoot()
                        AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)

                    }
                }
            }
        }

        icClose.setOnClickListener {
            etSearch.text.clear()
            etSearch.hideKeyboardAndFocus()
        }

        initialStateLayout.setOnClickListener {
            showExpandedState()
        }

        icClose.setOnClickListener {
            etSearch.text.clear()
            etSearch.hideKeyboardAndFocus()
        }

        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
//                reduceMarginFromRoot()
//                AppBroadcastHelper.setMainActivityBottomNavHideByScroll(true)
            }else{
//                resetMarginFromRoot()
//                AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)
                hideExpandedState()
            }

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
        recentChatList.clear()
        recentChatList.addAll(list)
        if (!isFilterModeOn) {
            if (list.isEmpty()) {
                showNoDataView()
            } else {
                hideNoDataView()
                recentChatAdaptper.submitList(list)
            }
        }
    }

    private fun FragmentRecentChatBinding.showExpandedState() {
        val animIn = ObjectAnimator.ofFloat(
            expandedStateLayout,
            "translationX",
            expandedStateLayout.width.toFloat(),
            0f
        )
        animIn.duration = 300
        animIn.start()

        initialStateLayout.visibility = View.GONE
        expandedStateLayout.visibility = View.VISIBLE

        etSearch.requestFocus()
        etSearch.showKeyboard()
    }

    private fun FragmentRecentChatBinding.hideExpandedState() {
        val animOut = ObjectAnimator.ofFloat(
            expandedStateLayout,
            "translationX",
            0f,
            expandedStateLayout.width.toFloat()
        )
        animOut.duration = 300
        animOut.start()

        animOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                initialStateLayout.visibility = View.VISIBLE
                expandedStateLayout.visibility = View.GONE
            }
        })

        etSearch.clearFocus()
    }

    private fun reduceMarginFromRoot(){
        val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(params.leftMargin,params.topMargin,params.rightMargin, com.intuit.sdp.R.dimen._5sdp)
        binding.root.layoutParams = params
    }

    private fun resetMarginFromRoot(){
        val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(params.leftMargin,params.topMargin,params.rightMargin, com.intuit.sdp.R.dimen._30sdp)
        binding.root.layoutParams = params
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
        val direction: NavDirections =
            BottomNavigationBarDirections.actionGlobalStartGroupChatFragment(
                Constants.MembersAddType.StartGroup.name,
                chatRoomId = null
            )
        navController.safeNavigate(direction, Helper.giveAnimationNavOption())
    }

    private fun onItemClick(data: UserRecentModel) {
        val isGroupChat = data.recentChat?.isGroupChat?.takeIf { it } ?: false
        val chatRoomId = data.groupInfo?.chatRoomId
        val userId = data.user?.userId
        if (isGroupChat) {
            chatRoomId?.let {
                val direction: NavDirections =
                    BottomNavigationBarDirections.actionGlobalGroupChatFragment(chatRoomId)
                navController.safeNavigate(direction, Helper.giveAnimationNavOption())
            }

        } else {
            userId?.let {
                val direction: NavDirections =
                    BottomNavigationBarDirections.actionGlobalChatFragment(it)
                navController.safeNavigate(direction, Helper.giveAnimationNavOption())
            }

        }

    }

    private fun onItemLongClick(data: UserRecentModel) {
        currentRecentChatDelete = data
        //Show Pop or Dialog
        AlertDialog(
            "Are you sure to delete this chat ?",
            this@RecentChatFragment,
            isForShowDelete = true
        ).show(
            childFragmentManager,
            "MyAlertDialog"
        )
    }

    override fun onResult(isYes: Boolean) {
        if (isYes) {
            if (currentRecentChatDelete!=null){
                chatViewModel.deleteRecentChat(currentRecentChatDelete!!.recentChat!!.chatRoomId!!)
            }
        }
    }

    //endregion
    override fun onDestroyView() {
        binding.etSearch.hideKeyboardAndFocus()
        _binding = null
        super.onDestroyView()
    }


}