package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentAddPostBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.show
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.android.material.floatingactionbutton.FloatingActionButton


class AddPostFragment : Fragment() {


    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private var interpolator: OvershootInterpolator = OvershootInterpolator()

    private val closeFabMenu = 0
    private val translationY = 100f
    private val tagPost = Constants.LogTag.Post


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(tagPost, isFunctionCall = true)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddPostBinding.inflate(layoutInflater)
        MyLogger.v(tagPost, isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(tagPost, isFunctionCall = true)
        handleInitialization()
    }

    private fun handleInitialization() {
        MyLogger.v(tagPost, isFunctionCall = true)
        initUi()
    }

    private fun initUi() {
        MyLogger.v(tagPost, isFunctionCall = true)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                0,
                systemBars.top,
                0,
                0
            )
            insets
        }
        binding.apply {
            setFab()
            setListener()
        }
    }

    private fun setFab() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            fabImage.alpha = 0f
            fabVideo.alpha = 0f
            fabImage.translationY = translationY
            fabVideo.translationY = translationY
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentAddPostBinding.setListener() {
        MyLogger.v(tagPost, isFunctionCall = true)

        //Just for making edittext scrollable
        etCreatePost.setOnTouchListener { v, event ->
            if (v.id == R.id.etCreatePost) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        fabMain.setOnClickListener {
            toggleFabMenu()
        }

        fabImage.setOnClickListener {
            toggleContentVisibility(linearImage, fabImage)
        }

        fabVideo.setOnClickListener {
            toggleContentVisibility(linearVideo, fabVideo)
        }

        ivCancleImage.setOnClickListener {
            hideContent(linearImage)
        }
        ivCancleVideo.setOnClickListener {
            hideContent(linearVideo)
        }


    }

    private fun toggleFabMenu() {
        binding.apply {
            if (fabMain.tag.toString().toInt() == closeFabMenu) {
                MyLogger.i(tagPost, msg = "Now open fab menu 😎")
                openMenu()
            } else {
                MyLogger.i(tagPost, msg = "Now close fab menu 😭")
                closeMenu()
            }
        }

    }

    private fun validateFabHide(isCancelClick: Boolean = false) {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            if (linearImage.isVisible && linearVideo.isVisible) {
                linearFab.hideMainFab()
            } else {
                linearFab.show()
                if (isCancelClick) {
                    if (linearFab.isVisible) {
                        if (fabImage.isGone) {
                            fabImage.showFab()
                        } else {
                            fabVideo.showFab()
                        }
                    }
                }

            }
        }
    }

    private fun closeMenu() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            fabMain.tag = 0

            linearFab.background = ContextCompat.getDrawable(
                requireContext(),
                android.R.drawable.screen_background_dark_transparent
            )
            fabMain.animate().setInterpolator(interpolator).rotation(0f).setDuration(300).start()

            fabImage.hideFab()
            fabVideo.hideFab()

        }
    }

    private fun openMenu() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            fabMain.tag = 1
            linearFab.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.shape_fab_bg)
            fabMain.animate().setInterpolator(interpolator).rotation(45f).setDuration(300).start()
            if (linearImage.isGone) {
                MyLogger.d(tagPost, msg = "Image part is now showing !")
                fabImage.showFab()
            }
            if (linearVideo.isGone) {
                MyLogger.d(tagPost, msg = "Video part is now showing !")
                fabVideo.showFab()
            }
        }
    }

    private fun LinearLayout.hideMainFab() {
        closeMenu()
        gone()
    }

    private fun toggleContentVisibility(
        contentLayout: LinearLayout,
        fabButton: FloatingActionButton
    ) {
        contentLayout.show()
        fabButton.hideFab()
        validateFabHide(false)
    }

    private fun hideContent(contentLayout: LinearLayout) {
        contentLayout.gone()
        validateFabHide(true)
    }

    private fun FloatingActionButton.hideFab() {
        animate().translationY(translationY).alpha(0f)
            .setInterpolator(interpolator).setDuration(300).start()
        gone()
    }

    private fun FloatingActionButton.showFab() {
        animate().translationY(0f).alpha(1f).setInterpolator(interpolator)
            .setDuration(300).start()
        show()
    }

    override fun onDestroyView() {
        MyLogger.v(tagPost, isFunctionCall = true)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLogger.v(tagPost, isFunctionCall = true)
    }

}