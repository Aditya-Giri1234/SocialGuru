package com.aditya.socialguru.ui_layer.fragment.post

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentUserLikeLIstBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.adapter.post.UserAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.DetailPostViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class UserLikeLIstFragment : Fragment() {


    private var _binding: FragmentUserLikeLIstBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post

    private var _userAdapter: UserAdapter? = null
    private val userAdapter get() = _userAdapter!!
    private lateinit var postId: String

    private val args by navArgs<UserLikeLIstFragmentArgs>()


    private val myPostViewModel by viewModels<DetailPostViewModel>()

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
        _binding = FragmentUserLikeLIstBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        postId = args.postId
        initUi()
        subscribeToObservation()
        if (!myPostViewModel.isDataLoaded) {
            getData()
            myPostViewModel.setDataLoadedStatus(true)
        }
    }

    private fun subscribeToObservation() {
        MyLogger.v(isFunctionCall = true)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                myPostViewModel.userList.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.hasBeenMessagedToUser = true
                            response.data?.let {
                                setData(it)
                            } ?: run {
                                setData()
                                Helper.showSnackBar(
                                    (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                    response.message.toString()
                                )
                            }

                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            response.hasBeenMessagedToUser = true
                            Helper.showSnackBar(
                                (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                response.message.toString()
                            )
                        }
                    }
                }.launchIn(this)

            }
        }
    }

    private fun initUi() {
        _userAdapter = UserAdapter {it,view ->
            navigateToProfileViewScreen(it)
        }
        binding.apply {
            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text="Likes"
            }
            rvUserList.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = userAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }

    }

    private fun FragmentUserLikeLIstBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        linearBackToTop.setSafeOnClickListener {
            rvUserList.smoothScrollToPosition(0)
        }
        rvUserList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    MyLogger.v(tagPost, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    MyLogger.v(tagPost, msg = "Up scroll")
                    showBackToTopView()
                } else {
                    MyLogger.v(tagPost, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@UserLikeLIstFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getData() {
        MyLogger.v(isFunctionCall = true)
        myPostViewModel.getPostLikeUser(postId)
    }

    private fun setData(userPosts: List<FriendCircleData> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

        if (userPosts.isEmpty()) {
            MyLogger.w(tagPost, msg = "list is empty then show no data view !")
            showNoDataView()
        } else {
            MyLogger.v(msg = "Now data is set into homeFragment !")
            hideNoDataView()
            userAdapter.submitList(userPosts.toList())
        }
    }

    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvUserList.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvUserList.myShow()
        }
    }


    private fun navigateToProfileViewScreen(userId: String) {
        val directions: NavDirections =
            UserLikeLIstFragmentDirections.actionUserLikeLIstFragmentToProfileViewFragment3(userId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}