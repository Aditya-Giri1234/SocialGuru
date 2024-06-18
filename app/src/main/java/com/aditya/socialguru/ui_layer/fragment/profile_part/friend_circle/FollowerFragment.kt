package com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentFollowerBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.profile.friend_circle.FriendAdapter
import com.aditya.socialguru.ui_layer.fragment.profile_part.ProfileViewFragmentArgs
import com.aditya.socialguru.ui_layer.viewmodel.profile.friend_circle.FriendViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class FollowerFragment : Fragment(), AlertDialogOption {

    private var _binding: FragmentFollowerBinding? = null
    private val binding get() = _binding!!

    private val tagProfile = Constants.LogTag.Profile

    private val followerViewModel by viewModels<FriendViewModel>()


    private var _followerAdapter: FriendAdapter? = null
    private val followerAdapter get() = _followerAdapter!!

    private var myLoader: MyLoader? = null
    private var removeUserId: String? = null

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFollowerBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _followerAdapter = FriendAdapter(false,{
            navigateToUserDetailsScreen(it)
        }) {
            AlertDialog(
                "Are you sure remove this follower ?",
                this@FollowerFragment,
                isForShowDelete = true
            ).show(
                childFragmentManager,
                "MyAlertDialog"
            )
            removeUserId = it
        }

        initUi()
        subscribeToObserver()
        if (!followerViewModel.isDataLoaded) {
            getData()
            followerViewModel.setDataLoadedStatus(true)
        }
    }


    private fun subscribeToObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                followerViewModel.followerList.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            MyLogger.v(tagProfile, msg = "Follower List response come !")
                            MyLogger.v(
                                tagProfile,
                                msg = response.data,
                                isJson = true,
                                jsonTitle = "Follower List"
                            )
                            response.data?.let {
                                setData(it)
                            } ?: run {
                                setData()
                                MyLogger.w(tagProfile, msg = "Follower list is empty !")
                            }
                        }

                        is Resource.Loading -> {
                            MyLogger.v(tagProfile, msg = "Follower List is fetching ...")
                        }

                        is Resource.Error -> {
                            MyLogger.e(
                                tagProfile,
                                msg = giveMeErrorMessage(
                                    "Fetching Follower List",
                                    response.message.toString()
                                )
                            )

                            if (!response.hasBeenMessagedToUser) {
                                response.hasBeenMessagedToUser = true
                                showSnackBar(response.message?.toString())
                            }
                        }
                    }
                }.launchIn(this)
                followerViewModel.removeFollower.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            hideDialog()
                            MyLogger.v(tagProfile, msg = "Follower is removed !")
                            showSnackBar("Follower Removed Successfully !", isSuccess = true)
                        }

                        is Resource.Loading -> {
                            MyLogger.v(tagProfile, msg = "Follower is removing ...")
                            showDialog()
                        }

                        is Resource.Error -> {
                            hideDialog()
                            MyLogger.e(
                                tagProfile,
                                msg = response.message
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
    }


    private fun initUi() {
        binding.apply {
            rvFollower.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = followerAdapter
            }
            setListener()
        }
    }

    private fun FragmentFollowerBinding.setListener() {

        linearBackToTop.setSafeOnClickListener {
            rvFollower.smoothScrollToPosition(0)
        }
        rvFollower.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    MyLogger.v(tagProfile, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    MyLogger.v(tagProfile, msg = "Up scroll")
                    showBackToTopView()
                } else {
                    MyLogger.v(tagProfile, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@FollowerFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun setData(list: List<FriendCircleData> = emptyList()) {
        if (list.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
            followerAdapter.submitList(list)
        }
    }

    private fun getData() {
        followerViewModel.getFollowerAndListenChangeEvent()
    }

    private fun showNoDataView() {
        binding.apply {
            rvFollower.gone()
            noDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvFollower.myShow()
            noDataView.gone()
        }
    }

    private fun removeFollower(userId: String) {


        followerViewModel.removeFollower(userId)
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

    private fun showDialog() {
        myLoader?.dismiss()
        myLoader = MyLoader()
        myLoader?.show(childFragmentManager, "My_Loader")
    }

    private fun hideDialog() {
        myLoader?.dismiss()
        myLoader = null
    }

    private fun navigateToUserDetailsScreen(userId: String) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalProfileViewFragment(userId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    override fun onResult(isYes: Boolean) {
        if (isYes) {
            removeUserId?.let {
                removeFollower(it)
            }
        }
    }

    override fun onDestroyView() {
        binding.rvFollower.adapter = null
        _followerAdapter = null
        _binding = null
        super.onDestroyView()
    }


}