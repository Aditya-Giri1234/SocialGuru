package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentAddPostBinding
import com.aditya.socialguru.databinding.FragmentRecentChatBinding
import com.aditya.socialguru.domain_layer.custom_class.dialog.SelectStartChatDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.chat.StartChatDialogOption
import com.vanniktech.ui.hideKeyboardAndFocus


class RecentChatFragment : Fragment(), StartChatDialogOption {

    private var _binding: FragmentRecentChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MyLogger.v(isFunctionCall = true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRecentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        binding.apply {
//            etSearch.setOnTouchListener { v, event ->
//                etSearch.focusable=EditText.FOCUSABLE
//                etSearch.requestFocus()
//                MyLogger.v(tagChat , msg = "Touch on search  view occurred !")
//
//                return@setOnTouchListener true
//            }
            icClose.setSafeOnClickListener {
                etSearch.text.clear()
                etSearch.hideKeyboardAndFocus()
            }
            setListener()
        }
    }

    private fun FragmentRecentChatBinding.setListener() {
        fBtnStartChat.setSafeOnClickListener {
            SelectStartChatDialog(this@RecentChatFragment).show(childFragmentManager,"My_Chat_Selected_Dialog")
        }
    }

    //region:: Ui override function
    override fun onSingleChatClick() {
        val direction: NavDirections =
            RecentChatFragmentDirections.actionRecentChatFragmentToStartChatFragment()
        navController.safeNavigate(direction, Helper.giveAnimationNavOption())
    }

    override fun onGroupChatClick() {

    }

    //endregion
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}