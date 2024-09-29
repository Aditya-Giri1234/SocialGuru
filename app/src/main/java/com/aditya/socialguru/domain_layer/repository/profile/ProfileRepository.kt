package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class ProfileRepository {
    suspend fun subscribeToFollowerCount(userId: String) =
        FirebaseManager.subscribeToFollowerCount(userId)

    suspend fun subscribeToFollowingCount(userId: String) =
        FirebaseManager.subscribeToFollowingCount(userId)


    suspend fun subscribeToPostCount(userId: String) = FirebaseManager.subscribeToPostCount(userId)

    suspend fun subscribeToLikeCount(userId: String) = FirebaseManager.subscribeToLikeCount(userId)

    suspend fun signOut()=FirebaseManager.signOut()

    suspend fun getUser(userId: String) = FirebaseManager.getUser(userId)

    suspend fun unFollow(userId: String)=FirebaseManager.unFollow(userId)
    suspend fun removeFriend(userId: String)=FirebaseManager.removeFriend(userId)

    suspend fun followUser(userId: String, followedId: String)=
        FirebaseManager.followUser(userId,followedId)
    suspend fun sendFriendRequest(userId: String, friendId: String)=
        FirebaseManager.sendFriendRequest(userId,friendId)
    suspend fun deleteFriendRequest(userId: String, friendId: String)=
        FirebaseManager.deleteFriendRequest(userId,friendId)

    suspend fun acceptFriendRequest(userId: String, friendId: String) = FirebaseManager.acceptFriendRequest(userId,friendId)

    suspend fun getUserRelationshipStatus(currentUserId: String, targetUserId: String)=
        FirebaseManager.getUserRelationshipStatus(currentUserId, targetUserId)

    fun listenUserRelationStatus(friendId: String)=FirebaseManager.listenUserRelationStatus(friendId)

    suspend fun findUser(query: String) = FirebaseManager.findUser(query)


}