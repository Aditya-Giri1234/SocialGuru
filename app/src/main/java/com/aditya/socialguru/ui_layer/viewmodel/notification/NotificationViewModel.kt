package com.aditya.socialguru.ui_layer.viewmodel.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.notification.UserNotificationModel
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.notification.NotificationRepo
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotificationViewModel(val app:Application) : AndroidViewModel(app) {

    private val tagNotification=Constants.LogTag.Notification

    private val repository=NotificationRepo()

    private val _notificationList=MutableSharedFlow<Resource<List<UserNotificationModel>>>(
        1,64,BufferOverflow.DROP_OLDEST
    )
    val notificationList get() = _notificationList.asSharedFlow()

    private val _singleNotificationDelete=MutableSharedFlow<Resource<UpdateResponse>>(
        0,64,BufferOverflow.DROP_OLDEST
    )
    val singleNotificationDelete get() = _singleNotificationDelete.asSharedFlow()

    private val _deleteAllNotification=MutableSharedFlow<Resource<UpdateResponse>>(
        0,64,BufferOverflow.DROP_OLDEST
    )
    val deleteAllNotification get() = _deleteAllNotification.asSharedFlow()


    private var _isDataLoaded=MutableLiveData<Boolean>(false)
    val isDataLoaded  get() = _isDataLoaded.value ?: false

    init {
        MyLogger.w(tagNotification, msg = "Notification viewmodel is initialized !")
    }


     fun getMyNotificationAndListen() = viewModelScope.myLaunch {
         _notificationList.tryEmit(Resource.Loading())

         if (SoftwareManager.isNetworkAvailable(app)) {
             repository.getMyNotificationAndListen().onEach {
                 MyLogger.v(tagNotification, msg = it, isJson = true)
                 _notificationList.tryEmit(handleNotificationListResponse(it))
             }.launchIn(this)

         } else {
             _notificationList.tryEmit(Resource.Error("No Internet Available !"))
         }
    }
    private fun handleNotificationListResponse(listenerHandling: ListenerEmissionType<UserNotificationModel, UserNotificationModel>): Resource<List<UserNotificationModel>> {
        MyLogger.v(tagNotification, isFunctionCall = true)

        val notificationList =
            notificationList.replayCache[0].data?.toMutableList() ?: mutableListOf<UserNotificationModel>()

        when (listenerHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagNotification,
                    msg = "This is starting notification type "
                )
                notificationList.clear()
                listenerHandling.responseList?.let {
                    notificationList.addAll(it.toMutableList() as ArrayList<UserNotificationModel>)
                    notificationList.sortByDescending { it.notificationData.notificationTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagNotification,
                    msg = "This is added notification type "
                )
                listenerHandling.singleResponse?.let {
                    notificationList.add(it)
                    notificationList.sortByDescending { it.notificationData.notificationTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagNotification, msg = "This is removed notification type")

                listenerHandling.singleResponse?.let { data ->
                    data.notificationData.notificationId.let { userId ->
                        notificationList.forEach { temp ->
                            if (temp.notificationData.notificationId == userId) {
                                notificationList.remove(temp)
                                notificationList.sortByDescending { it.notificationData.notificationTimeInTimeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(notificationList.toList())
    }


    fun deleteSingleNotification(notificationId:String) = viewModelScope.myLaunch{
        _singleNotificationDelete.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.deleteSingleNotification(notificationId).onEach {
                if (it.isSuccess) {
                    _singleNotificationDelete.tryEmit(Resource.Success(it))
                } else {
                    _singleNotificationDelete.tryEmit(
                        Resource.Error(
                            giveMeErrorMessage(
                                "Delete Single Notification Follower",
                                it.errorMessage.toString()
                            )
                        )
                    )
                }
            }.launchIn(this)
        } else {
            _singleNotificationDelete.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun deleteAllNotification() = viewModelScope.myLaunch{
        _deleteAllNotification.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.deleteAllNotification().onEach {
                if (it.isSuccess) {
                    _deleteAllNotification.tryEmit(Resource.Success(it))
                } else {
                    _deleteAllNotification.tryEmit(
                        Resource.Error(
                            giveMeErrorMessage(
                                "Delete All Notification Follower",
                                it.errorMessage.toString()
                            )
                        )
                    )
                }
            }.launchIn(this)
        } else {
            _deleteAllNotification.tryEmit(Resource.Error("No Internet Available !"))
        }
    }


    override fun onCleared() {
        MyLogger.w(tagNotification, msg = "Notification view model is finished !")
        super.onCleared()
    }



    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded.value=status
    }

}