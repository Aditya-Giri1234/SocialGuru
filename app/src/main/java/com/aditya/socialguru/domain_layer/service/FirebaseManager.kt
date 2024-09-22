package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.UserSetting
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.data_layer.model.post.Comment
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.ChatManager
import com.aditya.socialguru.domain_layer.service.firebase_service.CommentManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

object FirebaseManager {

    //region:: Auth related work here

    suspend fun login(email: String, password: String): Pair<String?, String?> =
        AuthManager.loginUser(email, password)

    suspend fun signUp(user: User): Pair<FirebaseUser?, String?> = AuthManager.createUser(user)

    suspend fun signOut() = AuthManager.signOutUser()

    fun isUserLogin(): Boolean = AuthManager.isUserLogin()

    suspend fun sendPasswordResetEmail(email: String?) = AuthManager.sendPasswordResetEmail(email)

    suspend fun updateUserEmailId(newEmailId: String, password: String) = AuthManager.updateUserEmailId(newEmailId , password)

    suspend fun updateUserPassword(currentPassword:String ,newPassword:String) = AuthManager.updateUserPassword(currentPassword , newPassword)

    suspend fun listenAuthOfUser() = AuthManager.listenAuthOfUser()


    //endregion

    //region:: User related work here
    suspend fun updateUserAvailability(isUserAvailable:Boolean) = UserManager.updateUserAvailability(isUserAvailable)
    suspend fun saveUserToDatabase(user: User): Pair<Boolean, String?> = UserManager.saveUser(user)

    suspend fun updateUser(user: User, oldImage: String? = null, newImage: String? = null) =
        UserManager.updateUser(user, oldImage, newImage)

    suspend fun getUser(userId: String): Flow<Resource<User>> = UserManager.getUserByIdAsync(userId)

    suspend fun subscribeToFollowerCount(userId: String) =
        UserManager.subscribeToFollowerCount(userId)

    suspend fun subscribeToFollowingCount(userId: String) =
        UserManager.subscribeToFollowingCount(userId)

    suspend fun subscribeToCurrentUser(userId: String) =
        UserManager.subscribeToCurrentUserData(userId)

    suspend fun getFollowerListAndListenChange() =
        UserManager.getFollowerListAndListenChange(AuthManager.currentUserId() ?: "")

    suspend fun getFollowingListAndListenChange() =
        UserManager.getFollowingListAndListenChange(AuthManager.currentUserId() ?: "")

    suspend fun getFriendListAndListenChange() =
        UserManager.getFriendListAndListenChange(AuthManager.currentUserId() ?: "")

    suspend fun removeFollower(userId: String) = UserManager.removeFollower(userId)
    suspend fun unFollow(userId: String) = UserManager.unFollow(userId)
    suspend fun removeFriend(userId: String) = UserManager.removeFriend(userId)

    suspend fun followUser(userId: String, followedId: String) =
        UserManager.followUser(userId, followedId)

    suspend fun sendFriendRequest(userId: String, friendId: String) =
        UserManager.sendFriendRequest(userId, friendId)

    suspend fun deleteFriendRequest(userId: String, friendId: String) =
        UserManager.deleteFriendRequest(userId, friendId)

    suspend fun declineFriendRequest(userId: String, friendId: String) =
        UserManager.declineFriendRequest(userId, friendId)

    suspend fun acceptFriendRequest(userId: String, friendId: String) =
        UserManager.acceptFriendRequest(userId, friendId)

    suspend fun getUserRelationshipStatus(currentUserId: String, targetUserId: String) =
        UserManager.getUserRelationshipStatus(currentUserId, targetUserId)

    fun listenUserRelationStatus(friendId: String) = UserManager.listenUserRelationStatus(friendId)

    fun listenFriendRequestComeEvent() = UserManager.listenFriendRequestComeEvent()

    suspend fun setFcmToken(token: String?) = UserManager.setFcmToken(token)

    suspend fun getAllUserByIds(userIds:List<String>) = UserManager.getAllUserByIds(userIds)

    suspend fun findUser(query: String) = UserManager.findUser(query)

    suspend fun deleteAccount() = AuthManager.deleteAccount()

    suspend fun updateUserSetting(userSetting: UserSetting) = UserManager.updateUserSetting(userSetting)

    //endregion

    //region:: Story Related work here
    suspend fun uploadStory(
        storyType: Constants.StoryType,
        uri: Uri? = null,
        text: StoryText? = null,
        user: User
    ) = StoryManager.uploadStory(storyType, uri, text, user)

