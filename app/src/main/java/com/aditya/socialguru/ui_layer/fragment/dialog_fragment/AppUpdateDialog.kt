package com.aditya.socialguru.ui_layer.fragment.dialog_fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.git.GitHubRelease
import com.aditya.socialguru.databinding.DeleteAlertDialogBinding
import com.aditya.socialguru.databinding.DialogAppUpdateInfoBinding
import com.aditya.socialguru.domain_layer.helper.giveMeColor
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.isResume
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setMarkdownText
import com.aditya.socialguru.domain_layer.helper.setWidthPercent
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption

class AppUpdateDialog (val versionInfo:GitHubRelease ,val negativeMessage:String?=null, val positiveMessage:String?=null , val negativeAction:(()->Unit)?=null, val positiveAction:(()->Unit)?=null) : DialogFragment() {

    private var _binding: DialogAppUpdateInfoBinding? = null
    private val binding get() = _binding!!

    private val manageUnknownAppSourcesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle the result if needed
        if (result.resultCode == Activity.RESULT_OK) {
            // The user has granted permission for unknown apps
            // Handle the case accordingly
            proceedToAppUpdate()
        }else{
            showInitialDialog()
        }
    }

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
            requireContext().apply {
                if(requireContext().packageManager.canRequestPackageInstalls()){
                    proceedToAppUpdate()
                }else{
                    updateDialog("App Updating",null,false,true)
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageUnknownAppSourcesLauncher.launch(intent)
                }
            }
        }
    }

    private fun proceedToAppUpdate(){
        updateDialog("App Updating",null,false,true)
        positiveAction?.invoke()
    }
    private fun showInitialDialog(){
        binding.apply {
            linearDownloadView.gone()
            linearMainUpdateInfo.myShow()
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
         if(this.isResume()){
             binding.apply {
                 linearDownloadView.myShow()
                 linearMainUpdateInfo.gone()
                 tvMessage.isGone = message ==null
                 callStatusLoader.isGone = isThreeDotHide

                 if(inderminate){
                     constProgress.myShow()
                     tvProgress.gone()
                     progressCircular.myShow()
                 }else{
                     tvProgress.myShow()
                     constProgress.isGone = progress ==null
                 }
                 progressCircular.isIndeterminate = inderminate
                 tvMessage.text = message
                 tvProgress.text = "$progress%"
                 progressCircular.progress = progress ?: 0
             }
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