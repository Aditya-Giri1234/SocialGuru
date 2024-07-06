package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.Users
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentStartGroupChatBinding
import com.aditya.socialguru.databinding.SampleStartGroupChatUserChipBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.adapter.chat.StartChatAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class StartGroupChatFragment : Fragment() {


    private var _binding: FragmentStartGroupChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private val allUser = mutableListOf<FriendCircleData>()
    private val selectedUser = mutableListOf<User>()
    private val notSelectedUser = mutableListOf<FriendCircleData>()

    private var _friendAdapter: StartChatAdapter? = null
    private val friendAdapter get() = _friendAdapter!!

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val chatViewModel by viewModels<ChatViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentStartGroupChatBinding.inflate(layoutInflater)
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
                        validateUserAndSetData(response.data)
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
                tvHeaderUserName.text = "Choose Friends"
            }

            rvAllUsers.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = friendAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }

        checkAndShowSelectedUser()

    }


    private fun FragmentStartGroupChatBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        btnNext.setOnClickListener {
            handleNextClick()
        }
    }


    /**
     * [checkAndShowSelectedUser] help me  when user navigate to other fragment then come back again then show previous selected user.
     * */
    private fun checkAndShowSelectedUser() {
        selectedUser.forEach {
            addIntoChip(it)
        }
    }

    private fun validateUserAndSetData(data: List<FriendCircleData>?) {
        allUser.clear()
        notSelectedUser.clear()
        if (data == null) {
            selectedUser.clear()
            setData()
            MyLogger.w(tagChat, msg = "Friend list is empty !")
        } else {
            allUser.addAll(data)
            selectedUser.forEachIndexed { index, user ->
                // Just remove view which is not a friend now
                if (data.none { it.user?.userId == user.userId }) {
                    binding.chipSelectedUsers.removeViewAt(index)
                    selectedUser.remove(user)
                }
            }
            notSelectedUser.addAll(
                allUser.filter { user ->
                    selectedUser.none { it.userId == user.user?.userId }
                }
            )
            setData(notSelectedUser)
        }
    }

    private fun setData(
        list: List<FriendCircleData> = emptyList()
    ) {
        if (allUser.isEmpty()) {
            showNoDataViewForChip()
        }

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

    private fun handleNextClick() {
        if (selectedUser.isEmpty()) {
            showSnackBar("At least 1 friend need to be selected !")
        } else {
            val directions: NavDirections =
                StartGroupChatFragmentDirections.actionStartGroupChatFragmentToMakingGroupFragment(
                    Users(selectedUser)
                )
            navController.navigate(directions, Helper.giveAnimationNavOption())
        }
    }

    private fun onItemClick(user: User) {
        selectedUser.add(user)
        notSelectedUser.remove(allUser.find { it.user?.userId == user.userId })
        addIntoChip(user)
    }

    private fun addIntoChip(user: User) {
        val chipBinding = SampleStartGroupChatUserChipBinding.inflate(layoutInflater)
        chipBinding.apply {
            chipText.text = user.userName
            Glide.with(circularImage).load(user.userProfileImage).placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user).into(circularImage)
            binding.chipSelectedUsers.addView(this.root)
            closeIcon.setOnClickListener {
                selectedUser.remove(user)
                allUser.find { it.user?.userId == user.userId }
                    ?.let { it1 -> notSelectedUser.add(it1) }
                binding.chipSelectedUsers.removeView(this.root)
                updateSelectedView()
            }
        }
        updateSelectedView()
    }

    private fun updateSelectedView() {
        binding.apply {
            if (selectedUser.isEmpty()) {
                showNoDataViewForChip()
            } else {
                hideNoDataViewForChip()
            }
        }
        setData(notSelectedUser)
    }

    private fun showNoDataViewForChip() {
        binding.apply {
            chipSelectedUsers.removeAllViews()
            scrollChipGroup.gone()
            tvNoDataViewForSelectedUser.myShow()
        }
    }

    private fun hideNoDataViewForChip() {
        binding.apply {
            scrollChipGroup.myShow()
            tvNoDataViewForSelectedUser.gone()
        }
    }

    private fun showNoDataView() {
        binding.apply {
            rvAllUsers.gone()
            tvNoDataViewForAllUser.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvAllUsers.myShow()
            tvNoDataViewForAllUser.gone()
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
        _friendAdapter = null
        _binding = null
        super.onDestroyView()
    }


}