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
import com.aditya.socialguru.databinding.FragmentHomeDiscoverPostBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.PostAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.DiscoverPostViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class HomeDiscoverPostFragment : Fragment(), OnPostClick {


    private var _binding: FragmentHomeDiscoverPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post


    private var _discoverPostAdapter: PostAdapter? = null
    private var myLoader: MyLoader? = null
    private val discoverPostAdapter get() = _discoverPostAdapter!!


    private val discoverPostViewModel: DiscoverPostViewModel by activityViewModels()

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
        _binding = FragmentHomeDiscoverPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()

        if (!discoverPostViewModel.isDataLoaded) {
            getData()
            discoverPostViewModel.setDataLoadedStatus(true)
        }
    }


    private fun subscribeToObserver() {
        observeFlow {
            discoverPostViewModel.userPost.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        response.data?.let {
                            setData(it)
                        } ?: run {
                            setData()
                            showSnackBar(message = response.message)
                        }
                    }

                    is Resource.Loading -> {

                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        showSnackBar(message = response.message)
                    }
                }
            }.launchIn(this)
            discoverPostViewModel.likePost.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                    }

                    is Resource.Loading -> {

                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        discoverPostAdapter.notifyDataSetChanged()
                        showSnackBar(message = response.message)
                    }
                }
            }.launchIn(this)
            discoverPostViewModel.savePost.onEach { response ->
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
                        response.hasBeenMessagedToUser = true
                        showSnackBar(response.message)
                    }

                }
            }.launchIn(this)
        }
    }

    private fun initUi() {
        _discoverPostAdapter = PostAdapter(this@HomeDiscoverPostFragment)
        binding.apply {
            rvDiscoverPost.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = discoverPostAdapter
            }
            setListener()
        }
    }


    private fun FragmentHomeDiscoverPostBinding.setListener() {
    }


    private fun getData() {
        discoverPostViewModel.getDiscoverPost()
    }

    private fun setData(userPosts: List<UserPostModel> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

        if (userPosts.isEmpty()) {
            MyLogger.w(tagPost, msg = "list is empty then show no data view !")
            showNoDataView()
        } else {
            MyLogger.v(tagPost,msg = "Now data is set into homeFragment !")
            hideNoDataView()
        }

        discoverPostAdapter.submitList(userPosts.toList())
    }

    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvDiscoverPost.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvDiscoverPost.myShow()
        }
    }

    override fun onResume() {
        super.onResume()
        // This done because when first viewpager have data and height increase then user come this fragment and this fragment data not loaded or not have data but it take previous fragment height so avoid this below line put.
        binding.root.requestLayout()
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


    //region:: Override Part
    override fun onImageClick(): (Uri) -> Unit = {}

    override fun onVideoClick(): (Uri) -> Unit = {}

    override fun onLikeClick(post: Post) {
        val isLiked = post.likedUserList?.contains(AuthManager.currentUserId()!!) ?: false
        post.run {
            discoverPostViewModel.updateLikeCount(postId!!, userId!!, !isLiked)
        }
    }

    override fun onCommentClick(postId: String) {
        navigateToDetailPostScreen(postId)
    }

    override fun onSettingClick(postId: String) {
        discoverPostViewModel.updatePostSaveStatus(postId)
    }

    override fun onSendClick(post: Post) {
        ShareManager.sharePost(requireContext(),post.postId!!)
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

    override fun onDestroyView() {
//        discoverPostViewModel.removeListener()
        _discoverPostAdapter = null
        binding.rvDiscoverPost.adapter = null
        _binding = null
        super.onDestroyView()
    }

}


