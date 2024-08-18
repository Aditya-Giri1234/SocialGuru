package com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.ui_layer.adapter.profile.friend_circle.FriendRequestAdapter
import com.aditya.socialguru.ui_layer.fragment.profile_part.ProfileViewFragmentArgs
import com.aditya.socialguru.ui_layer.viewmodel.profile.friend_circle.FriendViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class FriendRequestFragment : Fragment() {

    private var _binding: FragmentFriendRequestBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null
    private val navController get() = (requireActivity() as MainActivity).navController

    private var _friendRequestAdapter: FriendRequestAdapter? = null
    private val friendRequestAdapter get() = _friendRequestAdapter!!

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
        viewLifecycleOwner.observeFlow {
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
                        showSnackBar(response.message.toString(), false)
                    }
                }
            }.launchIn(this)
            friendViewModel.deleteFriendRequest.onEach { response ->
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
            friendRequestAdapter.submitList(friendCircleData)
        }

    }

    private fun onResultOfDeclineOrAccept(userId: String, decline: Boolean) {
        if (decline) {
            friendViewModel.declineFriendRequest(userId)
        } else {
            friendViewModel.acceptFriendRequest(userId)
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
        binding.rvFriendRequest.adapter = null
        _friendRequestAdapter = null
        _binding = null
        super.onDestroyView()
    }
}