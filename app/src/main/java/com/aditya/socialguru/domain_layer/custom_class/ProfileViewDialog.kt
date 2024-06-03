package com.aditya.socialguru.domain_layer.custom_class

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.SampleDialogViewBinding

class ProfileViewDialog(val bitmap: Bitmap) : DialogFragment() {

    private var _binding: SampleDialogViewBinding? = null
    private val binding: SampleDialogViewBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SampleDialogViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        binding.apply {
            ivProfileDialogView.setImageBitmap(bitmap)
            dialog?.let {
                it.setCanceledOnTouchOutside(true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val lp = WindowManager.LayoutParams()
                    lp.blurBehindRadius = 100
                    lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    it.window?.attributes = lp

                } else {
                    it.window?.let { window ->
                        window.setDimAmount(0.8f)
                        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    }
                }
            }
        }
    }
}