    suspend fun getAllStories(userId: String) =
        StoryManager.getAndListenTop30Friend(userId = userId)

    suspend fun getMyStory(userId: String) = StoryManager.getMyStory(userId)

    suspend fun deleteStoryById(storyId: String) = StoryManager.deleteStoryById(storyId)
    suspend fun deleteAllMyStories() = StoryManager.deleteAllMyStories()

    suspend fun deleteStaleStories() = StoryManager.deleteStaleStories()
    suspend fun listenMyStoryCount() = StoryManager.listenMyStoryCount()

    //endregion

    //region::  Post Related Work Here

    suspend fun uploadingPost(post: Post) = PostManager.uploadPost(post)

    suspend fun getDiscoverPost() = PostManager.getDiscoverPost()
    suspend fun getFollowingPost(userIds: List<String>) = PostManager.getFollowingPost(userIds)

    suspend fun getMyPost(userId: String) = PostManager.getMyPost(userId)
    suspend fun deletePostById(postId: String) = PostManager.deletePostById(postId)
    suspend fun deleteAllMyPost() = PostManager.deleteAllMyPost()

    suspend fun getPostById(postId: String) = PostManager.getPostById(postId)
    suspend fun getMyLikedPost(userId: String) = PostManager.getMyLikedPost(userId)

    suspend fun subscribeToPostCount(userId: String) = UserManager.subscribeToPostCount(userId)

    suspend fun subscribeToLikeCount(userId: String) = UserManager.subscribeToLikeCount(userId)

    suspend fun updateLikeCount(postId: String, postCreatorUserId: String, isLiked: Boolean) =
        PostManager.updateLikeCount(postId, postCreatorUserId, isLiked)

    suspend fun getPostLikeUser(postId: String) = PostManager.getPostLikeUser(postId)

    suspend fun updatePostSaveStatus(postId: String) = PostManager.updatePostSaveStatus(postId)
    suspend fun listenMySavedPost() = PostManager.listenMySavedPost()
    suspend fun listenMyLikedPost() = PostManager.listenMyLikedPost()
    suspend fun listenCommentedPost(userId: String) = PostManager.listenCommentedPost(userId)
    suspend fun listenMySavedPostForScreenView() = PostManager.listenMySavedPostForScreenView()

    //endregion

    //region:: Notification related work here

    suspend fun getMyNotificationAndListen() = UserManager.getMyNotificationAndListen()
    suspend fun deleteSingleNotification(notificationId: String) =
        UserManager.deleteSingleNotification(notificationId)

    suspend fun deleteAllNotification() = UserManager.deleteAllNotification()
    //endregion

    //region:: Chat related work here
    suspend fun sentMessage(message: Message ,lastMessage: LastMessage, chatRoomId: String , isUserOnline:Boolean=false)=ChatManager.sentMessage(message ,lastMessage,chatRoomId,isUserOnline)

    suspend fun getChatMessageAndListen(chatRoomId: String) = ChatManager.getChatMessageAndListen(chatRoomId)
    suspend fun getChatMessageAndUpdateSeenStatusForNotification(chatRoomId: String) = ChatManager.getChatMessageAndUpdateSeenStatusForNotification(chatRoomId)

    suspend fun updateSeenStatusForGroupChatForNotification(chatRoomId: String) = ChatManager.updateSeenStatusForGroupChatForNotification(chatRoomId)
    fun updateSeenStatus(status:String,messageId:String,chatRoomId: String ,receiverId:String) = ChatManager.updateSeenStatus(status,messageId,chatRoomId ,receiverId)

    suspend fun listenLastMessage(chatRoomId: String) = ChatManager.listenLastMessage(chatRoomId)
    suspend fun updateUserAvailabilityForChatRoom(chatRoomId: String,isIAmUser1:Boolean,status: Boolean)=ChatManager.updateUserAvailabilityForChatRoom(chatRoomId
    ,isIAmUser1,status)

    suspend fun getMessageById(chatRoomId: String,messageId:String) = ChatManager.getMessageById(chatRoomId,messageId)
    suspend fun getGroupMessageById(chatRoomId: String, messageId: String) = ChatManager.getGroupMessageById(chatRoomId,messageId)
    suspend fun getCommentById(postId: String, commentId: String) = CommentManager.getCommentById(postId,commentId)
    suspend fun getRecentChatAndListen() = ChatManager.getRecentChatAndListen()

