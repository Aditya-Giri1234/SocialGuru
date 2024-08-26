package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.single_chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentStartChatBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.adapter.chat.StartChatAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class StartChatFragment : Fragment() {
    private var _binding: FragmentStartChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private var _friendAdapter: StartChatAdapter? = null
    private val friendAdapter get() = _friendAdapter!!

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val chatViewModel by viewModels<ChatViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _friendAdapter = StartChatAdapter {
            onItemClick(it)
        }
        initUi()
        subscribeToObserver()
        getData()
    }

    private fun subscribeToObserver() {
        viewLifecycleOwner.observeFlow {
            chatViewModel.friendList.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        MyLogger.i(tagChat, msg = "Friend List response come !")
                        MyLogger.d(
                            tagChat, msg = response.data, isJson = true, jsonTitle = "Friend List"
                        )
                        response.data?.let {
                            setData(it)
                        } ?: run {
                            setData()
                            MyLogger.w(tagChat, msg = "Friend list is empty !")
                        }
                    }

                    is Resource.Loading -> {
                        MyLogger.v(tagChat, msg = "Friend List is fetching ...")
                    }

                    is Resource.Error -> {
                        MyLogger.e(
                            tagChat, msg = giveMeErrorMessage(
                                "Fetching Friend List", response.message.toString()
                            )
                        )

                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            showSnackBar(response.message?.toString())
                        }
                    }
                }
            }.launchIn(this)
        }
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                profileImage.gone()
                icBack.myShow()
                tvHeaderUserName.text = "Select Friend"
            }

            rvFriend.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = friendAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled=false
            }
            setListener()
        }
    }

    private fun FragmentStartChatBinding.setListener() {

        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }

        linearBackToTop.setSafeOnClickListener {
            rvFriend.smoothScrollToPosition(0)
        }
        rvFriend.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    MyLogger.v(tagChat, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    MyLogger.v(tagChat, msg = "Up scroll")
                    showBackToTopView()
                } else {
                    MyLogger.v(tagChat, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })
    }

    private fun setData(list: List<FriendCircleData> = emptyList()) {
        if (list.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
            friendAdapter.submitList(list)
        }
    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.getFriendListAndListenChange()
            chatViewModel.setDataLoadedStatus(true)
        }
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@StartChatFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }
    private fun onItemClick(user: User) {
        val directions:NavDirections=BottomNavigationBarDirections.actionGlobalChatFragment(user.userId!!)
        navController.safeNavigate(directions,Helper.giveAnimationNavOption(R.id.startChatFragment ,true))
    }


    private fun showNoDataView() {
        binding.apply {
            rvFriend.gone()
            noDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvFriend.myShow()
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


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}