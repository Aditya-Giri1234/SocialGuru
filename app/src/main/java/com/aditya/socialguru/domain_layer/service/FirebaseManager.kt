package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.ChatManager
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

    //endregion

    //region::  Post Related Work Here

    suspend fun uploadingPost(post: Post) = PostManager.uploadPost(post)

    suspend fun getDiscoverPost() = PostManager.getDiscoverPost()
    suspend fun getFollowingPost() = PostManager.getFollowingPost()

    suspend fun getMyPost(userId: String) = PostManager.getMyPost(userId)

    suspend fun getPostById(postId: String) = PostManager.getPostById(postId)
    suspend fun getMyLikedPost(userId: String) = PostManager.getMyLikedPost(userId)

    suspend fun subscribeToPostCount(userId: String) = UserManager.subscribeToPostCount(userId)

    suspend fun subscribeToLikeCount(userId: String) = UserManager.subscribeToLikeCount(userId)

    suspend fun updateLikeCount(postId: String, postCreatorUserId: String, isLiked: Boolean) =
        PostManager.updateLikeCount(postId, postCreatorUserId, isLiked)

    suspend fun getPostLikeUser(postId: String) = PostManager.getPostLikeUser(postId)

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
    fun updateSeenStatus(status:String,messageId:String,chatRoomId: String ,receiverId:String) = ChatManager.updateSeenStatus(status,messageId,chatRoomId ,receiverId)

    suspend fun listenLastMessage(chatRoomId: String) = ChatManager.listenLastMessage(chatRoomId)
    suspend fun updateUserAvailabilityForChatRoom(chatRoomId: String,isIAmUser1:Boolean,status: Boolean)=ChatManager.updateUserAvailabilityForChatRoom(chatRoomId
    ,isIAmUser1,status)

    suspend fun getMessageById(chatRoomId: String,messageId:String) = ChatManager.getMessageById(chatRoomId,messageId)
    suspend fun getRecentChatAndListen() = ChatManager.getRecentChatAndListen()

    suspend fun deleteMessage(message: Message,chatRoomId:String,userId:String  , lastMessage: LastMessage?, secondLastMessage:Message?=null) = ChatManager.deleteMessage(message,chatRoomId,userId,lastMessage,secondLastMessage)
    //endregion
}