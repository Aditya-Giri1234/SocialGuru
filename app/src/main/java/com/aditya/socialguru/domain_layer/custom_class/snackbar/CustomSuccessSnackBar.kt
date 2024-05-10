package com.aditya.socialguru.domain_layer.custom_class.snackbar

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aditya.socialguru.R
import com.google.android.material.snackbar.BaseTransientBottomBar


class CustomSuccessSnackBar (parent: ViewGroup, content: CustomSuccessSnackBarView, message: String) :
    BaseTransientBottomBar<CustomSuccessSnackBar>(parent, content, content) {

    init {
        getView().setBackgroundColor(
            ContextCompat.getColor(
                view.context,
                android.R.color.transparent
            )
        )
        getView().setPadding(0, 0, 0, 0)
        getView().rotation = 180f
        val messageText = getView().findViewById<TextView>(R.id.tvMessage)
        messageText.text = message
        getView().setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    dismiss()
                }
            }
            true
        }
    }

    companion object {
        fun make(viewGroup: ViewGroup, message: String): CustomSuccessSnackBar {
            val customView = LayoutInflater.from(viewGroup.context).inflate(
                R.layout.custom_snackbar_success, viewGroup, false
            ) as CustomSuccessSnackBarView
            return CustomSuccessSnackBar(viewGroup, customView, message)
        }
    }
}