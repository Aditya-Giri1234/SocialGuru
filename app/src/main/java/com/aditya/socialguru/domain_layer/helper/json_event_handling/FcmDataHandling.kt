package com.aditya.socialguru.domain_layer.helper.json_event_handling

import android.content.Context
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.MyNotificationManager
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object FcmDataHandling : HandleJsonData {

    override fun handle(context: Context, _notificationData: String) {
        MyLogger.v(Constants.LogTag.Notification , msg = _notificationData)

        val notificationData=Gson().fromJson(_notificationData,NotificationData::class.java) ?: return
        notificationData.type ?: return

        when(notificationData.type){
            Constants.NotificationType.NEW_FOLLOWER.name -> {
                handleNewFollowerNotification(notificationData,context)
            }
            Constants.NotificationType.ACCEPT_FRIEND_REQUEST.name -> {
                handleAcceptFriendRequestNotification(notificationData,context)
            }
            Constants.NotificationType.FRIEND_REQUEST_COME.name -> {
                handleFriendRequestComeNotification(notificationData,context)
            }
            Constants.NotificationType.LIKE_IN_POST.name -> {
                handleLikeInPostNotification(notificationData,context)
            }
            Constants.NotificationType.COMMENT_IN_POST.name -> {
                handleCommentInPostNotification(notificationData,context)
            }
            Constants.NotificationType.TextChat.name -> {
                handleTextChatMessage(notificationData,context)
            }
            Constants.NotificationType.MediaChat.name -> {
                handleMediaChatMessage(notificationData,context)
            }
        }
    }

    private fun handleMediaChatMessage(notificationData: NotificationData, context: Context) {
        val userId=notificationData.friendOrFollowerId!!
        val messageId=notificationData.messageId!!
        val chatRoomId=notificationData.chatRoomId!!
        launchCoroutineInIOThread {
            val messageData=async {  FirebaseManager.getMessageById(chatRoomId,messageId).first()}
            val userData=async {  FirebaseManager.getUser(userId).first().data }

        }


    }

    private fun handleTextChatMessage(notificationData: NotificationData, context: Context) {
        val userId=notificationData.friendOrFollowerId!!
        val messageId=notificationData.messageId!!
        val chatRoomId=notificationData.chatRoomId!!
        launchCoroutineInIOThread {
            val messageData=async {  FirebaseManager.getMessageById(chatRoomId,messageId).first()}
            val userData=async {  FirebaseManager.getUser(userId).first().data }
            val user=userData.await()
            val message=messageData.await()
            if (user!=null){
                FirebaseManager.updateSeenStatus(Constants.SeenStatus.Received.status,messageId,chatRoomId)
                MyNotificationManager.showTextChatMessage(user,notificationData ,message,context)
                MyNotificationManager.showGroupSummaryNotification(context)
            }

        }
    }

    private fun handleCommentInPostNotification(
        notificationData: NotificationData,
        context: Context
    ) {

    }

    private fun handleLikeInPostNotification(notificationData: NotificationData, context: Context) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseManager.getUser(it).onEach {
                    it.data?.let {
                        MyNotificationManager.showLikeInPostNotification(it,notificationData,context)
                        MyNotificationManager.showGroupSummaryNotification(context)
                    }
                }.launchIn(this)
            }
        }
    }

    private fun handleFriendRequestComeNotification(
        notificationData: NotificationData,
        context: Context
    ) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseManager.getUser(it).onEach {
                    it.data?.let {
                        MyNotificationManager.showFriendRequestComeNotification(it,notificationData,context)
                        MyNotificationManager.showGroupSummaryNotification(context)
                    }
                }.launchIn(this)
            }
        }
    }

    private fun handleAcceptFriendRequestNotification(
        notificationData: NotificationData,
        context: Context
    ) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseManager.getUser(it).onEach {
                    it.data?.let {
                        MyNotificationManager.showAcceptFriendRequestNotification(it,notificationData,context)
                        MyNotificationManager.showGroupSummaryNotification(context)
                    }
                }.launchIn(this)
            }
        }
    }

    private fun handleNewFollowerNotification(notificationData: NotificationData, context: Context) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseManager.getUser(it).onEach {
                    it.data?.let {
                        MyNotificationManager.showNewFollowerNotification(it,notificationData,context)
                        MyNotificationManager.showGroupSummaryNotification(context)
                    }
                }.launchIn(this)
            }
        }
    }





    /*private fun setIntent(number: String, context: Context): Intent {
        MyLogger.d(msg = "Set intent now begin")
        val intent = Intent(context, IncomingCallActivity::class.java)

        //From outside activity context only this needed not other !
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        intent.putExtra(
            Constants.IntentTable.Number.name, number
        )


        // Need to update this code
        if (number != Constants.UNKNOWN_NUMBER) {
            MyLogger.i(msg = "All Digit is number -> $number")
            intent.putExtra(
                Constants.IntentTable.FormattedNumber.name,
                Helper.getFormattedNumber(number, context)
            )

        } else {
            MyLogger.i(msg = "Not all Digit is number ->$number")
            intent.putExtra(Constants.IntentTable.FormattedNumber.name, number)
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

        return intent
    }*/
}