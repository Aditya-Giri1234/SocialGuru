package com.aditya.socialguru.domain_layer.helper

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.NavOptions
import com.aditya.socialguru.R
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSnackBar
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSnackBarView
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSuccessSnackBar
import com.google.android.material.snackbar.BaseTransientBottomBar
import java.util.UUID

object Helper {

    private var toast: Toast? = null
    private var loader: MyLoader? = null

    fun giveAnimationNavOption(): NavOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    fun giveAnimationNavOption(popUpTo: Int, isInclusive: Boolean): NavOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .setPopUpTo(popUpTo, isInclusive)
            .build()

    fun givePopUpNavOption(popUpTo: Int, isInclusive: Boolean): NavOptions = NavOptions.Builder()
        .setPopUpTo(popUpTo, isInclusive)
        .build()


    fun showSnackBar(view: View, input: String) {
        val cl = view.findViewById<CoordinatorLayout>(R.id.coordLayout)
        cl?.bringToFront()

        CustomSnackBar.make(cl as ViewGroup, input).setAnimationMode(
            BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        ).setDuration(input.length * 80).show()
    }

    fun showSuccessSnackBar(view: View, input: String) {
        val cl = view.findViewById<CoordinatorLayout>(R.id.coordLayout)
        cl?.bringToFront()
        CustomSuccessSnackBar.make(cl as ViewGroup, input).setAnimationMode(
            BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        ).setDuration(input.length * 80).show()
    }


    fun customToast(context: Context, msg: CharSequence, duration: Int, isNeeded: Boolean = false) {
//        if (isNeeded) {
        toast?.cancel();
        toast = Toast.makeText(context, msg, duration)
        toast?.show();
//        }
    }

    fun showLoader(activity: Activity) {
        loader?.dismiss()
        loader = MyLoader()
        loader?.show((activity as AppCompatActivity).supportFragmentManager, "my_loader_dialog")
    }

    fun hideLoader() {
        loader?.dismiss()
        loader = null
    }


    fun isPasswordValid(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
        return password.matches(Regex(passwordRegex))
    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }


    fun getVideoSize(activity: Activity, uri: Uri): Float {
        return activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                val sizeInBytes = cursor.getLong(sizeIndex)
                // Convert bytes to MB
                sizeInBytes / (1024f * 1024f)
            } else {
                -1f  // Indicate error or no size found
            }
        } ?: -1f  // Indicate error or no cursor returned
    }


    fun getPostSubCollectionPath(id:String):String="${Constants.Table.Post.name}_$id"
    fun getStorySubCollectionPath(id:String):String="${Constants.Table.Stories.name}_$id"
}