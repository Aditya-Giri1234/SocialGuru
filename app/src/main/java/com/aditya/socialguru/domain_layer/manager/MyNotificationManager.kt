package com.aditya.socialguru.domain_layer.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavDeepLinkBuilder
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.ui_layer.fragment.profile_part.ProfileViewFragmentArgs
import java.util.Random


object MyNotificationManager {

    const val TAG = "MyNotificationManager"
    private val tagNotification=Constants.LogTag.Notification

    //give channel name and id
    private const val USER_ACTION_CHANNEL = "User Action Channel"
    private const val USER_ACTION_CHANNEL_ID = "USER_ACTION_CHANNEL_ID"
    private const val POST_ACTION_CHANNEL = "Post Action Channel"
    private const val POST_ACTION_CHANNEL_ID = "POST_ACTION_CHANNEL_ID"
    private const val CHAT_MESSAGE_CHANNEL = "Chat Message Channel"
    private const val CHAT_MESSAGE_CHANNEL_ID = "CHAT_MESSAGE_CHANNEL_ID"

    //now make each notification id
    private const val _USER_ACTION_NOTIFICATION_ID = 0
    private const val _POST_ACTION_NOTIFICATION_ID = 100
    private const val _CHAT_MESSAGE_NOTIFICATION_ID = 200
    private const val GROUP_SUMMARY_NOTIFICATION_ID = 400

    //Give each pending intent different request code
    private const val USER_PENDING_INTENT_REQUEST_CODE = 0
    private const val POST_PENDING_INTENT_REQUEST_CODE = 1
    private const val CHAT_PENDING_INTENT_REQUEST_CODE = 2

    //Group Id
    private const val NOTIFICATON_GROUP="Notification Group"


    private val USER_ACTION_NOTIFICATION_ID: Int
        get() {
            return _USER_ACTION_NOTIFICATION_ID + (Random().nextInt(Int.MAX_VALUE))
        }
    private val POST_ACTION_NOTIFICATION_ID: Int
        get() {
            return _POST_ACTION_NOTIFICATION_ID + (Random().nextInt(Int.MAX_VALUE))
        }
    private val CHAT_MESSAGE_NOTIFICATION_ID: Int
        get() {
            return _CHAT_MESSAGE_NOTIFICATION_ID + (Random().nextInt(Int.MAX_VALUE))
        }


    fun showNewFollowerNotification(
        user: User,
        notificationData: NotificationData,
        context: Context
    ) {

        // There is no need to make deep link in graph it was implicit deep link  which help like open url base link in app but below is explicit deep link which not require any thing.

        // And one more thing it will recreate give activity and open destination
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.bottom_navigation)
            .setDestination(R.id.profileViewFragment3) // Reference deep link action ID
            .setComponentName(MainActivity::class.java)  // This tell deep link this graph is present That main activity. If you not use this it go to default activity which was launcher activity and find this graph their.
            .setArguments(ProfileViewFragmentArgs(notificationData.friendOrFollowerId!!).toBundle()) // Pass user ID if needed
            .createPendingIntent()


            /*.createTaskStackBuilder().run {
                val intentList=intents
                if (intentList.isEmpty()) {
                    throw IllegalStateException(
                        "No intents added to TaskStackBuilder; cannot getPendingIntent"
                    )
                }else{

                    val intent=intents[0].apply {
                        removeFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
                        )
                    }
                    PendingIntent.getActivity(context,110,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }

            }*/



        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            USER_ACTION_CHANNEL_ID,
            USER_ACTION_CHANNEL, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(notificationChannel)

        val appIcon = context.packageManager.getApplicationIcon(context.packageName)

        val notification = NotificationCompat.Builder(context, USER_ACTION_CHANNEL_ID)
        notification.setSmallIcon(R.drawable.app_icon)
        if (appIcon is AdaptiveIconDrawable) {
            val bitmap = appIcon.toBitmap()
            notification.setLargeIcon(bitmap)
        } else {
            // If it's not an AdaptiveIconDrawable, assume it's a BitmapDrawable
            notification.setLargeIcon((appIcon as BitmapDrawable).bitmap)
        }
        notification.setTicker("Follower")
        notification.setContentTitle("New Follower")
        notification.setContentText("${user.userName ?: "Unknown"} is follow you.")


        // Set other notification properties..

        // Don't set sound and vibrate individually for this notification
        notification.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        notification.setCategory(NotificationCompat.EXTRA_INFO_TEXT)
        notification.setGroup(NOTIFICATON_GROUP)
        notification.setColor(Color.BLUE)
        notification.setAutoCancel(true)

