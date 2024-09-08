package com.aditya.socialguru.ui_layer.fragment.dialog_fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.UserSetting
import com.aditya.socialguru.databinding.NotificationSettingDialogBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.getScreenHeight
import com.aditya.socialguru.domain_layer.helper.getScreenWidth
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hide
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.profile.SettingViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class NotificationSettingDialog : DialogFragment() {
    private var _binding: NotificationSettingDialogBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null
    private var job: Job? = null
    private var userData: User? = null

    private var startX by Delegates.notNull<Long>()
    private var startY by Delegates.notNull<Long>()
    private val args: NotificationSettingDialogArgs by navArgs()
    private val settingViewModel by viewModels<SettingViewModel>()

    private val pref by lazy {
        SharePref(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startX = args.startX
        startY = args.startY
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = NotificationSettingDialogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Access the dialog's window to apply animations
        dialog.window?.setWindowAnimations(R.style.DialogAnimation)
        return dialog
    }


    private fun handleInitialization() {
        initUi()
        subscribeObserver()
    }


    private fun subscribeObserver() {
        observeFlow {
            pref.getPrefUser().onEach {
                userData = it
                loadDefaultSetting()
            }.launchIn(this)
            settingViewModel.settingUpdate.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar("Setting Update Successfully !", isSuccess = true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        loadDefaultSetting()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(response.message.toString())
                    }
                }
            }.launchIn(this)
        }
    }

    private fun initUi() {
        binding.apply {
            dialog?.let {
                it.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            setListener()
        }
    }

    private fun NotificationSettingDialogBinding.setListener() {
        tvStatus.setSafeOnClickListener {
            tvStatus.gone()
        }
        switchSingleChat.setSafeOnClickListener {
            updateUserSetting(0, switchSingleChat.isChecked)
        }
        switchGroupChat.setSafeOnClickListener {
            updateUserSetting(1, switchGroupChat.isChecked)
        }
        switchPost.setSafeOnClickListener {
            updateUserSetting(2, switchPost.isChecked)
        }
        switchFriendCircle.setSafeOnClickListener {
            updateUserSetting(3, switchFriendCircle.isChecked)
        }
    }


    private fun loadDefaultSetting() {
        userData?.let {
            val userSetting = it.userSetting
            binding.apply {
                if (userSetting != null) {
                    switchSingleChat.isChecked = userSetting.isSingleChatNotificationMute ?: false
                    switchGroupChat.isChecked = userSetting.isGroupChatNotificationMute ?: false
                    switchPost.isChecked = userSetting.isPostNotificationMute ?: false
                    switchFriendCircle.isChecked =
                        userSetting.isFriendCircleNotificationMute ?: false
                } else {
                    switchSingleChat.isChecked = false
                    switchGroupChat.isChecked = false
                    switchPost.isChecked = false
                    switchFriendCircle.isChecked = false
                }
            }
        }
    }

    private fun updateUserSetting(
        order: Int,
        value: Boolean
    ) {
        userData?.let { user ->
            val updatedSettings = when (order) {
                0 -> {
                    user.userSetting?.copy(
                        isSingleChatNotificationMute = value
                    ) ?: UserSetting(
                        isSingleChatNotificationMute = false
                    )
                }

                1 -> {
                    user.userSetting?.copy(
                        isGroupChatNotificationMute = value
                    ) ?: UserSetting(
                        isGroupChatNotificationMute = false
                    )
                }

                2 -> {
                    user.userSetting?.copy(
                        isPostNotificationMute = value
                    ) ?: UserSetting(
                        isPostNotificationMute = false
                    )
                }

                else -> {
                    user.userSetting?.copy(
                        isFriendCircleNotificationMute = value
                    ) ?: UserSetting(
                        isFriendCircleNotificationMute = false
                    )
                }
            }
            updatedSettings.let { settingViewModel.updateUserSetting(it) }
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
        // Cancel any previous job before starting a new one
        job?.cancel()

        // Calculate the display duration based on message length
        val timeLength = message?.length?.times(80) ?: 1000

        // Update UI elements based on whether it's a success or an error
        binding.apply {
            tvStatus.apply {
                setBackgroundColor(
                    if (isSuccess) resources.getColor(
                        R.color.green,
                        null
                    ) else Color.RED
                )
                text = message
                showCustomSnackBar(timeLength)
            }
        }
    }

    private fun showCustomSnackBar(timeLength: Int) {
        // First, change constraints to move cardSingleChat below tvStatus
        changeConstraints(show = true)

        // Animate tvStatus slide in (custom slow-fast-slow animation)
        binding.tvStatus.myShow()
        val slideInAnimator = ObjectAnimator.ofFloat(
            binding.tvStatus,
            "translationY",
            -binding.tvStatus.height.toFloat(),
            0f
        )
        slideInAnimator.interpolator = AccelerateDecelerateInterpolator()
        slideInAnimator.duration = 300
        slideInAnimator.start()

        slideInAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {}

            override fun onAnimationEnd(p0: Animator) {
                // Once the snackbar is shown, start a coroutine to hide it after timeLength
                job = lifecycleScope.launch {
                    delay(timeLength.toLong())
                    hideCustomSnackBar()
                }
            }

            override fun onAnimationCancel(p0: Animator) {}

            override fun onAnimationRepeat(p0: Animator) {}
        })
    }

    private fun hideCustomSnackBar() {
        // Animate tvStatus slide out
        val slideOutAnimator = ObjectAnimator.ofFloat(
            binding.tvStatus,
            "translationY",
            0f,
            -binding.tvStatus.height.toFloat()
        )
        slideOutAnimator.interpolator = AccelerateDecelerateInterpolator()
        slideOutAnimator.duration = 300
        slideOutAnimator.start()

        slideOutAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {}

            override fun onAnimationEnd(p0: Animator) {
                // After hiding, change constraints back and make tvStatus invisible
                binding.tvStatus.hide()
                changeConstraints(show = false)
            }

            override fun onAnimationCancel(p0: Animator) {}

            override fun onAnimationRepeat(p0: Animator) {}
        })
    }

    private fun changeConstraints(show: Boolean) {
        val constraintSet = ConstraintSet()
        // Get the current margin of cardSingleChat
        val topMargin =
            (binding.cardSingleChat.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        constraintSet.clone(binding.mainConst) // Clone the current layout constraints

        if (show) {
            constraintSet.clear(binding.cardSingleChat.id, ConstraintSet.TOP)
            // Move cardSingleChat below tvStatus
            constraintSet.connect(
                binding.cardSingleChat.id,
                ConstraintSet.TOP,
                binding.tvStatus.id,
                ConstraintSet.BOTTOM,
                topMargin
            )
        } else {
            // Move cardSingleChat back to top of the parent

            constraintSet.clear(
                binding.cardSingleChat.id,
                ConstraintSet.TOP
            ) // Remove top-to-bottom constraint
            constraintSet.connect(
                binding.cardSingleChat.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                topMargin
            )
        }


        TransitionManager.beginDelayedTransition(binding.mainConst, getCustomSlideTransition())
        constraintSet.applyTo(binding.mainConst)
    }

    private fun getCustomSlideTransition(): Transition {
        return ChangeBounds().apply {
            duration = 100L
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}


