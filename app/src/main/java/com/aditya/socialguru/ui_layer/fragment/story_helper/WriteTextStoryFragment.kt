package com.aditya.socialguru.ui_layer.fragment.story_helper

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.databinding.FragmentWriteTextStoryBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.vanniktech.emoji.EmojiPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.system.measureTimeMillis


class WriteTextStoryFragment : Fragment() {

    private var _binding: FragmentWriteTextStoryBinding? = null
    private val binding get() = _binding!!
    private val tagStory = Constants.LogTag.Story
    private val emojiKeyboardTag = 0
    private val emojiPopup by lazy {
        EmojiPopup(binding.root, binding.etStatus)
    }

    private val fontList = arrayOf(
        R.font.alike_angular_regular,
        R.font.bebas_neue_regular,
        R.font.dancing_script_medium,
        R.font.inter_medium,
        R.font.open_sans_medium,
        R.font.pacifico_regular,
        R.font.poppin_medium,
        R.font.roboto_condensed_medium,
        R.font.roboto_medium,
        R.font.ubuntu_sans_mono_medium
    )
    private var currentFont = R.font.roboto_medium
    private var currentColor = Color.BLACK

    private val navController get() = (requireActivity() as MainActivity).navController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWriteTextStoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {

        initUi()
    }

    private fun initUi() {
        binding.apply {


            setListener()
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentWriteTextStoryBinding.setListener() {
        icEmoji.setOnClickListener {
            if (it.tag.toString().toInt() == emojiKeyboardTag) {
                emojiPopup.toggle()
                icEmoji.tag = "1"  // Important tag set into string else get unexpected result
                icEmoji.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_keybaord
                    )
                )
            } else {
                icEmoji.tag = "0"
                emojiPopup.dismiss()
                icEmoji.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.emoji
                    )
                )
            }
        }
        etStatus.setOnTouchListener { v, event ->
            MyLogger.v(tagStory, msg = "Now user touch on edit status view !")
            v.onTouchEvent(event)
            emojiPopup.dismiss()
            true
        }

        icColorPalete.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                val time = measureTimeMillis {
                    changeBackgroundColor()
                }

                MyLogger.w(tagStory, msg = "Time taken to change color is $time")

            }
        }

        icTextFont.setOnClickListener {
            currentFont = fontList.random()
            etStatus.typeface = ResourcesCompat.getFont(requireContext(), currentFont)
        }

        icClose.setOnClickListener {
            navController?.navigateUp()
        }

        lifecycleScope.myLaunch {
            etStatus.getQueryTextChangeStateFlow().debounce(100).distinctUntilChanged()
                .flatMapLatest {
                    if (it.startsWith(" ")) {
                        runOnUiThread {
                            etStatus.setText(it.trim())
                        }
                    }
                    flow {
                        emit(it.trim())
                    }
                }.flowOn(Dispatchers.Default).collect {
                    runOnUiThread {
                        // Do nothing
                    }
                }
        }


        btnDone.setSafeOnClickListener {
            if (isValidate()) {
                requireContext().sendBroadcast(Intent(Constants.BroadcastType.StoryUploading.name).apply {
                    putExtra(
                        Constants.DATA,
                        StoryText(etStatus.text.toString(), currentFont ,currentColor)
                    )
                })
                navController.navigateUp()
            }

        }

        binding.root.setOnClickListener {
            etStatus.hideKeyboard()
        }


    }

    private fun isValidate(): Boolean {
        if (binding.etStatus.text.isNullOrEmpty()) {
            Helper.showSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                    R.id.coordLayout
                ), "Status is empty !"
            )
            return false

        }
        return true

    }

    private suspend fun changeBackgroundColor() {
        MyLogger.v(tagStory, isFunctionCall = true)
        currentColor = getRandomColor()
        binding.root.setBackgroundColor(currentColor)
    }

    // Function to generate a random color
    private fun getRandomColor(): Int {
        MyLogger.v(tagStory, isFunctionCall = true)
        val rnd = Random
        val maxAttempts = 10
        var color: Int
        var attempts = 0

        do {
            val red = rnd.nextInt(256)
            val green = rnd.nextInt(256)
            val blue = rnd.nextInt(256)
            color = Color.rgb(red, green, blue)
            attempts++
        } while (isColorTooLightOrDark(color) && attempts < maxAttempts)

        // If a suitable color is not found within the max attempts, use a default color
        if (isColorTooLightOrDark(color)) {
            color = Color.GRAY
        }

        return color
    }

    // Function to check if a color is too light or too dark
    private fun isColorTooLightOrDark(color: Int): Boolean {
        MyLogger.v(tagStory, isFunctionCall = true)
        val brightness =
            Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114
        val minThreshold = 50.0  // Adjusted to make it less restrictive
        val maxThreshold = 205.0 // Adjusted to make it less restrictive
        return brightness < minThreshold || brightness > maxThreshold
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}