    suspend fun deleteMessage(message: Message,chatRoomId:String,userId:String  , lastMessage: LastMessage?, secondLastMessage:Message?=null) = ChatManager.deleteMessage(message,chatRoomId,userId,lastMessage,secondLastMessage)

    suspend fun clearChats(chatRoomId: String,receiverId: String) = ChatManager.clearChats(chatRoomId,receiverId)
    suspend fun muteChatNotification(userId: String, isMute: Boolean) = ChatManager.muteChatNotification(userId, isMute)

    suspend fun isUserMuted(userId: String) : Boolean = ChatManager.isUserMuted(userId)
    suspend fun isUserMutedAndListen(userId: String) = ChatManager.isUserMutedAndListen(userId)

    suspend fun getAllMediaOfChat(chatRoomId: String) = ChatManager.getAllMediaOfChat(chatRoomId)


    // group chat

    suspend fun sentGroupMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>, // Just for add  message recent chat
        action: Constants.InfoType? = null,
        addedOrRemovedUserId: String? = null,
        groupInfo: GroupInfo? = null
    ) = ChatManager.sentGroupMessage(
        message,
        lastMessage,
        chatRoomId,
        users,
        action,
        addedOrRemovedUserId,
        groupInfo
    )

    suspend fun sentGroupInfoMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,// Just for add  message recent chat
        action: Constants.InfoType,
        addedOrRemovedUserId: String? = null,
        newMembers:List<String>?=null,
        groupInfo: GroupInfo? = null
    ) = ChatManager.sentGroupInfoMessage(message, lastMessage, chatRoomId, users, action, addedOrRemovedUserId, newMembers,groupInfo)

    suspend fun updateGroupInfo(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>, // Just for add  message recent chat
        groupInfo: GroupInfo? = null ,
        deleteImage:String?=null ,
        uploadingImage:String?=null
    ) = ChatManager.updateGroupInfo(
        message,
        lastMessage,
        chatRoomId,
        users,
        groupInfo ,
        deleteImage ,
        uploadingImage
    )

    suspend fun deleteMessage(
        message: GroupMessage,
        chatRoomId: String,
        users: List<String>,
        lastMessage: GroupLastMessage?,
        secondLastMessage: GroupMessage? = null
    ) = ChatManager.deleteMessage(message, chatRoomId, users, lastMessage, secondLastMessage)

    suspend fun clearChats(chatRoomId: String, users: List<String>, lastMessage: GroupLastMessage,
                           secondLastMessage: GroupMessage) = ChatManager.clearChats(chatRoomId, users ,lastMessage,secondLastMessage)


    suspend fun getGroupChatMessageAndListen(chatRoomId: String) = ChatManager.getGroupChatMessageAndListen(chatRoomId)

    suspend fun getGroupMemberInfo(chatRoomId: String) = ChatManager.getGroupMemberInfo(chatRoomId)

    suspend fun getGroupInfo(chatRoomId: String) = ChatManager.getGroupInfo(chatRoomId)

    suspend fun updateGroupMemberOnlineStatus(chatRoomId: String,status:Boolean) = ChatManager.updateGroupMemberOnlineStatus(chatRoomId,status)
    suspend fun listenNewMessage(chatRoomId: String) = ChatManager.listenNewMessage(chatRoomId)

    suspend fun  updateGroupReceivedStatus(
        messageId: String,
        chatRoomId: String,
        senderId: String
    ) = ChatManager.updateGroupReceivedStatus(messageId,chatRoomId,senderId)


    //endregion

    //region :: Recent Chat
    suspend fun deleteRecentChat(chatRoomId: String) = ChatManager.deleteRecentChat(chatRoomId)
    //endregion

    //region:: Comment related work

    suspend fun sendComment(
        comment: Comment, isCreatorOnline: Boolean
    ) = CommentManager.sendComment(comment, isCreatorOnline)

    suspend fun deleteComment(comment: Comment) = CommentManager.deleteComment(comment)

    suspend fun getCommentAndListen(postId: String) = CommentManager.getCommentAndListen(postId)

    suspend fun getCommentersAndListen(postId: String) = CommentManager.getCommentersAndListen(postId)
    suspend fun updateMyOnlineStatus(postId: String, status:Boolean,post: Post?=null) = CommentManager.updateMyOnlineStatus(postId,status ,post)

    //endregion
}