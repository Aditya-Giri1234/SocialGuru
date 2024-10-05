package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.navigateUp
import androidx.viewpager2.widget.ViewPager2
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.databinding.PopUpChatScreenBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.NetworkManager.monitorInternet
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeDiscoverPostFragment
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeFollowingPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyCommentPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyLikedPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyPostFragment
import com.aditya.socialguru.ui_layer.viewmodel.profile.MyPostViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class MyActivityFragment : Fragment()  , AlertDialogOption {

    private var _binding:FragmentMyActivityBinding?=null
    private val binding get() = _binding!!

    private val jobQueue: ArrayDeque<()->Unit> = ArrayDeque()

    private val navController get() = (requireActivity() as MainActivity).navController

    // Don't use lazy it lead to memory leak and not leave old view when fragment switching and come back this view that time thi variable if initialize with lazy that not leave old view and crash app
    private var _pagerAdapter:NormalPagerAdapter?=null
    private val pagerAdapter get()=_pagerAdapter!!
    private var myLoader: MyLoader? = null
    private val args by navArgs<MyActivityFragmentArgs>()
    private val myPostViewModel by viewModels<MyPostViewModel>()
    private lateinit var userId:String
    private lateinit var userDetails:User
    private val pageScrollListener = object : ViewPager2.OnPageChangeCallback(){
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (_binding==null)  return
            if (position!=0){
                binding.myToolbar.icSetting.gone()
            }else{
                lifecycleScope.launch {
                    val postCount = myPostViewModel.postCount.first()
                    handleSettingHideShow(postCount)
                }
            }
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
       _binding=FragmentMyActivityBinding.inflate(layoutInflater)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        userId=args.userId
        userDetails = args.userDetail
        initUi()
        subscribeToObserver()
        getDataWithValidation()
    }

    private fun subscribeToObserver() {
        observeFlow {
            myPostViewModel.postCount.onEach {
                if(it == Int.MIN_VALUE){
                    handleSettingHideShow(0)
                    jobQueue.add {
                        getData()
                    }
                }else{
                    handleSettingHideShow(it)
                }
            }.launchIn(this)
            requireContext().monitorInternet().onEach { isInternetAvailable ->
                if(isInternetAvailable){
                    jobQueue.forEach {
                        it.invoke()
                    }
                    jobQueue.clear()
                }
            }.launchIn(this)
            myPostViewModel.deleteAllPost.onEach {response ->
                when(response){
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("All Post Deleted Successfully !" , isSuccess = true
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
        }
    }

    private fun getDataWithValidation(){
        if(!myPostViewModel.isDataLoaded){
            myPostViewModel.setDataLoadedStatus(true)
            getData()
        }
    }
    private fun getData() {
        myPostViewModel.subscribeToPostCount(userId)
    }


    private fun initUi() {
        binding.apply {

            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text= if (userId==AuthManager.currentUserId()!!)"My Activity" else userDetails.userName
            }

            setUpViewPager()
            setListener()
        }
    }

    private fun FragmentMyActivityBinding.setUpViewPager() {
        _pagerAdapter = NormalPagerAdapter(
            listOf(
                ShowMyPostFragment(userId),
                ShowMyCommentPostFragment(userId),
                ShowMyLikedPostFragment(userId)
            ), childFragmentManager, viewLifecycleOwner.lifecycle
        )
        viewPagerHome.apply {
            adapter = pagerAdapter
        }

        tabHome.apply {
            attachTo(viewPagerHome)
        }
        viewPagerHome.registerOnPageChangeCallback(pageScrollListener)
    }

    private fun FragmentMyActivityBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController?.navigateUp()
        }
        myToolbar.icSetting.setSafeOnClickListener {
            showPopupMenu()
        }
    }

    private fun handleSettingHideShow(postCount: Int) {
        if(postCount!=Int.MIN_VALUE&&postCount!=0&&userId==AuthManager.currentUserId()!!){
            binding.myToolbar.icSetting.myShow()
        }else{
            binding.myToolbar.icSetting.gone()
        }
    }
    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpChatScreenBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.myToolbar.icSetting)

        bindingPopUp.tvClear.text="Delete All Post "
        bindingPopUp.linearItemDeleteAll.setSafeOnClickListener {
            Constants.ChatDialogInvokeAction.ClearChat
            AlertDialog("Are your sure delete all post ?", this@MyActivityFragment, true).show(
                childFragmentManager,
                "MY_Dialog"
            )
            popUp.dismiss()
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



    override fun onResult(isYes: Boolean) {
        if (isYes){
            myPostViewModel.deleteAllMyPost()
        }
    }

    override fun onDestroyView() {
        binding.viewPagerHome.unregisterOnPageChangeCallback(pageScrollListener)
        _pagerAdapter=null
        binding.viewPagerHome.adapter = null
        _binding = null
        super.onDestroyView()
    }

}


