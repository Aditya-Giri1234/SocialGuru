package com.aditya.socialguru.domain_layer.helper

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import com.aditya.socialguru.R
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.ProfileViewDialog
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSnackBar
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSuccessSnackBar
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
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
    fun giveUpAndBottomAnimationNavOption(): NavOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_top)
        .setExitAnim(R.anim.slide_out_top)
        .setPopEnterAnim(R.anim.slide_in_bottom)
        .setPopExitAnim(R.anim.slide_out_bottom)
        .build()

    fun giveUpAndBottomAnimationNavOption(popUpTo: Int, isInclusive: Boolean): NavOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_top)
        .setExitAnim(R.anim.slide_out_top)
        .setPopEnterAnim(R.anim.slide_in_bottom)
        .setPopExitAnim(R.anim.slide_out_bottom)
        .setPopUpTo(popUpTo, isInclusive)
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


    fun getPostId():String="${Constants.Table.Post.name}_${generateUUID()}"
    fun getStoryId():String="${Constants.Table.Stories.name}_${generateUUID()}"
    fun getCommentId(post_id:String):String="${Constants.Table.Post.name}_${post_id}"
   private fun getMediaId(timestamp: Long):String="${Constants.Table.Media.name}_$timestamp"


    /**
     * Help to delete image  from server and database without changes data class.
    * */
    fun getImageMediaId(timestamp: Long) :String = "${getMediaId(timestamp)}_I"

    /**
     * Help to delete video from server and database without changes data class.
     * */
    fun getVideoMediaId(timestamp: Long) : String = "${getMediaId(timestamp)}_V"

    fun getChatRoomId(receiverId:String) :String {
        // Sort the user IDs lexicographically
        val sortedUserIds = listOf(AuthManager.currentUserId()!!, receiverId).sorted()
        // Concatenate them with a delimiter to form the chat room ID
        return "Chat_${sortedUserIds[0]}_${sortedUserIds[1]}"
    }

    fun getMessageId():String = "${Constants.Table.Messages.name}_${AuthManager.currentUserId()!!}_${System.currentTimeMillis()}"
    fun getNotificationId():String="${Constants.Table.Notification.name}_${generateUUID()}"
    fun getUserId():String=UUID.randomUUID().toString()
    fun getPostCommentId(postId:String):String="${Constants.Table.Post.name}_${Constants.Table.Comment.name}_$postId"


    /**
     * @param timestamp is taking time into millisecond.
     *
     * @return give corresponding date and time in text.
    * */
    fun formatTimestampToDateAndTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }


    fun getTimeAgo(timestampMillis: Long): String {
        // Convert the timestamp in milliseconds to LocalDateTime
        val pastTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault())
        val now = LocalDateTime.now()

        val duration = Duration.between(pastTime, now)

        return when {
            duration.toMinutes() < 1 -> "just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()} minute(s) ago"
            duration.toHours() < 24 -> "${duration.toHours()} hour(s) ago"
            duration.toDays() == 1L -> "yesterday"
            else -> "${duration.toDays()} day(s) ago"
        }
    }

    fun getTimeForChat(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun getChatDateHeaderTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun showImageDialog(activity: Activity, bitmap: Bitmap) {
        val dialog = ProfileViewDialog(bitmap)
        dialog.show((activity as AppCompatActivity).supportFragmentManager, "MyDialog")
    }

    suspend fun setLogout(context: Context){
        val pref=SharePref(context)
        pref.deleteAllFromDataStore()
    }

    fun showFcmNotSendDialog(activity: Activity,message: String){
        AlertDialog(message,null,false).show((activity as AppCompatActivity).supportFragmentManager ,"My_Fcm_Token_Not_Send")
    }

    fun LifecycleOwner.observeFlow(run:suspend CoroutineScope.()->Unit){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                run()
            }
        }
    }

}