package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentSearchUserGlobalBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.helper.showKeyboard
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.adapter.post.UserAdapter
import com.aditya.socialguru.ui_layer.viewmodel.profile.ProfileViewModel
import com.vanniktech.ui.hideKeyboardAndFocus
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SearchUserGlobalFragment : Fragment() {

    private var _binding: FragmentSearchUserGlobalBinding? = null
    private val binding get() = _binding!!
    private val tagChat = Constants.LogTag.Chats
    private var lastSearch =""
    private var myLoader: MyLoader? = null
    private var _userAdapter: UserAdapter? = null
    private val userAdapter get() = _userAdapter!!

    private val profileViewModel by viewModels<ProfileViewModel>()

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchUserGlobalBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
    }

    private fun subscribeToObserver() {

        observeFlow {

            profileViewModel.findUsers.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideLoader()
                        response.data?.let {
                            setData(it)
                        } ?: run {
                            showNoDataView()
                        }
                    }

                    is Resource.Loading -> {
                        showLoader()
                    }

                    is Resource.Error -> {
                        showNoDataView()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            binding.etSearch.getQueryTextChangeStateFlow().debounce(100).distinctUntilChanged()
                .flatMapLatest { query ->
                    return@flatMapLatest flowOf(query.trim())
                }.onEach {
                    lastSearch=it
                    runOnUiThread {
                        if (it.isEmpty()) {
                            binding.icClose.gone()

                        }else{
                            binding.icClose.myShow()
                            profileViewModel.findUser(it)
                        }
                    }

                }.launchIn(this)
        }

    }

    private fun initUi() {
        _userAdapter = UserAdapter { user, view ->
            navigateToProfileScreen(user.userId!!)
        }
        binding.apply {
            if (lastSearch.isNotEmpty()){
                initialStateLayout.gone()
                expandedStateLayout.myShow()
                etSearch.setText(lastSearch)
                etSearch.requestFocus()
            }
            rvSearchUser.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = userAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }
    }

    private fun FragmentSearchUserGlobalBinding.setListener() {
        ivBack.setOnClickListener {
            navController.navigateUp()
        }

        linearBackToTop.setSafeOnClickListener {
            rvSearchUser.smoothScrollToPosition(0)
        }
        rvSearchUser.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    MyLogger.v(tagChat, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    MyLogger.v(tagChat, msg = "Up scroll")
                    showBackToTopView()
                } else {
                    MyLogger.v(tagChat, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })

        root.setOnTouchListener { v, event ->
            etSearch.hideKeyboardAndFocus()
            false
        }

        initialStateLayout.setOnClickListener {
            showExpandedState()
        }

        icClose.setOnClickListener {
            etSearch.text.clear()
            etSearch.hideKeyboardAndFocus()
        }

        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideExpandedState()
            }
        }
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@SearchUserGlobalFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun setData(list: List<FriendCircleData>) {
        if (list.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
            userAdapter.submitList(list)
        }
    }

    private fun FragmentSearchUserGlobalBinding.showExpandedState() {
        val animIn = ObjectAnimator.ofFloat(
            expandedStateLayout,
            "translationX",
            expandedStateLayout.width.toFloat(),
            0f
        )
        animIn.duration = 300
        animIn.start()

        initialStateLayout.visibility = View.GONE
        expandedStateLayout.visibility = View.VISIBLE

        etSearch.requestFocus()
        etSearch.showKeyboard()
    }

    private fun FragmentSearchUserGlobalBinding.hideExpandedState() {
        val animOut = ObjectAnimator.ofFloat(
            expandedStateLayout,
            "translationX",
            0f,
            expandedStateLayout.width.toFloat()
        )
        animOut.duration = 300
        animOut.start()

        animOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                initialStateLayout.visibility = View.VISIBLE
                expandedStateLayout.visibility = View.GONE
            }
        })

        etSearch.clearFocus()
    }

    private fun navigateToProfileScreen(id: String) {
        binding.etSearch.hideKeyboardAndFocus()
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalProfileViewFragment(id)
        navController.safeNavigate(directions)
    }

    private fun showLoader() {
        binding.noDataView.gone()
        binding.rvSearchUser.gone()
        binding.progressBar.myShow()
    }

    private fun hideLoader() {
        binding.progressBar.gone()
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

    private fun showNoDataView() {
        binding.apply {
            noDataView.myShow()
            rvSearchUser.gone()
            progressBar.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            noDataView.gone()
            rvSearchUser.myShow()
            progressBar.gone()
        }
    }

    override fun onPause() {
        hideKeyboard()
        super.onPause()
    }

    override fun onDestroyView() {
        hideKeyboard()
        _binding = null
        super.onDestroyView()
    }

}