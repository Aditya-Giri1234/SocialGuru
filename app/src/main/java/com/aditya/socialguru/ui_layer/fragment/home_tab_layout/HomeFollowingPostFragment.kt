package com.aditya.socialguru.ui_layer.fragment.home_tab_layout

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentHomeFollowingPostBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NetworkManager.monitorInternet
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.PostAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.FollowingPostViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class HomeFollowingPostFragment : Fragment(), OnPostClick {
    private var _binding: FragmentHomeFollowingPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()

    private var _followingPostAdapter: PostAdapter? = null
    private var myLoader: MyLoader? = null
    private val followingPostAdapter get() = _followingPostAdapter!!


    private val followingPostViewModel: FollowingPostViewModel by activityViewModels()

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
        _binding = FragmentHomeFollowingPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()

        if (!followingPostViewModel.isDataLoaded) {
            getData() // This call again again when new fragment become top then back this fragment
            followingPostViewModel.setDataLoadedStatus(true)
        }
    }

    private fun subscribeToObserver() {
        observeFlow {
            followingPostViewModel.userPost.onEach { response ->
                response.let {
                    MyLogger.i(tagPost, msg = "Response coming in ui screen !")
                    when (response) {
                        is Resource.Success -> {
                            response.hasBeenMessagedToUser = true
                            response.data?.let {
                                setData(it)
                            } ?: run {
                                setData()
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
                                        showSnackBar(response.message.toString())
                                    }
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
            followingPostViewModel.likePost.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                    }

                    is Resource.Loading -> {

                    }

                    is Resource.Error -> {
                        followingPostAdapter.notifyDataSetChanged()
                        showSnackBar(response.message.toString())
                    }
                }
            }.launchIn(this)
            followingPostViewModel.savePost.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(
                            response.data?.errorMessage, isSuccess =
                            true
                        )
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
            followingPostViewModel.followingList.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        MyLogger.i(tagPost, msg = "Following User List response come !")
                        response.data?.let {
                            if (it.isEmpty()) {
                                setData()
                            } else {
                                followingPostViewModel.getFollowingPost(it.mapNotNull { it.userId })
                            }
                        } ?: run {
                            setData()
                            MyLogger.w(tagPost, msg = "Following list is empty !")
                        }
                    }

                    is Resource.Loading -> {
                        MyLogger.v(tagPost, msg = "Following List is fetching ...")
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
                                    showSnackBar(response.message.toString())
                                }
                            }
                        }
                    }
                }
            }.launchIn(this)
        }
    }

    private fun initUi() {
        _followingPostAdapter = PostAdapter(this@HomeFollowingPostFragment)
        binding.apply {
            rvFollowingPost.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = followingPostAdapter
            }
            setListener()
        }
    }


    private fun FragmentHomeFollowingPostBinding.setListener() {


    }


    private fun getData() {
        followingPostViewModel.getFollowingListAndListenChange()
    }

    private fun setData(userPosts: List<UserPostModel> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

        if (userPosts.isEmpty()) {
            MyLogger.w(tagPost, msg = "list is empty then show no data view !")
            showNoDataView()
        } else {
            MyLogger.v(msg = "Now data is set into homeFragment !")
            followingPostAdapter.submitList(userPosts.toList())
            hideNoDataView()
        }
    }

    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvFollowingPost.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvFollowingPost.myShow()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestLayout()
    }


    //region:: Override Part
    override fun onImageClick(): (Uri) -> Unit = {

    }

    override fun onVideoClick(): (Uri) -> Unit = {

    }

    override fun onLikeClick(post: Post) {
        val isLiked = post.likedUserList?.contains(AuthManager.currentUserId()!!) ?: false
        MyLogger.w(tagPost, msg = "User Liked this post before click :- $isLiked")
        post.run {
            followingPostViewModel.updateLikeCount(postId!!, userId!!, !isLiked)
        }

    }

    override fun onCommentClick(postId: String) {
        navigateToDetailPostScreen(postId)
    }

    override fun onSettingClick(postId: String) {
        followingPostViewModel.updatePostSaveStatus(postId)
    }

    override fun onSendClick(post: Post) {
        ShareManager.sharePost(requireContext(), post.postId!!)
    }

    override fun onPostClick(postId: String) {
        navigateToDetailPostScreen(postId)
    }


    //endregion

    private fun navigateToDetailPostScreen(postId: String) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalDetailPostFragment(postId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
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
        followingPostViewModel.removeListener()
        _followingPostAdapter = null
        binding.rvFollowingPost.adapter = null
        _binding = null
        super.onDestroyView()
    }
}