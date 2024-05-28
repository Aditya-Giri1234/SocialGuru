package com.aditya.socialguru.domain_layer.custom_class

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.aditya.socialguru.R
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import kotlin.math.ceil

class StatusProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    private val progressBars = mutableListOf<ProgressBar>()
    private val progressBarsFillTime = mutableListOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private var isRunning = false
    private lateinit var onCurrentStoryComplete: () -> Unit
    private val tagStory = Constants.LogTag.Story

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun setProgressBarCount(count: Int, onComplete: () -> Unit) {
        removeAllViews()
        progressBars.clear()
        currentIndex = 0
        onCurrentStoryComplete = onComplete

        MyLogger.d(tagStory, msg = "Progress Bar count :- $count")

        // Loop to add progress bars
        for (i in 0 until count) {
            val progressBar = createProgressBar()
            progressBars.add(progressBar)
            progressBarsFillTime.add(-1)
            addView(progressBar)
        }
    }

    private fun createProgressBar(): ProgressBar {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        val heightInPx = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)

        val layoutParams = LinearLayout.LayoutParams(
            0,
            heightInPx, // Set fixed height using sdp
            1f
        )
        layoutParams.setMargins(2, 0, 2, 0)
        progressBar.layoutParams = layoutParams
        progressBar.progressDrawable =
            ContextCompat.getDrawable(context, R.drawable.progress_bar_white)
        progressBar.progress = 0
        return progressBar
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (currentIndex < progressBars.size) {
                progressBars[currentIndex].progress += 1
                if (progressBars[currentIndex].progress >= 100) {
                    MyLogger.v(
                        tagStory,
                        msg = "Current story index := $currentIndex  finished and next story ${currentIndex + 1} is now start !"
                    )
                    currentIndex++
                    isRunning = false
                    onCurrentStoryComplete.invoke()
                } else {
                    MyLogger.d(
                        tagStory,
                        msg = "Current story index := $currentIndex   and fill time is ${progressBarsFillTime[currentIndex]} , and progress is ${progressBars[currentIndex].progress} , \n interval time :- ${
                            (ceil(progressBarsFillTime[currentIndex] / 100.0).toLong())
                        }"
                    )
                    handler.postDelayed(
                        this,
                        ceil(progressBarsFillTime[currentIndex] / 100.0).toLong()
                    )
                }
            }
        }
    }

    fun startProgress() {
        MyLogger.v(tagStory, isFunctionCall = true)
        if (currentIndex < progressBars.size && !isRunning) {
            isRunning = true
            handler.post(progressRunnable)
        }
    }

    fun setProgressTime(progressBarIndex: Int, time: Int) {
        MyLogger.v(tagStory, isFunctionCall = true)
        MyLogger.d(
            tagStory,
            msg = "ProgressBarIndex :- $progressBarIndex and progressBarsFillTime for this index is :- ${progressBarsFillTime[progressBarIndex].toInt()}"
        )
        if (progressBarIndex >= progressBars.size || progressBarsFillTime[progressBarIndex].toInt() != -1) return
        progressBarsFillTime[progressBarIndex] = time
    }

    private fun stopProgress() {
        MyLogger.v(tagStory, isFunctionCall = true)
        isRunning = false
        handler.removeCallbacks(progressRunnable)
    }

    fun resetProgress() {
        MyLogger.v(tagStory, isFunctionCall = true)
        stopProgress()
        progressBars.forEach { it.progress = 0 }
        currentIndex = 0
    }

    fun next() {
        MyLogger.v(tagStory, isFunctionCall = true)
        if (currentIndex < progressBars.size - 1) {
            removeCallback()
            progressBars[currentIndex].progress = 100
            currentIndex++
            isRunning = false
        }
    }

    fun previous() {
        MyLogger.v(tagStory, isFunctionCall = true)
        if (currentIndex > 0) {
            removeCallback()
            progressBars[currentIndex].progress = 0
            currentIndex--
            progressBars[currentIndex].progress = 0
            isRunning = false
        }
    }

    fun removeCallback() {
        isRunning=false
        handler.removeCallbacks(progressRunnable)
    }

    fun pause() {
        removeCallback()
    }
    fun resume(){
        startProgress()
    }

}
