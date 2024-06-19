package com.aditya.socialguru.ui_layer.fragment.story_helper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger
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

    private lateinit var userStories: UserStories
    private var _user: User? = null
    private val user get() = _user!!

    private var _stories: MutableList<Stories>? = null
    private val stories get() = _stories!!

    private lateinit var userId: String

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
        userId=args.userId
        initUi()
        subscribeToObserver()
        if (!isDataLoaded) {
            getData()
        }

    }


    private fun subscribeToObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        response.hasBeenMessagedToUser = true
                        showNoStatusView()
                        Helper.showSnackBar(
                            (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                            response.message.toString()
                        )
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
                        Helper.showSnackBar(
                            (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                            response.message.toString()
                        )
                    }
                }

            }.launchIn(this)

        }
    }

    private fun initUi() {
        binding.apply {
            rvStatus.apply {
                layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                adapter = myStoryAdapter
                setItemTouchListener()
            }


            if (userId!= AuthManager.currentUserId()!!) {
                tvHeader.text = "Stories"
            }


            setListener()
        }
    }

    private fun FragmentShowMyStoryBinding.setListener() {
        icBack.setOnClickListener {
            navController?.navigateUp()
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
                AlertDialog("Are you sure delete this status ?", this@ShowMyStoryFragment).show(
                    childFragmentManager,
                    "MyAlertDialog"
                )
            }

        })

        swipeHelper.attachToRecyclerView(binding.rvStatus)
    }


    private fun performDelete(position: Int) {
        stories[currentItem].storyId?.let { showMyStoryViewModel.deleteStoryById(it) }
    }

    private fun setData() {
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
        if (isYes) {
            performDelete(currentItem)
        } else {
            myStoryAdapter.notifyItemChanged(currentItem)
        }
    }

    private fun showNoStatusView() {
        binding.apply {
            tvNoStatusView.myShow()
            rvStatus.gone()
        }
    }

    private fun hideNoStatusView() {
        binding.apply {
            tvNoStatusView.gone()
            rvStatus.myShow()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}