        // below two line important because in android 8 notification not show like head up so that we need below  2 line.
        notification.setContentIntent(pendingIntent)
        notification.setFullScreenIntent(pendingIntent,true)

        MyLogger.d(Constants.LogTag.Notification, msg = "Notification Id :- $USER_ACTION_NOTIFICATION_ID")

        val notificationId= USER_ACTION_NOTIFICATION_ID

        removeHeadUpNotificationBehaviour(notification,notificationManager,notificationId)
        notificationManager.notify(notificationId, notification.build())
    }

    fun showFriendRequestComeNotification(
        user: User,
        notificationData: NotificationData,
        context: Context
    ) {

        // There is no need to make deep link in graph it was implicit deep link  which help like open url base link in app but below is explicit deep link which not require any thing.

        // And one more thing it will recreate give activity and open destination
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.bottom_navigation)
            .setDestination(R.id.profileViewFragment3) // Reference deep link action ID
            .setComponentName(MainActivity::class.java)  // This tell deep link this graph is present That main activity. If you not use this it go to default activity which was launcher activity and find this graph their.
            .setArguments(ProfileViewFragmentArgs(notificationData.friendOrFollowerId!!).toBundle()) // Pass user ID if needed
            .createPendingIntent()


        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            USER_ACTION_CHANNEL_ID,
            USER_ACTION_CHANNEL, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(notificationChannel)

        val appIcon = context.packageManager.getApplicationIcon(context.packageName)

        val notification = NotificationCompat.Builder(context, USER_ACTION_CHANNEL_ID)
        notification.setSmallIcon(R.drawable.app_icon)
        if (appIcon is AdaptiveIconDrawable) {
            val bitmap = appIcon.toBitmap()
            notification.setLargeIcon(bitmap)
        } else {
            // If it's not an AdaptiveIconDrawable, assume it's a BitmapDrawable
            notification.setLargeIcon((appIcon as BitmapDrawable).bitmap)
        }
        notification.setTicker("Friend")
        notification.setContentTitle("Friend Request")
        notification.setContentText("${user.userName ?: "Unknown"} send friend request .")


        // Set other notification properties...

        // Don't set sound and vibrate individually for this notification
        notification.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        notification.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        notification.setGroup(NOTIFICATON_GROUP)
        notification.setColor(Color.GRAY)

        // below two line important because in android 8 notification not show like head up so that we need below  2 line.
        notification.setContentIntent(pendingIntent)
        notification.setFullScreenIntent(pendingIntent,true)

        MyLogger.d(Constants.LogTag.Notification, msg = "Notification Id :- $USER_ACTION_NOTIFICATION_ID")

        val notificationId= USER_ACTION_NOTIFICATION_ID

        removeHeadUpNotificationBehaviour(notification,notificationManager,notificationId)
        notificationManager.notify(notificationId, notification.build())
    }

    fun showAcceptFriendRequestNotification(
        user: User,
        notificationData: NotificationData,
        context: Context
    ) {

        // There is no need to make deep link in graph it was implicit deep link  which help like open url base link in app but below is explicit deep link which not require any thing.

        // And one more thing it will recreate give activity and open destination
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.bottom_navigation)
            .setDestination(R.id.profileViewFragment3) // Reference deep link action ID
            .setComponentName(MainActivity::class.java)  // This tell deep link this graph is present That main activity. If you not use this it go to default activity which was launcher activity and find this graph their.
            .setArguments(ProfileViewFragmentArgs(notificationData.friendOrFollowerId!!).toBundle()) // Pass user ID if needed
            .createPendingIntent()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            USER_ACTION_CHANNEL_ID,
            USER_ACTION_CHANNEL, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(notificationChannel)

        val appIcon = context.packageManager.getApplicationIcon(context.packageName)

        val notification = NotificationCompat.Builder(context, USER_ACTION_CHANNEL_ID)
        notification.setSmallIcon(R.drawable.app_icon)
        if (appIcon is AdaptiveIconDrawable) {
            val bitmap = appIcon.toBitmap()
            notification.setLargeIcon(bitmap)
        } else {
            // If it's not an AdaptiveIconDrawable, assume it's a BitmapDrawable
            notification.setLargeIcon((appIcon as BitmapDrawable).bitmap)
        }
        notification.setTicker("Accept")
        notification.setContentTitle("Accept Friend Request")
        notification.setContentText("${user.userName ?: "Unknown"} is accept your friend request.")


        // Set other notification properties...

        notification.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        notification.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        notification.setGroup(NOTIFICATON_GROUP)
        notification.setColor(Color.GREEN)
        notification.setAutoCancel(true)

        // below two line important because in android 8 notification not show like head up so that we need below  2 line.
        notification.setContentIntent(pendingIntent)
        notification.setFullScreenIntent(pendingIntent,true)

        MyLogger.d(Constants.LogTag.Notification, msg = "Notification Id :- $USER_ACTION_NOTIFICATION_ID")

        val notificationId= USER_ACTION_NOTIFICATION_ID

        removeHeadUpNotificationBehaviour(notification,notificationManager,notificationId)
        notificationManager.notify(notificationId, notification.build())
    }


    fun showGroupSummaryNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val appIcon = context.packageManager.getApplicationIcon(context.packageName)
        val summaryNotification = NotificationCompat.Builder(context, USER_ACTION_CHANNEL_ID)
        summaryNotification.setSmallIcon(R.drawable.app_icon)
        if (appIcon is AdaptiveIconDrawable) {
            val bitmap = appIcon.toBitmap()
            summaryNotification.setLargeIcon(bitmap)
        } else {
            // If it's not an AdaptiveIconDrawable, assume it's a BitmapDrawable
            summaryNotification.setLargeIcon((appIcon as BitmapDrawable).bitmap)
        }
            .setSmallIcon(R.drawable.app_icon)
            .setGroup(NOTIFICATON_GROUP) // Use the group key
            .setGroupSummary(true)
            .setColor(Color.BLACK)
            .setContentTitle("Notification") // Customize the summary title
            .setContentText("You got some notification.") // Customize the summary content
            .setAutoCancel(true)
            .build()

        // Notify the summary notification
        notificationManager.notify(GROUP_SUMMARY_NOTIFICATION_ID, summaryNotification.build())
    }


    /**
     * This function re-notify given notification by notification id after 1 second without head's behaviour.
     *
     * @param notification In which notification head's up behaviour gone
     * @param notificationManager for re - notify same notification without head's behaviour
     * @param notificationId  for notification id help to re-notify
     * */
    private fun removeHeadUpNotificationBehaviour(notification: NotificationCompat.Builder , notificationManager: NotificationManager , notificationId:Int){
        // Schedule transition to notification drawer after 5 seconds (adjust as needed)
        MyLogger.v(tagNotification, isFunctionCall = true)

        val transitionHandler = Handler(Looper.getMainLooper())
        transitionHandler.postDelayed({
            // Update the notification to remove full screen intent
            notification.setFullScreenIntent(null, false)

            // Notify again to update the existing notification in the drawer
            MyLogger.d(tagNotification, msg = "Now head's up notification behaviour gone for notification id:-$notificationId")
            notificationManager.notify(notificationId, notification.build())
        }, 1000)
    }

