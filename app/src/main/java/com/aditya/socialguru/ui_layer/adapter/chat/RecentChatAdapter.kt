package com.aditya.socialguru.ui_layer.adapter.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.chat.UserRecentModel
import com.aditya.socialguru.databinding.SampleRecentChatLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.giveMeColor
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.bumptech.glide.Glide

class RecentChatAdapter(
    val onItemClick: (UserRecentModel) -> Unit,
    val onItemLongClick: (UserRecentModel) -> Unit
) :
    RecyclerView.Adapter<RecentChatAdapter.ViewHolder>() {
    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<UserRecentModel>() {
        override fun areItemsTheSame(oldItem: UserRecentModel, newItem: UserRecentModel): Boolean {
            return oldItem.user?.userId == newItem.user?.userId
        }

        override fun areContentsTheSame(
            oldItem: UserRecentModel,
            newItem: UserRecentModel
        ): Boolean {
            return oldItem == newItem
        }
    })

    fun submitList(list: List<UserRecentModel>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SampleRecentChatLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UserRecentModel) {
            binding.apply {
                item.user?.apply {
                    Glide.with(ivProfilePic.context).load(userProfileImage)
                        .placeholder(R.drawable.ic_user).error(R.drawable.ic_user)
                        .into(ivProfilePic)
                    tvUserName.text = userName
                } ?: run{
                    item.groupInfo?.run {
                        Glide.with(ivProfilePic.context).load(groupPic)
                            .placeholder(R.drawable.ic_user).error(R.drawable.ic_user)
                            .into(ivProfilePic)
                        tvUserName.text = groupName
                    }

                }

                item.recentChat?.apply {
                    val isSenderIsMe = AuthManager.currentUserId() == senderId
                    val infoMessage = when (infoMessageType) {
                        Constants.InfoType.GroupCreated.name -> {
                            if (isSenderIsMe){
                                "You created this group!"
                            }else{
                                "Someone added you in this group!"
                            }

                        }

                        Constants.InfoType.GroupNameChange.name -> {
                            if (isSenderIsMe){
                                "You change group name !"
                            }else{
                                "Someone change group name !"
                            }
                        }

                        Constants.InfoType.GroupDescChange.name -> {
                            if (isSenderIsMe){
                                "You change group description !"
                            }else{
                                "Someone change group description !"
                            }
                        }

                        Constants.InfoType.GroupPicChange.name -> {
                            if (isSenderIsMe){
                                "You change group profile picture!"
                            }else{
                                "Someone change group profile picture!"
                            }
                        }

                        Constants.InfoType.MemberAdded.name -> {
                            if (isSenderIsMe){
                                "You added someone in this group!"
                            }else{
                                "Someone added someone in this group!"
                            }
                        }

                        Constants.InfoType.MemberRemoved.name -> {
                            if (isSenderIsMe){
                                "You removed someone from this group!"
                            }else {
                                "Someone removed from this group!"
                            }
                        }

                        else -> {
                            ""
                        }
                    }
                    tvLastMessageTime.text = Helper.getTimeForChat(lastMessageTimeInTimeStamp!!)
                    when (lastMessageSeen) {
                        Constants.SeenStatus.Sending.status -> {
                            Glide.with(ivMessageSeenStatus.context)
                                .load(R.drawable.ic_message_sending).into(ivMessageSeenStatus)
                        }

                        Constants.SeenStatus.Send.status -> {
                            Glide.with(ivMessageSeenStatus.context).load(R.drawable.ic_message_sent)
                                .into(ivMessageSeenStatus)
                        }

                        Constants.SeenStatus.Received.status -> {
                            Glide.with(ivMessageSeenStatus.context)
                                .load(R.drawable.ic_message_received).into(ivMessageSeenStatus)
                        }

                        Constants.SeenStatus.MessageSeen.status -> {
                            Glide.with(ivMessageSeenStatus.context).load(R.drawable.ic_message_seen)
                                .into(ivMessageSeenStatus)
                        }

                        else -> {
                            Glide.with(ivMessageSeenStatus.context)
                                .load(R.drawable.ic_message_sending).into(ivMessageSeenStatus)
                        }
                    }


                    val unSeenMessageCount = unSeenMessageCount ?: 0
                    if (unSeenMessageCount == 0) {
                        tvUnSeenMessage.gone()
                        tvLastMessageTime.setTextColor(tvLastMessageTime.context.giveMeColor(R.color.white))
                    } else {
                        tvUnSeenMessage.myShow()
                        tvUnSeenMessage.text = unSeenMessageCount.toString()
                        tvLastMessageTime.setTextColor(tvLastMessageTime.context.giveMeColor(R.color.green))
                    }

                    if (senderId == AuthManager.currentUserId()!!&&infoMessageType==null) {
                            ivMessageSeenStatus.myShow()
                    } else {
                        ivMessageSeenStatus.gone()
                    }

                    tvLastMessage.text =
                        if (lastMessageType == Constants.LastMessageType.Media.type) {
                            ivMedia.myShow()
                            "Media"
                        } else {
                            ivMedia.gone()
                            message ?: infoMessage
                        }
                }

                if (absoluteAdapterPosition == differ.currentList.size - 1) {
                    root.context.apply {
                        root.setMargin(
                            com.intuit.sdp.R.dimen._1sdp,
                            com.intuit.sdp.R.dimen._1sdp,
                            com.intuit.sdp.R.dimen._1sdp,
                            com.intuit.sdp.R.dimen._10sdp
                        )
                    }
                }

                root.setSafeOnClickListener {
                    onItemClick(item)
                }
                root.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SampleRecentChatLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    private fun View.setMargin(start: Int, top: Int, end: Int, bottom: Int) {
        val layoutParams = layoutParams
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            layoutParams.setMargins(
                resources.getDimensionPixelSize(start),
                resources.getDimensionPixelSize(top),
                resources.getDimensionPixelSize(end),
                resources.getDimensionPixelSize(bottom)
            )
            this.layoutParams = layoutParams
        }
    }


}