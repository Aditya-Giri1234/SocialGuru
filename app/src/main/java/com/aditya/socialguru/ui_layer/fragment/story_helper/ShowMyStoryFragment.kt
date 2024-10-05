package com.aditya.socialguru.ui_layer.fragment.story_helper

import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.databinding.FragmentShowMyStoryBinding
import com.aditya.socialguru.databinding.PopUpChatScreenBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NetworkManager.monitorInternet
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.MyStoryAdapter
import com.aditya.socialguru.ui_layer.viewmodel.ShowMyStoryViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class ShowMyStoryFragment : Fragment(), AlertDialogOption {

    private var _binding: FragmentShowMyStoryBinding? = null
    private val binding get() = _binding!!

    private val tagStory = Constants.LogTag.Story
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()

    private lateinit var userStories: UserStories
    private var _user: User? = null
    private val user get() = _user!!

    private var _stories: MutableList<Stories>? = null
    private val stories get() = _stories!!

    private lateinit var userId: String

    private var dialogInvokeType: DialogInvokeTypeForMyStoryScreen =
        DialogInvokeTypeForMyStoryScreen.ForSingleStatusDelete
    private var isDataLoaded = false
    private val args by navArgs<ShowMyStoryFragmentArgs>()

    private val navController get() = (requireActivity() as MainActivity).navController
    private val showMyStoryViewModel by viewModels<ShowMyStoryViewModel>()
    private var currentItem = -1

    private val pref by lazy {
        SharePref(requireContext())
    }

    private val myStoryAdapter by lazy {
        MyStoryAdapter {
            navigateToStatusView()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentShowMyStoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        userId = args.userId
        initUi()
        subscribeToObserver()
        if (!isDataLoaded) {
            getData()
        }

    }


    private fun subscribeToObserver() {
        observeFlow {
            showMyStoryViewModel.myStories.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            // show status
                            userStories = it
                            _user = it.user
                            _stories = it.stories
                            setData()

                        } ?: run {
                            // Show no status view
                            showNoStatusView()
                        }
                    }

                    is Resource.Loading -> {
                        MyLogger.v(tagStory, msg = "Story is loading ....")
                    }

                    is Resource.Error -> {
                        showNoStatusView()
                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.add {
                                        getData()
                                    }
                                }

                                else -> {
                                    showSnackBar(message = response.message)
                                }
                            }
                        }
                    }
                }

            }.launchIn(this)
            showMyStoryViewModel.deleteStories.onEach { response ->
                when (response) {

                    is Resource.Success -> {
                        Helper.hideLoader()
                        _stories?.remove(stories[currentItem])
                        myStoryAdapter.notifyDataSetChanged()

                        if (stories.isEmpty()) {
                            showNoStatusView()
                        }
                        // This help to handle or change work manager status
                        requireActivity().sendBroadcast(
                            Intent(Constants.AppBroadCast.StoryChange.name).putExtra(
                                Constants.DATA,
                                2
                            ).apply { setPackage(requireContext().packageName) })

                        Helper.showSuccessSnackBar(
                            (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                            "Status deleted successfully !"
                        )
                    }

                    is Resource.Loading -> {
                        MyLogger.v(tagStory, msg = "Story is loading ....")
                        Helper.showLoader(requireActivity())
                    }

                    is Resource.Error -> {
                        Helper.hideLoader()
                        showNoStatusView()
                        showSnackBar(message = response.message)
                    }
                }

            }.launchIn(this)
            showMyStoryViewModel.deleteAllStories.onEach { response ->
                when (response) {

                    is Resource.Success -> {
                        Helper.hideLoader()
                        showNoStatusView()
                        // This help to handle or change work manager status
                        requireActivity().sendBroadcast(
                            Intent(Constants.AppBroadCast.StoryChange.name).putExtra(
                                Constants.DATA,
                                2
                            ).apply { setPackage(requireContext().packageName) })

                        Helper.showSuccessSnackBar(
                            (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                            "All Status Deleted Successfully!"
                        )
                    }

                    is Resource.Loading -> {
                        Helper.showLoader(requireActivity())
                    }

                    is Resource.Error -> {
                        Helper.hideLoader()
                        showNoStatusView()
                        showSnackBar(message = response.message)
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
            rvStatus.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = myStoryAdapter
                if (userId == AuthManager.currentUserId()!!) {
                    setItemTouchListener()
                }
            }


            if (userId != AuthManager.currentUserId()!!) {
                tvHeader.text = "Stories"
            }


            setListener()
        }
    }

    private fun FragmentShowMyStoryBinding.setListener() {
        icBack.setOnClickListener {
            navController?.navigateUp()
        }
        icSetting.setSafeOnClickListener {
                showPopupMenu()
        }
    }


    private fun getData() {
        showMyStoryViewModel.getMyStory(userId)
    }

    private fun setItemTouchListener() {

        val deleteColor = resources.getColor(android.R.color.holo_red_light)
        val callColor = resources.getColor(android.R.color.holo_green_light)


        val swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            //more code here
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                currentItem = viewHolder.absoluteAdapterPosition
                dialogInvokeType = DialogInvokeTypeForMyStoryScreen.ForSingleStatusDelete
                AlertDialog("Are you sure delete this status ?", this@ShowMyStoryFragment).show(
                    childFragmentManager,
                    "MyAlertDialog"
                )
            }

        })

        swipeHelper.attachToRecyclerView(binding.rvStatus)
    }

    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpChatScreenBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.icSetting)

        bindingPopUp.tvClear.text = "Delete All Post "
        bindingPopUp.linearItemDeleteAll.setSafeOnClickListener {
            Constants.ChatDialogInvokeAction.ClearChat
            dialogInvokeType = DialogInvokeTypeForMyStoryScreen.ForAllStatusDelete
            AlertDialog("Are your sure delete all status ?", this@ShowMyStoryFragment, true).show(
                childFragmentManager,
                "MY_Dialog"
            )
            popUp.dismiss()
        }
    }

    private fun performDelete(position: Int) {
        stories[currentItem].storyId?.let { showMyStoryViewModel.deleteStoryById(it) }
    }

    private fun setData() {
        if (userId != AuthManager.currentUserId()) {
            binding.tvHeader.text = user.userName ?: "Stories"
        }
        binding.apply {
            if (stories.isNotEmpty()) {
                hideNoStatusView()
                myStoryAdapter.submitList(stories)
            } else {
                showNoStatusView()
            }

        }
    }

    private fun navigateToStatusView() {
        val directions: NavDirections =
            ShowMyStoryFragmentDirections.actionShowMyStoryFragmentToStoryShowFragment(userStories)
        navController?.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    override fun onResult(isYes: Boolean) {
        when (dialogInvokeType) {
            DialogInvokeTypeForMyStoryScreen.ForSingleStatusDelete -> {
                if (isYes) {
                    performDelete(currentItem)
                } else {
                    myStoryAdapter.notifyItemChanged(currentItem)
                }
            }

            DialogInvokeTypeForMyStoryScreen.ForAllStatusDelete -> {
                if (isYes) {
                showMyStoryViewModel.deleteAllStory()
                }
            }
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

    private fun showNoStatusView() {
        binding.apply {
            icSetting.gone()
            tvNoStatusView.myShow()
            rvStatus.gone()
        }
    }

    private fun hideNoStatusView() {
        binding.apply {
            icSetting.myShow()
            tvNoStatusView.gone()
            rvStatus.myShow()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}

private enum class DialogInvokeTypeForMyStoryScreen {
    ForSingleStatusDelete,
    ForAllStatusDelete
}

