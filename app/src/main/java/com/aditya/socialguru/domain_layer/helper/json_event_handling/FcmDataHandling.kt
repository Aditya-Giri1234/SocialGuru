package com.aditya.socialguru.domain_layer.helper.json_event_handling

import android.content.Context
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.MyNotificationManager
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.SharePref
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
        MyLogger.v(Constants.LogTag.Notification, msg = _notificationData)

        val notificationData =
            Gson().fromJson(_notificationData, NotificationData::class.java) ?: return
        notificationData.type ?: return

        when (notificationData.type) {
            Constants.NotificationType.NEW_FOLLOWER.name -> {
                handleNewFollowerNotification(notificationData, context)
            }

            Constants.NotificationType.ACCEPT_FRIEND_REQUEST.name -> {
                handleAcceptFriendRequestNotification(notificationData, context)
            }

            Constants.NotificationType.FRIEND_REQUEST_COME.name -> {
                handleFriendRequestComeNotification(notificationData, context)
            }

            Constants.NotificationType.LIKE_IN_POST.name -> {
                handleLikeInPostNotification(notificationData, context)
            }

            Constants.NotificationType.COMMENT_IN_POST.name -> {
                handleCommentInPostNotification(notificationData, context)
            }

            Constants.NotificationType.SINGLE_CHAT.name -> {
                handleSingleChatMessage(notificationData, context)
            }

            Constants.NotificationType.GROUP_CHAT.name -> {
                handleGroupChatMessage(notificationData, context)
            }
        }
    }


    private fun handleGroupChatMessage(notificationData: NotificationData, context: Context) {
        val senderId = notificationData.friendOrFollowerId!!
        val messageId = notificationData.messageId!!
        val chatRoomId = notificationData.chatRoomId!!
        launchCoroutineInIOThread {
            val messageData =
                async { FirebaseManager.getGroupMessageById(chatRoomId, messageId).first() }
            val userData = async { FirebaseManager.getUser(senderId).first().data }
            val groupInfoData = async { FirebaseManager.getGroupInfo(chatRoomId).first() }
            val user = userData.await()
            val message = messageData.await()
            val groupInfo = groupInfoData.await()
            if (user != null) {
                FirebaseManager.updateGroupReceivedStatus(messageId, chatRoomId, senderId)
                SharePref(context).getPrefUser().first()?.let { myData ->
                    val userSetting = myData.userSetting
                    if(userSetting==null || userSetting.isGroupChatNotificationMute!=true){
                        if (!FirebaseManager.isUserMuted(chatRoomId)) {
                            MyNotificationManager.showGroupChatMessage(
                                user,
                                myData,
                                notificationData,
                                message,
                                groupInfo,
                                context
                            )
//                                MyNotificationManager.showGroupChatNotification(context)
                            MyNotificationManager.showGroupSummaryNotification(context)
                        } else {
                            MyLogger.w(
                                Constants.LogTag.Notification,
                                msg = "Notification come of chat but sender is muted so that no notification show !"
                            )
                        }
                    }
                }
            }
        }
    }


    private fun handleSingleChatMessage(notificationData: NotificationData, context: Context) {
        val userId = notificationData.friendOrFollowerId!!   // senderId
        val messageId = notificationData.messageId!!
        val chatRoomId = notificationData.chatRoomId!!
        launchCoroutineInIOThread {
            val messageData =
                async { FirebaseManager.getMessageById(chatRoomId, messageId).first() }
            val userData = async { FirebaseManager.getUser(userId).first().data }
            val user = userData.await()
            val message = messageData.await()
            if (user != null) {
                FirebaseManager.updateSeenStatus(
                    Constants.SeenStatus.Received.status,
                    messageId,
                    chatRoomId,
                    notificationData.friendOrFollowerId
                )
                SharePref(context).getPrefUser().first()?.let { myData ->
                    val userSetting = myData.userSetting
                    if (userSetting==null || userSetting.isSingleChatNotificationMute!=true){
                        if (!FirebaseManager.isUserMuted(userId)) {
                            MyNotificationManager.showSingleChatMessage(
                                user,
                                notificationData,
                                message,
                                context
                            )
//                                MyNotificationManager.showGroupChatNotification(context)
                            MyNotificationManager.showGroupSummaryNotification(context)
                        } else {
                            MyLogger.w(
                                Constants.LogTag.Notification,
                                msg = "Notification come of chat but sender is muted so that no notification show !"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleCommentInPostNotification(
        notificationData: NotificationData,
        context: Context
    ) {
        val userId = notificationData.friendOrFollowerId!!   // senderId
        val messageId = notificationData.messageId!!
        val postId = notificationData.postId!!
        launchCoroutineInIOThread {
            SharePref(context).getPrefUser().first()?.let { user ->
                val userSetting = user.userSetting
                if(userSetting ==null || userSetting.isPostNotificationMute!=true){
                    val commentData =
                        async { FirebaseManager.getCommentById(postId, messageId).first() }
                    val userData = async { FirebaseManager.getUser(userId).first().data }
                    val senderData  = userData.await()
                    val message = commentData.await()
                    if (senderData != null) {
                        MyNotificationManager.showCommentOnPostNotification(
                            senderData,
                            notificationData,
                            message,
                            context
                        )
//                            MyNotificationManager.showGroupPostNotification(context)
                        MyNotificationManager.showGroupSummaryNotification(context)
                    }
                }
            }
        }
    }

    private fun handleLikeInPostNotification(notificationData: NotificationData, context: Context) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                SharePref(context).getPrefUser().first()?.let { user ->
                    val userSetting = user.userSetting
                    if (userSetting == null || userSetting.isPostNotificationMute != true) {
                        FirebaseManager.getUser(it).first().let{
                            it.data?.let {
                                MyNotificationManager.showLikeInPostNotification(
                                    it,
                                    notificationData,
                                    context
                                )
                                MyNotificationManager.showGroupSummaryNotification(context)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleFriendRequestComeNotification(
        notificationData: NotificationData,
        context: Context
    ) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                SharePref(context).getPrefUser().first()?.let { user ->
                    val userSetting = user.userSetting
                    if(userSetting==null || userSetting.isFriendCircleNotificationMute!=true){
                        FirebaseManager.getUser(it).first().let {
                            it.data?.let {
                                MyNotificationManager.showFriendRequestComeNotification(
                                    it,
                                    notificationData,
                                    context
                                )
//                                    MyNotificationManager.showGroupFriendCircleNotification(context)
                                MyNotificationManager.showGroupSummaryNotification(context)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleAcceptFriendRequestNotification(
        notificationData: NotificationData,
        context: Context
    ) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                SharePref(context).getPrefUser().first()?.let { user ->
                    val userSetting = user.userSetting
                    if(userSetting==null || userSetting.isFriendCircleNotificationMute!=true){
                        FirebaseManager.getUser(it).first().let {
                            it.data?.let {
                                MyNotificationManager.showAcceptFriendRequestNotification(
                                    it,
                                    notificationData,
                                    context
                                )
//                                    MyNotificationManager.showGroupFriendCircleNotification(context)
                                MyNotificationManager.showGroupSummaryNotification(context)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleNewFollowerNotification(
        notificationData: NotificationData,
        context: Context
    ) {
        notificationData.friendOrFollowerId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                SharePref(context).getPrefUser().first()?.let { user ->
                    val userSetting = user.userSetting
                    if(userSetting==null || userSetting.isFriendCircleNotificationMute!=true){
                        FirebaseManager.getUser(it).first().let {
                            it.data?.let {
                                MyNotificationManager.showNewFollowerNotification(
                                    it,
                                    notificationData,
                                    context
                                )
//                                    MyNotificationManager.showGroupFriendCircleNotification(context)
                                MyNotificationManager.showGroupSummaryNotification(context)
                            }
                        }
                    }
                }
            }
        }
    }
}