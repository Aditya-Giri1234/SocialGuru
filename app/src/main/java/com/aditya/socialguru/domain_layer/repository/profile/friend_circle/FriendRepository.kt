package com.aditya.socialguru.domain_layer.repository.profile.friend_circle

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class FriendRepository {
    suspend fun getFollowerAndListenChangeEvent()=FirebaseManager.getFollowerListAndListenChange()
    suspend fun getFollowingListAndListenChange()=FirebaseManager.getFollowingListAndListenChange()
    suspend fun getFriendListAndListenChange()=FirebaseManager.getFriendListAndListenChange()
    suspend fun removeFollower(userId: String)= FirebaseManager.removeFollower(userId)
    suspend fun unFollow(userId: String)= FirebaseManager.unFollow(userId)
    suspend fun removeFriend(userId: String)= FirebaseManager.removeFriend(userId)

    fun listenFriendRequestComeEvent()= UserManager.listenFriendRequestComeEvent()

    suspend fun declineFriendRequest(userId: String, friendId: String) =
        FirebaseManager.declineFriendRequest(userId, friendId)

    suspend fun acceptFriendRequest(userId: String, friendId: String) = FirebaseManager.acceptFriendRequest(userId,friendId)

}