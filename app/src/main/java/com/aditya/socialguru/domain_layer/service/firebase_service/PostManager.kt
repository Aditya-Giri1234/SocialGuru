package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.post.DiscoverPostListenerEmissionType
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.PostUploadingResponse
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.StoryListenerEmissionType
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


/**
 * [PostManager] This class only do crud  operation on firebase firestore [com.aditya.socialguru.data_layer.model.post.Post] collection and its subCollection.
 * */
object PostManager {

    private val tagPost = Constants.LogTag.Post

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }


    private var discoverPostListener: ListenerRegistration? = null
    private var isFirstTimeDiscoverPostListnerCall = true

    private var followingPostListener: ListenerRegistration? = null
    private var isFirstTimeFollowingPostListnerCall = true


    //region::Get Random Post (Discover)


    suspend fun getDiscoverPost() = callbackFlow<DiscoverPostListenerEmissionType> {

        val discoverPostQuery = firestore.collection(Constants.Table.Post.name)
        val posts = discoverPostQuery.get().await()


        val userPostList = posts.mapNotNull {
            val post=it.toObject<Post>()
            post.userId?.let { id ->
                UserManager.getUserById(id)?.let { UserPostModel(it,post) }
            }
        }.toMutableList()


        MyLogger.i(
            tagPost,
            msg = userPostList,
            isJson = true,
            jsonTitle = "User Post list !"
        )

        trySend(
            DiscoverPostListenerEmissionType(
                Constants.PostEmitType.Starting,
                userPostList = userPostList.toList()
            )
        )
        userPostList.clear()
        discoverPostListener?.remove()
        discoverPostListener = discoverPostQuery.addSnapshotListener { value, error ->

            if (error!=null) return@addSnapshotListener

            if (!isFirstTimeDiscoverPostListnerCall) {
                value?.documentChanges?.forEach { document ->
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            trySend(
                                DiscoverPostListenerEmissionType(
                                    Constants.PostEmitType.Added,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        DocumentChange.Type.REMOVED -> {
                            trySend(
                                DiscoverPostListenerEmissionType(
                                    Constants.PostEmitType.Removed,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        else -> {}
                    }
                }
            }
            isFirstTimeDiscoverPostListnerCall = false

        }


        awaitClose {
            discoverPostListener?.remove()
            channel.close()
        }

    }


    //endregion


    //region::Get Following Post (Discover)


    suspend fun getFollowingPost() = callbackFlow<DiscoverPostListenerEmissionType> {

        val discoverPostQuery = firestore.collection(Constants.Table.Post.name)
        val posts = discoverPostQuery.get().await()


        val userPostList = posts.mapNotNull {
            val post=it.toObject<Post>()
            post.userId?.let { id ->
                UserManager.getUserById(id)?.let { UserPostModel(it,post) }
            }
        }.toMutableList()


        MyLogger.i(
            tagPost,
            msg = userPostList,
            isJson = true,
            jsonTitle = "User Post list !"
        )

        trySend(
            DiscoverPostListenerEmissionType(
                Constants.PostEmitType.Starting,
                userPostList = userPostList.toList()
            )
        )
        userPostList.clear()
        followingPostListener?.remove()
        followingPostListener = discoverPostQuery.addSnapshotListener { value, error ->

            if (error!=null) return@addSnapshotListener

            if (!isFirstTimeFollowingPostListnerCall) {
                value?.documentChanges?.forEach { document ->
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            trySend(
                                DiscoverPostListenerEmissionType(
                                    Constants.PostEmitType.Added,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        DocumentChange.Type.REMOVED -> {
                            trySend(
                                DiscoverPostListenerEmissionType(
                                    Constants.PostEmitType.Removed,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        else -> {}
                    }
                }
            }
            isFirstTimeFollowingPostListnerCall = false

        }


        awaitClose {
            discoverPostListener?.remove()
            channel.close()
        }

    }


    //endregion


    //region:: Post Uploading


    suspend fun uploadPost(post: Post) {
        val isImagePresent = post.imageUrl != null
        val isVideoPresent = post.videoUrl != null

        when {
            isVideoPresent -> {
                uploadVideo(post, isImagePresent)
            }

            isImagePresent -> {
                uploadImage(post)
            }

            else -> {
                savePostToDatabase(post)
            }
        }
    }


    private suspend fun uploadVideo(post: Post, imagePresent: Boolean) {
        AppBroadcastHelper.setPostUploadState(
            PostUploadingResponse(
                Constants.PostUploadState.VideoUploading
            )
        )
        MyLogger.v(tagPost, isFunctionCall = true)
        val videoUri = post.videoUrl!!.convertParseUri()
        StorageManager.uploadVideoToServer(
            Constants.Table.Post.name,
            Constants.FolderName.PostVideo.name,
            videoUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.VideoUploading,
                            it.progress
                        )
                    )
                }

                Constants.StorageManagerState.Error -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.Success -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.VideoUploaded
                        )
                    )
                    if (imagePresent) {
                        uploadImage(post, it.url)
                    } else {
                        savePostToDatabase(post, it.url)
                    }
                }
            }
        }
    }


    private suspend fun uploadImage(post: Post, onLineVideoUri: String? = null) {
        AppBroadcastHelper.setPostUploadState(
            PostUploadingResponse(
                Constants.PostUploadState.ImageUploading
            )
        )

        MyLogger.v(tagPost, isFunctionCall = true)
        val imageUri = post.imageUrl!!.convertParseUri()
        StorageManager.uploadImageToServer(
            Constants.Table.Post.name,
            Constants.FolderName.PostVideo.name,
            imageUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.ImageUploading,
                            it.progress
                        )
                    )
                }

                Constants.StorageManagerState.Error -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.Success -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.ImageUploaded
                        )
                    )
                    savePostToDatabase(post, onLineVideoUri, it.url)
                }
            }
        }
    }


    private suspend fun savePostToDatabase(
        post: Post,
        onlineVideoUri: String? = null,
        onlineImageUri: String? = null
    ) {
        MyLogger.v(tagPost, isFunctionCall = true)
        AppBroadcastHelper.setPostUploadState(
            PostUploadingResponse(
                Constants.PostUploadState.PostUploading
            )
        )

        val saveDatabaseReference =
            firestore.collection(Constants.Table.Post.name)
                .document(post.postId!!)
        val updatedPost = post.copy(
            imageUrl = onlineImageUri?.toString(),
            videoUrl = onlineVideoUri?.toString(),
            postUploadingTimeInTimeStamp = System.currentTimeMillis()
        )
        saveDatabaseReference.set(updatedPost).addOnCompleteListener {
            if (it.isSuccessful) {
                MyLogger.i(tagPost, msg = "Post saved successfully.")
                AppBroadcastHelper.setPostUploadState(
                    PostUploadingResponse(
                        Constants.PostUploadState.PostUploaded
                    )
                )

            } else {
                MyLogger.e(
                    tagPost,
                    msg = giveMeErrorMessage("Uploading post", it.exception.toString())
                )
                AppBroadcastHelper.setPostUploadState(
                    PostUploadingResponse(
                        Constants.PostUploadState.Error
                    )
                )
            }
        }.await()

    }

    //endregion

}