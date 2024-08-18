package com.aditya.socialguru.domain_layer.repository.comment

import com.aditya.socialguru.data_layer.model.post.Comment
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.CommentManager

class CommentRepo {

    suspend fun sendComment(
        comment: Comment, isCreatorOnline: Boolean
    ) = CommentManager.sendComment(comment, isCreatorOnline)

    suspend fun deleteComment(comment: Comment) = CommentManager.deleteComment(comment)
    suspend fun getCommentAndListen(postId: String) = CommentManager.getCommentAndListen(postId)

    suspend fun getCommentersAndListen(postId: String) = CommentManager.getCommentersAndListen(postId)

    suspend fun updateMyOnlineStatus(postId: String, status:Boolean,post: Post?=null) = FirebaseManager.updateMyOnlineStatus(postId,status ,post)

}