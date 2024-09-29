package com.aditya.socialguru.ui_layer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.model.notification.UserNotificationModel
import com.aditya.socialguru.databinding.SampleNotificationItemBinding
import com.aditya.socialguru.domain_layer.custom_class.chauthai_library.ViewBinderHelper
import com.aditya.socialguru.domain_layer.helper.Constants.NotificationType
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide




class NotificationAdapter(val onNotificationDelete:(data:NotificationData)->Unit,val onNotificationClick: (data: NotificationData, isProfileView: Boolean) -> Unit) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    companion object {
        private val callback = object : DiffUtil.ItemCallback<UserNotificationModel>() {
            override fun areItemsTheSame(
                oldItem: UserNotificationModel,
                newItem: UserNotificationModel
            ): Boolean {
                return oldItem.notificationData.notificationId == newItem.notificationData.notificationId
            }

            override fun areContentsTheSame(
                oldItem: UserNotificationModel,
                newItem: UserNotificationModel
            ): Boolean {
                return oldItem.notificationData == newItem.notificationData
            }

        }
    }

    private val viewBinderHelper = ViewBinderHelper()

    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<UserNotificationModel>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SampleNotificationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: UserNotificationModel) {
            viewBinderHelper.setOpenOnlyOne(true)
            binding.apply {
                data.user.let {
                    if (it.userProfileImage==null){
                        tvInitial.myShow()
                        tvInitial.text = it.userName?.get(0).toString()
                        tvInitial.setCircularBackground(Helper.setUserProfileColor(it))
                    }else{
                        tvInitial.gone()
                        Glide.with(ivProfilePic).load(it.userProfileImage).placeholder(R.drawable.ic_user).error(
                            R.drawable.ic_user).into(ivProfilePic)
                    }

                    data.notificationData.let { notificationData ->

                        //Bind swipe layout to this id
                        viewBinderHelper.bind(binding.swipeLayout,notificationData.notificationId!!)


                        val (message, isProfileView) = when (notificationData.type) {
                            NotificationType.NEW_FOLLOWER.name -> {
                                Pair(it.userName?.let {
                                    "$it follow you."
                                } ?: "You get new Follower!", true)

                            }

                            NotificationType.FRIEND_REQUEST_COME.name -> {
                                Pair(it.userName?.let {
                                    "$it send friend request."
                                } ?: "You get new friend request!", true)

                            }

                            NotificationType.ACCEPT_FRIEND_REQUEST.name -> {
                                Pair(
                                    it.userName?.let {
                                        "$it accept your friend request."
                                    } ?: "Your friend request accepted.", true)

                            }

                            NotificationType.LIKE_IN_POST.name -> {
                                Pair(it.userName?.let {
                                    "$it like your post."
                                } ?: "Someone like your post!", false)
                            }

                            NotificationType.COMMENT_IN_POST.name -> {
                                Pair(it.userName?.let {
                                    "$it comment your post."
                                } ?: "Someone comment your post!", false)

                            }

                            else -> {
                                Pair("You get some new notification.", true)
                            }
                        }
                        tvNotificationMessage.text = message
                        notificationData.notificationTimeInTimeStamp?.let {
                            tvNotificationTime.text = Helper.getTimeAgo(it.toLong())
                        } ?: run {
                            tvNotificationTime.gone()
                        }

                        frameGrpMain.setSafeOnClickListener {
                            onNotificationClick(data.notificationData,isProfileView)
                        }
                        frameDelete.setSafeOnClickListener {
                            onNotificationDelete(data.notificationData)
                        }


                    }
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SampleNotificationItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }
    fun isListIsEmpty() : Boolean = differ.currentList.isEmpty()


}