/*
 fun showIncomingCallNotification(number: String, context: Context) {
        val phoneNumberUtil = PhoneNumberUtil.createInstance(context)
        val formatNumber: String =
            if (number != Constants.UNKNOWN_NUMBER && number.isDigitsOnly()) {
                Helper.getFormattedNumber(number, context)

            } else {
                number
            }
        val contactInfo = ContactManager.getContactByNumber(context, formatNumber)
        val (name, photoUri) = contactInfo?.let {
            Pair(it.displayNamePrimary, it.photoUri)
        } ?: Pair(null, null)


        val acceptIntent =
            if (Helper.isActivityRunning(MainActivity::class.java.simpleName)) {
                Intent(context, IncomingCallActivity::class.java)
            } else {
                Intent(context, SplashScreen::class.java).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    action = Intent.ACTION_MAIN
                }.apply {
                    // this clear task flag needed because we need this when permission revoke and call come
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }


        acceptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP)
        acceptIntent.putExtra(Constants.IntentTable.FormattedNumber.name, formatNumber)
        acceptIntent.putExtra(Constants.IntentTable.UserName.name, name)
        acceptIntent.putExtra(Constants.IntentTable.PhotoUri.name, photoUri)
        acceptIntent.putExtra(Constants.IntentTable.IsCallAccept.name, true)
        acceptIntent.putExtra(
            Constants.IntentTable.CallType.name,
            Constants.CallStatus.IncomingCall.name
        )
        acceptIntent.putExtra(Constants.IntentTable.IsNotification.name, true)
        acceptIntent.putExtra(Constants.IntentTable.IsCallNotification.name, true)


        val declineIntent = Intent(context, MyBroadCastReceiver::class.java)
        declineIntent.action = Constants.CallNotificationType.Decline.name


        val defaultIntent = setIntent(number, context)


        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                PENDING_INTENT_REQUEST_CODE,
                defaultIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        } else {
            PendingIntent.getActivity(
                context,
                PENDING_INTENT_REQUEST_CODE,
                defaultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
        }

        val pendingAccept = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                ACCEPT_PENDING_INTENT_REQUEST_CODE,
                acceptIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        } else {
            PendingIntent.getActivity(
                context,
                ACCEPT_PENDING_INTENT_REQUEST_CODE,
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
        }
        val pendingDecline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                DECLINE_PENDING_INTENT_REQUEST_CODE,
                declineIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                DECLINE_PENDING_INTENT_REQUEST_CODE,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
            )
        }

        val customView = RemoteViews(context.packageName, R.layout.sample_notification)

        val nightModeFlags: Int = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                customView.setInt(
                    R.id.name,
                    "setTextColor",
                    ContextCompat.getColor(context, R.color.white)
                )
                customView.setInt(
                    R.id.tvCallType,
                    "setTextColor",
                    ContextCompat.getColor(context, R.color.white)
                )
            }

            Configuration.UI_MODE_NIGHT_NO -> {

            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {

            }
        }



        customView.setOnClickPendingIntent(R.id.btnAnswer, pendingAccept)
        customView.setOnClickPendingIntent(R.id.btnDecline, pendingDecline)

//        customView.setTextViewText(R.id.name, name ?: formatNumber)
        customView.setTextViewText(R.id.name, "${name ?: formatNumber} is calling")

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        //region:: Delete Notification channel

        //delete existing channel and then recreate with new updated setting
        deleteNotificationChannel(context, IncomingCallChannelId)

        //endregion


        val notificationChannel = NotificationChannel(
            IncomingCallChannelId,
            IncomingCallChannel, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            // Set Sound and vibration


            SoftwareManager.checkVibrationIsOn(context).takeIf { it }?.run {
                MyLogger.i(msg = "$TAG -  showIncomingNotification - Vibration is on !")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 500, 1000)

            } ?: run {
                MyLogger.i(msg = "$TAG -  showIncomingNotification - Vibration is off !")
                enableVibration(false)
            }

            SoftwareManager.checkRingerIsOn(context).takeIf { it }?.run {
                MyLogger.i(msg = "$TAG -  showIncomingNotification - Ringer Mode is on !")
                MyLogger.v(msg = "$TAG -  showIncomingNotification - uri->$ringtoneUri")
                setSound(
                    ringtoneUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } ?: run {
                MyLogger.i(msg = "$TAG -  showIncomingNotification - Ringer Mode is off !")
            }
        }


        notificationManager.createNotificationChannel(notificationChannel)


        // Create notification


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val caller = Person.Builder()

                // Caller icon
                .setIcon(photoUri?.let {
                    Icon.createWithContentUri(photoUri)
                })
                // Caller name
                .setName(name ?: formatNumber)
                .setImportant(true)
                .build()

            val notificationStyle =
                Notification.CallStyle.forIncomingCall(caller, pendingDecline, pendingAccept)
            val notification = Notification.Builder(context, IncomingCallChannelId)
            notification.apply {
                setSmallIcon(R.drawable.azhai_logo_new_big)
                setContentTitle(Constants.Azhai)
                setContentText("IncomingCall")
                setCategory(NotificationCompat.CATEGORY_CALL)
//                setOngoing(true)      but we set FLAG_INSISTENT in below so that we need we also set this with that
                // If user click direct notification it should trigger launcher activity
                setContentIntent(pendingIntent)
                setFullScreenIntent(pendingIntent, true)
                style = notificationStyle
            }
            val note = notification.build()
            note.flags = Notification.FLAG_INSISTENT or Notification.FLAG_ONGOING_EVENT

            notificationManager.notify(IncomingCallNotificationId, note)

        } else {
            val notification = NotificationCompat.Builder(context, IncomingCallChannelId)
            notification.apply {
                // Setting Icon (small and large)
                setSmallIcon(R.drawable.azhai_logo_new_big)

                // Set Content IN Notification
                setContentTitle(Constants.Azhai)
                setContentText("IncomingCall")

                // Set Category of notification
                setCategory(NotificationCompat.CATEGORY_CALL)


                // If user click direct notification it should trigger launcher activity
                setContentIntent(pendingIntent)
                setFullScreenIntent(pendingIntent, true)


                // Setting Style
                setStyle(NotificationCompat.DecoratedCustomViewStyle())

                // Setting How Notification show user
                setCustomContentView(customView)
            }

            val note = notification.build()
            note.flags = Notification.FLAG_INSISTENT or Notification.FLAG_ONGOING_EVENT

            notificationManager.notify(IncomingCallNotificationId, note)
        }


    }

    private fun getIncomingCallNotificationPriority(context: Context): Int {
        val keyguardManager =
            context.getSystemService(FirebaseMessagingService.KEYGUARD_SERVICE) as KeyguardManager
        val isAppInForeground = SoftwareManager.isForeground(
            context.packageName,
            context
        ) && !keyguardManager.isDeviceLocked


        MyLogger.i(msg = "For getting notification priority and app in foreground $isAppInForeground")

        return isAppInForeground.takeIf { it }?.let {
            NotificationCompat.PRIORITY_DEFAULT
        } ?: NotificationCompat.PRIORITY_MAX

    }

    private fun getIncomingCallNotificationChannelPriority(context: Context): Int {
        val keyguardManager =
            context.getSystemService(FirebaseMessagingService.KEYGUARD_SERVICE) as KeyguardManager
        val isAppInForeground = SoftwareManager.isForeground(
            context.packageName,
            context
        ) && !keyguardManager.isDeviceLocked

        MyLogger.i(msg = "For getting notification channel priority and app in foreground $isAppInForeground")
        return isAppInForeground.takeIf { it }?.let {
            NotificationManager.IMPORTANCE_DEFAULT
        } ?: NotificationManager.IMPORTANCE_HIGH

    }


    fun showCallNotification(context: Context, number: String?, type: String, isMute: Boolean) {

        val callEndIntent = Intent(context.applicationContext, MyBroadCastReceiver::class.java)
        callEndIntent.action = Constants.DuringCall.End.name

        val callMuteIntent = Intent(context.applicationContext, MyBroadCastReceiver::class.java)
        callMuteIntent.action = Constants.DuringCall.Mute.name
        callEndIntent.putExtra(Constants.IntentTable.Number.name, number)
        callMuteIntent.putExtra(Constants.IntentTable.CallType.name, type)

        val intent = Intent(context.applicationContext, CallDialingScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context,
                PENDING_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            ) // Must use mutable
        } else {
            PendingIntent.getActivity(
                context,
                PENDING_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val pendingEnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context.applicationContext,
                END_CALL_PENDING_INTENT_REQUEST_CODE,
                callEndIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context.applicationContext,
                END_CALL_PENDING_INTENT_REQUEST_CODE,
                callEndIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val pendingMute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context.applicationContext,
                MUTE_UNMUTE_CALL_PENDING_INTENT_REQUEST_CODE,
                callMuteIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context.applicationContext,
                MUTE_UNMUTE_CALL_PENDING_INTENT_REQUEST_CODE,
                callMuteIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }


        val customView = RemoteViews(context.packageName, R.layout.sample_call_notification)
        if (isMute) {
            customView.setTextViewText(R.id.btnMuteCall, "UnMute")
        } else {
            customView.setTextViewText(R.id.btnMuteCall, "Mute")
        }


        val nightModeFlags: Int = context.applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                customView.setInt(
                    R.id.tvCallNotiTitle,
                    "setTextColor",
                    ContextCompat.getColor(context.applicationContext, R.color.white)
                )
                customView.setInt(
                    R.id.chronometer,
                    "setTextColor",
                    ContextCompat.getColor(context.applicationContext, R.color.white)
                )
                customView.setInt(
                    R.id.tvCallNotiStatus,
                    "setTextColor",
                    ContextCompat.getColor(context.applicationContext, R.color.white)
                )
                customView.setInt(
                    R.id.btnEndcall,
                    "setTextColor",
                    ContextCompat.getColor(context.applicationContext, R.color.startRed)
                )
                customView.setInt(
                    R.id.btnMuteCall,
                    "setTextColor",
                    ContextCompat.getColor(context.applicationContext, R.color.startRed)
                )
            }

            Configuration.UI_MODE_NIGHT_NO -> {

            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {

            }
        }

        customView.setOnClickPendingIntent(R.id.btnEndcall, pendingEnd)
        customView.setOnClickPendingIntent(R.id.btnMuteCall, pendingMute)

        customView.setChronometer(
            R.id.chronometer,
            SystemClock.elapsedRealtime() - (MyTelephonyManager.callNotificationSeconds * 1000).toLong(),
            "%hh:%mm:%ss",
            true
        )

        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            CallNotificationChannelId,
            CallNotificationChannel, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(notificationChannel)

        val notification =
            NotificationCompat.Builder(context.applicationContext, CallNotificationChannelId)
        notification.setContentTitle("Azhai")
        notification.setTicker("Call_STATUS")
        if (MyTelephonyManager.isCallConnected) {
            notification.setContentText("$type is continue ...")
//            notification.setCustomContentView(customView)
            notification.setCustomBigContentView(customView)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notification.setStyle(NotificationCompat.BigTextStyle())
            } else {
                notification.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            }

        } else {
            notification.setContentText("Call is Connecting ....")
        }
        notification.setSmallIcon(R.drawable.azhai_logo_new)
        notification.setCategory(NotificationCompat.CATEGORY_CALL)
        notification.setOngoing(true) // by this user can't dismiss or cancel notification
        notification.setContentIntent(pendingIntent)
        notification.setFullScreenIntent(pendingIntent, true)
        notification.setAutoCancel(true)

        notificationManager.notify(ShowCallNotificationId, notification.build())
    }


    private fun setIntent(number: String, context: Context): Intent {
        MyLogger.i(msg = "Set intent now begin")
        val phoneNumberUtil = PhoneNumberUtil.createInstance(context)

        val intent =
            if (Helper.isActivityRunning(MainActivity::class.java.simpleName)) {
                Intent(context, IncomingCallActivity::class.java)
            } else {
                Intent(context, SplashScreen::class.java).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    action = Intent.ACTION_MAIN
                }.apply {
                    // this clear task flag needed because we need this when permission revoke and call come
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(
            Constants.IntentTable.Number.name, number
        )


        if (number != Constants.UNKNOWN_NUMBER) {
            MyLogger.i(msg = "$TAG - Number is Known -> $number")

            intent.putExtra(
                Constants.IntentTable.FormattedNumber.name,
                if (number.isDigitsOnly()) Helper.getFormattedNumber(number, context) else number
            )


        } else {
            MyLogger.i(msg = "Number is Unknown")
            intent.putExtra(
                Constants.IntentTable.FormattedNumber.name, number
            )
        }
        val contactInfo = ContactManager.getContactByNumber(context, number)
        val (name, photoUri) = contactInfo?.let {
            Pair(it.displayNamePrimary, it.photoUri)
        } ?: Pair(null, null)
        intent.putExtra(Constants.IntentTable.UserName.name, name)
        intent.putExtra(Constants.IntentTable.PhotoUri.name, photoUri)
        intent.putExtra(Constants.IntentTable.CallType.name, Constants.CallStatus.IncomingCall.name)
        intent.putExtra(Constants.IntentTable.IsNotification.name, true)
        intent.putExtra(Constants.IntentTable.IsCallNotification.name, true)

        MyLogger.i(msg = "Intent is now set")
        storeDataInSharedPreference(intent, context)

        return intent
    }

    private fun storeDataInSharedPreference(intent: Intent, context: Context) {

        MyLogger.i(msg = "Notification data is store successful in preference !")

        val pref = SharedPref(context)

        val data = IntentTable().apply {
            number = intent.getStringExtra(Constants.IntentTable.Number.name)
            formattedNumber = intent.getStringExtra(Constants.IntentTable.FormattedNumber.name)
            userName = intent.getStringExtra(Constants.IntentTable.UserName.name)
            photoUri = intent.getStringExtra(Constants.IntentTable.PhotoUri.name)
            callType = intent.getStringExtra(Constants.IntentTable.CallType.name)
            isNotification = true
            isCallNotification = true
        }

        val gson = Gson()
        val dataString = gson.toJson(data)

        pref.setPrefString(IntentTable::class.java.simpleName, dataString)

    }*/

    private fun deleteNotificationChannel(context: Context, channelId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        MyLogger.w(msg = "Channel with Channel Id -> $channelId  deleted  successfully!")
        notificationManager.deleteNotificationChannel(channelId)
    }


    fun clearAllNotification(context: Context) {
        MyLogger.d(isFunctionCall = true)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = notificationManager.activeNotifications
        notificationManager.cancelAll()
    }



    fun isNotificationVisible(context: Context, id: Int): Boolean {
        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return mNotificationManager.activeNotifications.any { it.id == id }
    }
}