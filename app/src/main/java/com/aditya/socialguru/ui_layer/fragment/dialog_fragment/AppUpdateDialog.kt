package com.aditya.socialguru.ui_layer.fragment.dialog_fragment

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isGone
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.git.GitHubRelease
import com.aditya.socialguru.databinding.DeleteAlertDialogBinding
import com.aditya.socialguru.databinding.DialogAppUpdateInfoBinding
import com.aditya.socialguru.domain_layer.helper.giveMeColor
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setMarkdownText
import com.aditya.socialguru.domain_layer.helper.setWidthPercent
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption

class AppUpdateDialog (val versionInfo:GitHubRelease ,val negativeMessage:String?=null, val positiveMessage:String?=null , val negativeAction:(()->Unit)?=null, val positiveAction:(()->Unit)?=null) : DialogFragment() {

    private var _binding: DialogAppUpdateInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAppUpdateInfoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // Set the dialog to occupy the full width of the parent
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT // You can change this to MATCH_PARENT if you want full height too
        )

        return dialog
    }
    private fun handleInitialization() {
        initUi()
    }

    private fun initUi() {
        binding.apply {
            dialog?.apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawableResource(android.R.drawable.screen_background_dark_transparent)
            }

            negativeMessage?.let {
                btnCancel.text = it
                btnCancel.setPadding(15)
                btnCancel.setTextColor(requireContext().giveMeColor(R.color.red))
            }
            positiveMessage?.let {
                btnYes.text = it
                btnYes.setPadding(15)
                btnYes.setTextColor(requireContext().giveMeColor(R.color.green))
            }
            fillVersionInfo()
            setListener()
        }
    }

    private fun DialogAppUpdateInfoBinding.setListener() {
        btnCancel.setOnClickListener {
            negativeAction?.invoke()
            dismissNow()
        }
        btnYes.setOnClickListener {
            positiveAction?.invoke()
            dismissNow()
        }
    }
    private fun fillVersionInfo() {
        binding.versionName.text = versionInfo.name
        if (versionInfo.body.isNotEmpty()) {
            binding.versionInfo.setMarkdownText(versionInfo.body)
        } else {
            binding.versionInfo.gone()
        }
    }

     fun updateDialog(message:String?, progress:Int? , isThreeDotHide:Boolean , inderminate:Boolean=false){
        binding.apply {
            linearMainUpdateInfo.gone()
            linearDownloadView.myShow()
            tvMessage.isGone = message ==null
            callStatusLoader.isGone = isThreeDotHide

            if(inderminate){
                constProgress.myShow()
                tvProgress.gone()
                progressCircular.myShow()
            }else{
                constProgress.isGone = progress ==null
            }
            progressCircular.isIndeterminate = inderminate
            tvMessage.text = message
            tvProgress.text = "$progress%"
            progressCircular.progress = progress ?: 0
        }
    }

    override fun onResume() {
        super.onResume()
        this.setWidthPercent(80)
    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}