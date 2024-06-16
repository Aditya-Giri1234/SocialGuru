package com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.databinding.FragmentShowMyCommentPostBinding
import com.aditya.socialguru.databinding.FragmentShowMyPostBinding
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.adapter.post.PostAdapter
import com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment.HomeFragmentDirections
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeDiscoverPostFragment
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeFollowingPostFragment
import com.aditya.socialguru.ui_layer.fragment.post.DetailPostFragmentArgs
import com.aditya.socialguru.ui_layer.viewmodel.profile.MyPostViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class ShowMyPostFragment : Fragment(), OnPostClick {

    private var _binding: FragmentShowMyPostBinding? = null
    private val binding get() = _binding!!

    private var _postAdapter: PostAdapter? = null
    private val postAdapter get() = _postAdapter!!

    private val tagProfile = Constants.LogTag.Profile
    private val myPostViewModel by viewModels<MyPostViewModel>()


    private val pref by lazy {
        SharePref(requireContext())
    }

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MyLogger.v(isFunctionCall = true)
        // Inflate the layout for this fragment
        _binding = FragmentShowMyPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MyLogger.v(isFunctionCall = true)
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        MyLogger.v(isFunctionCall = true)
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
                myPostViewModel.myPost.onEach { response ->
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
        MyLogger.v(isFunctionCall = true)
        _postAdapter = PostAdapter(this@ShowMyPostFragment)
        binding.apply {
            rvMyPost.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = postAdapter
            }
            setListener()
        }
    }


    private fun FragmentShowMyPostBinding.setListener() {

        linearBackToTop.setSafeOnClickListener {
            rvMyPost.smoothScrollToPosition(0)
        }
        rvMyPost.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState==RecyclerView.SCROLL_STATE_IDLE){
                    MyLogger.v(tagProfile, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy<0){
                    MyLogger.v(tagProfile, msg = "Up scroll")
                    showBackToTopView()
                }else{
                    MyLogger.v(tagProfile, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })



    }


    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@ShowMyPostFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getData() {
        MyLogger.v(isFunctionCall = true)
        viewLifecycleOwner.lifecycleScope.launch {
            pref.getPrefUser().first()?.let { user ->
                MyLogger.v(tagProfile, msg = user, isJson = true)
                user.userId?.let {
                    myPostViewModel.getMyPost(it)
                }
            }
        }
    }

    private fun setData(userPosts: List<UserPostModel> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

        if (userPosts.isEmpty()) {
            MyLogger.w(tagProfile, msg = "list is empty then show no data view !")
            showNoDataView()
        } else {
            MyLogger.v(msg = "Now data is set into homeFragment !")
            hideNoDataView()
            postAdapter.submitList(userPosts.toList())
        }
    }

    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvMyPost.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvMyPost.myShow()
        }
    }

    //region:: Override Function
    override fun onImageClick(): (Uri) -> Unit = {}

    override fun onVideoClick(): (Uri) -> Unit = {}

    override fun onLikeClick() {
    }

    override fun onCommentClick(postId: String) {
        navigateToDetailPostScreen(postId)
    }

    override fun onSettingClick() {
    }

    override fun onSendClick() {
    }

    override fun onPostClick(postId: String) {
        navigateToDetailPostScreen(postId)
    }



    //endregion

    private fun navigateToDetailPostScreen(postId: String) {
        navController.safeNavigate(R.id.myActivityFragment , R.id.detailPostFragment2, Helper.giveAnimationNavOption() ,
            DetailPostFragmentArgs(postId).toBundle())
    }


    override fun onDestroyView() {
        MyLogger.v(isFunctionCall = true)
        _binding = null
        super.onDestroyView()
    }


}