package com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity

import android.net.Uri
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
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentShowMyLikedPostBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NetworkManager.monitorInternet
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.PostAdapter
import com.aditya.socialguru.ui_layer.fragment.post.DetailPostFragmentArgs
import com.aditya.socialguru.ui_layer.viewmodel.profile.MyPostViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class ShowMyLikedPostFragment(val userId:String) : Fragment(), OnPostClick {

    private var _binding: FragmentShowMyLikedPostBinding? = null
    private val binding get() = _binding!!

    private var _postAdapter: PostAdapter? = null
    private val postAdapter get() = _postAdapter!!

    private var myLoader: MyLoader? = null

    private val tagProfile = Constants.LogTag.Profile
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private val myPostViewModel by viewModels<MyPostViewModel>()

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
        _binding = FragmentShowMyLikedPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
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
                myPostViewModel.myLikedPost.onEach { response ->
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
                myPostViewModel.likePost.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            hideDialog()
                            response.hasBeenMessagedToUser = true
                        }

                        is Resource.Loading -> {
                            showDialog()
                        }

                        is Resource.Error -> {
                            response.hasBeenMessagedToUser = true
                            hideDialog()
                            postAdapter.notifyDataSetChanged()
                            Helper.showSnackBar(
                                (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                response.message.toString()
                            )
                        }
                    }
                }.launchIn(this)


                myPostViewModel.savePost.onEach {
                        response->
                    when(response){
                        is Resource.Success->{
                            hideDialog()
                            response.hasBeenMessagedToUser=true
                            showSnackBar(response.data?.errorMessage, isSuccess =
                            true)
                        }
                        is Resource.Loading->{
                            showDialog()
                        }
                        is Resource.Error->{
                            hideDialog()
                            response.hasBeenMessagedToUser=true
                            showSnackBar(response.message)
                        }

                    }
                }.launchIn(this)
            }
        }
    }

    private fun initUi() {
        _postAdapter = PostAdapter(this@ShowMyLikedPostFragment)
        binding.apply {
            rvLikedPost.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = postAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }
    }

    private fun FragmentShowMyLikedPostBinding.setListener() {
        linearBackToTop.setSafeOnClickListener {
            rvLikedPost.smoothScrollToPosition(0)
        }
        rvLikedPost.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            if (this@ShowMyLikedPostFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getData() {
        MyLogger.v(isFunctionCall = true)
        myPostViewModel.getMyLikedPost(userId)
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
            rvLikedPost.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvLikedPost.myShow()
        }
    }


    //region:: Post Operation

    override fun onResume() {
        super.onResume()
        // This done because when first viewpager have data and height increase then user come this fragment and this fragment data not loaded or not have data but it take previous fragment height so avoid this below line put.
        binding.root.requestLayout()
    }

    override fun onImageClick(): (Uri) -> Unit = {}

    override fun onVideoClick(): (Uri) -> Unit = {}

    override fun onLikeClick(post: Post) {
        if (userId==AuthManager.currentUserId()){
            //Currently i am seeing my post
            post.run {
                myPostViewModel.updateLikeCount(postId!!, userId!!, false)
            }
        }else{
            // Currently i am seeing other post
            val isLiked = post.likedUserList?.contains(AuthManager.currentUserId()!!) ?: false
            post.run {
                myPostViewModel.updateLikeCount(postId!!, AuthManager.currentUserId()!!, !isLiked)
            }
        }
    }

    override fun onCommentClick(postId: String) {
        navigateToDetailPostScreen(postId)
    }

    override fun onSettingClick(postId: String) {
        myPostViewModel.updatePostSaveStatus(postId)
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
        myPostViewModel.removeAllListener()
        _binding = null
        super.onDestroyView()
    }


}