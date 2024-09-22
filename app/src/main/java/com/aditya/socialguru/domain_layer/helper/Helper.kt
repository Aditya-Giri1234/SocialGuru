package com.aditya.socialguru.domain_layer.helper

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.ProfileViewDialog
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSnackBar
import com.aditya.socialguru.domain_layer.custom_class.snackbar.CustomSuccessSnackBar
import com.aditya.socialguru.domain_layer.manager.MyLogger
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object Helper {

    private var toast: Toast? = null
    private var loader: MyLoader? = null

    fun giveAnimationNavOption(): NavOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    fun giveAnimationNavOptionWithSingleTop()= NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .setLaunchSingleTop(true)
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
    fun getCommentId(post_id:String):String="${Constants.Table.Post.name}_${post_id}_${generateUUID()}_${System.currentTimeMillis()}"
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

    fun getGroupChatId() : String = "${Constants.Table.GroupChat.name}_${AuthManager.currentUserId()!!}_${System.currentTimeMillis()}"
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
            duration.toMinutes() < 60 -> "${duration.toMinutes()} minute ago"
            duration.toHours() < 24 -> "${duration.toHours()} hour ago"
            duration.toDays() == 1L -> "yesterday"
            else -> "${duration.toDays()} day ago"
        }
    }

    fun getTimeForPostAndComment(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val timeDiff = now - timestamp

        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff)
        val hours = TimeUnit.MILLISECONDS.toHours(timeDiff)
        val days = TimeUnit.MILLISECONDS.toDays(timeDiff)

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 30 -> "${days}d"
            days < 365 -> "${days / 30}mo"
            else -> "${days / 365}y"
        }
    }

    fun getTimeForChat(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val currentCalendar = Calendar.getInstance()

        // Set the calendar to the timestamp provided
        calendar.timeInMillis = timestamp

        // Case 1: Check if the timestamp is today
        if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        // Case 2: Check if the timestamp is yesterday
        currentCalendar.add(Calendar.DAY_OF_YEAR, -1)
        if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday"
        }

        // Case 3: Any other day (before yesterday)
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getTimeForChatMessage(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getChatDateHeaderTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    fun is24HourOldStory(storyTimestamp: Long): Boolean {
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Calculate the difference in time between the current time and the story timestamp
        val timeDifference = currentTime - storyTimestamp

        // 24 hours in milliseconds
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000

        // Check if the time difference is greater than or equal to 24 hours
        return timeDifference >= twentyFourHoursInMillis
    }

    fun isOneMinuteOldStory(storyTimestamp: Long): Boolean {
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Calculate the difference in time between the current time and the story timestamp
        val timeDifference = currentTime - storyTimestamp

        // 1 minute in milliseconds
        val oneMinuteInMillis = 60 * 1000

        // Check if the time difference is greater than or equal to 1 minute
        return timeDifference >= oneMinuteInMillis
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

    fun Fragment.observeFlow(run:suspend CoroutineScope.()->Unit){
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                run()
            }
        }
    }

    // region:: Get Random color

     fun setUserProfileColor(user: Map<String, User>) : MutableMap<String, Int>{
        val colorMap = mutableMapOf<String, Int>()
        for ((key, userDetails) in user) {
            val color = generateColorFromUserId(key)
            colorMap[key] = color
        }
        return colorMap
    }

     fun setUserProfileColor(user: User) : Int{
        return generateColorFromUserId(user.userId!!)
    }
    fun setUserProfileColor(userId: String) : Int{
        return generateColorFromUserId(userId)
    }


    private fun generateColorFromUserId(userId: String): Int {
        val hash = userId.hashCode()

        // Generate RGB values
        val red = (hash and 0xFF0000 shr 16) % 256
        val green = (hash and 0x00FF00 shr 8) % 256
        val blue = (hash and 0x0000FF) % 256

        // Adjust the color to ensure it's neither too dark nor too white
        val minBrightness = 100 // Minimum brightness to avoid dark colors
        val maxBrightness = 200 // Maximum brightness to avoid white colors

        val adjustedRed = red.coerceIn(minBrightness, maxBrightness)
        val adjustedGreen = green.coerceIn(minBrightness, maxBrightness)
        val adjustedBlue = blue.coerceIn(minBrightness, maxBrightness)

        return Color.rgb(adjustedRed, adjustedGreen, adjustedBlue)
    }



    //endregion

    fun deleteCacheFile(context: Context, fileName: String?){
        if (fileName == null) return
        val isDelete = context.deleteFile(fileName)
        MyLogger.w(msg = "Temp File $fileName is ${if(isDelete) "Deleted" else "Not Deleted"}")
    }

}