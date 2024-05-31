package com.aditya.socialguru.domain_layer.service.firebase_service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.storage_manager.StorageManagerStatus
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


/**
 * [StorageManager] This class only do crud  operation on firebase storage [com.google.firebase.storage.FirebaseStorage] folder and its subFolde.
 * */
object StorageManager {

    private val tagStory=Constants.LogTag.Story

    // Firebase storage reference
    private val storageReference = FirebaseStorage.getInstance().reference


    fun uploadImageToServer(rootFolderName:String,folderName: String, imageUri: Uri) =
        callbackFlow<StorageManagerStatus> {

            val storageRef =
                storageReference.child(rootFolderName).child(folderName)

            storageRef.putFile(imageUri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = calculateProgress(taskSnapshot)
                    trySend(
                        StorageManagerStatus(
                            Constants.StorageManagerState.InProgress,
                            progress = progress
                        )
                    )
                }.addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        MyLogger.i(tagStory, msg = "Image uploading successfully !")
                        downLoadUri(storageRef) { uri: Uri?, error: String ->
                            if (uri != null) {
                                MyLogger.i(tagStory, msg = "Uri is not :- $uri")
                                trySend(
                                    StorageManagerStatus(
                                        Constants.StorageManagerState.Success,
                                        url = uri.toString()
                                    )
                                )
                            } else {
                                MyLogger.e(tagStory, msg = " Error when get url :- $error")
                                trySend(
                                    StorageManagerStatus(
                                        Constants.StorageManagerState.UrlNotGet,
                                        error = giveMeErrorMessage("Download url", error.toString())
                                    )
                                )
                            }
                        }
                    } else {
                        MyLogger.i(tagStory, msg = "Image uploading failed !")
                        trySend(
                            StorageManagerStatus(
                                Constants.StorageManagerState.Error,
                                error = giveMeErrorMessage(
                                    "Image Uploading ",
                                    task.exception?.message.toString()
                                )
                            )
                        )
                    }
                }.await()

            awaitClose {
                channel.close()
            }
        }


    fun uploadVideoToServer(rootFolder:String,folderName:String,videoUri: Uri) =
        callbackFlow<StorageManagerStatus> {

            val storageRef =
                storageReference.child(rootFolder).child(folderName)

            storageRef.putFile(videoUri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = calculateProgress(taskSnapshot)
                    trySend(
                        StorageManagerStatus(
                            Constants.StorageManagerState.InProgress,
                            progress = progress
                        )
                    )
                }.addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        MyLogger.i(tagStory, msg = "Image uploading successfully !")
                        downLoadUri(storageRef) { uri: Uri?, error: String ->
                            if (uri != null) {
                                MyLogger.i(tagStory, msg = "Uri is not :- $uri")
                                trySend(
                                    StorageManagerStatus(
                                        Constants.StorageManagerState.Success,
                                        url = uri.toString()
                                    )
                                )
                            } else {
                                MyLogger.e(tagStory, msg = " Error when get url :- $error")
                                trySend(
                                    StorageManagerStatus(
                                        Constants.StorageManagerState.UrlNotGet,
                                        error = giveMeErrorMessage("Download url", error.toString())
                                    )
                                )
                            }
                        }
                    } else {
                        MyLogger.i(tagStory, msg = "Image uploading failed !")
                        trySend(
                            StorageManagerStatus(
                                Constants.StorageManagerState.Error,
                                error = giveMeErrorMessage(
                                    "Image Uploading ",
                                    task.exception?.message.toString()
                                )
                            )
                        )
                    }
                }.await()

            awaitClose {
                channel.close()
            }
        }

    private fun downLoadUri(
        storageReference: StorageReference,
        onResult: (uri: Uri?, error: String) -> Unit
    ) {
        storageReference.downloadUrl.addOnCompleteListener { urlResult ->
            if (urlResult.isSuccessful) {
                MyLogger.i(msg = "Story url download successfully ...")
                onResult(urlResult.result ,"")

            } else {
                MyLogger.e(msg = "Story url download failed  ...")
                AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UrlNotGet)
            }
        }
    }


    private fun calculateProgress(taskSnapshot: UploadTask.TaskSnapshot): Int {
        return (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
    }

}



