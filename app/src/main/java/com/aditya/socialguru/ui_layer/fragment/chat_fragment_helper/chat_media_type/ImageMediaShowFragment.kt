package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.chat_media_type

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.chat.ChatMediaData
import com.aditya.socialguru.databinding.FragmentAllMediaShowBinding
import com.aditya.socialguru.databinding.FragmentImageMediaShowBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.monitorInternet
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.adapter.chat.ChatMediaAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class ImageMediaShowFragment(val chatRoomId:String) : Fragment() {

    private var _binding: FragmentImageMediaShowBinding? = null
    private val binding get() = _binding!!

    private var _mediaAdapter: ChatMediaAdapter? = null
    private val mediaAdapter get() = _mediaAdapter!!

    private val tagChat= Constants.LogTag.Chats
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()

    private val chatViewModel by viewModels<ChatViewModel>()

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImageMediaShowBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _mediaAdapter = ChatMediaAdapter({
            onImageClick(it)
        }) {
            onVideoClick(it)
        }
        initUi()
        subscribeToObserver()
        getDataWithValidation()
    }

    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.chatMedia.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            MyLogger.v(tagChat, msg = it, isJson = true, jsonTitle = "All Images ")
                            it.takeIf { it.isNotEmpty() }?.let {
                                showData(it)
                            }
                        } ?: run {
                            showNoDataView()
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
                                        getData()
                                    }
                                }
                                else ->{
                                    showSnackBar(message = response.message)
                                }
                            }
                        }
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
            rvImageMedia.apply {
                val gridLayoutManager=
                    GridLayoutManager(requireContext(), 3)
                gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter?.getItemViewType(position)) {
                            ChatMediaAdapter.DateView -> 3 // Full width for date view
                            else -> 1 // Regular width for other views
                        }
                    }
                }

                layoutManager=gridLayoutManager
                adapter = mediaAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }
    }

    private fun FragmentImageMediaShowBinding.setListener() {
        linearBackToTop.setSafeOnClickListener {
            rvImageMedia.smoothScrollToPosition(0)
        }

        rvImageMedia.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    MyLogger.v(tagProfile, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
//                    MyLogger.v(tagProfile, msg = "Up scroll")
                    showBackToTopView()
                } else {
//                    MyLogger.v(tagProfile, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })
    }
    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@ImageMediaShowFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getDataWithValidation() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.setDataLoadedStatus(true)
            getData()
        }
    }
    private fun getData(){
        chatViewModel.getAllMediaForChat(chatRoomId, 1)
    }

    private fun showData(media: List<ChatMediaData>) {
        hideNoDataView()
        mediaAdapter.submitList(media)
    }

    private fun showNoDataView() {
        binding.apply {
            rvImageMedia.gone()
            tvNoDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvImageMedia.myShow()
            tvNoDataView.gone()
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

    private fun onImageClick(uri: Uri) {
        val direction: NavDirections =
            BottomNavigationBarDirections.actionGlobalShowImageFragment(uri)
        navController.safeNavigate(direction, Helper.giveAnimationNavOption())
    }

    private fun onVideoClick(uri: Uri) {
        val direction: NavDirections =
            BottomNavigationBarDirections.actionGlobalShowVideoFragment(uri)
        navController.safeNavigate(direction, Helper.giveAnimationNavOption())
    }

    override fun onDestroyView() {
        _mediaAdapter = null
        binding.rvImageMedia.adapter = null
        _binding = null
        super.onDestroyView()
    }


}