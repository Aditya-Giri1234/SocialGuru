package com.aditya.socialguru.ui_layer.adapter.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.databinding.ChatMessageDateHeaderBinding
import com.aditya.socialguru.databinding.SampleGroupInfoItemBinding
import com.aditya.socialguru.databinding.SampleReceiverMessageViewBinding
import com.aditya.socialguru.databinding.SampleSenderMessageViewBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.InfoType
import com.aditya.socialguru.domain_layer.helper.Constants.MessageType
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.PostImageVideoAdapter
import com.bumptech.glide.Glide
import kotlin.system.measureTimeMillis

class GroupChatAdapter(val chatMessageOption: ChatMessageOption) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val SENDER_VIEW_TYPE = 0
    private val RECEIVER_VIEW_TYPE = 1
    private val DATE_HEADER_VIEW_TYPE = 2
    private val INFO_VIEW_TYPE = 3
    private val tagChat = Constants.LogTag.Chats
    private var user: User? = null
    private var userDetails: Map<String, User> = emptyMap()

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<GroupMessage>() {
        override fun areItemsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
            return oldItem == newItem
        }
    })

    fun submitList(list: List<GroupMessage>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    fun submitUser(user: Map<String, User>) {
        this.userDetails = user
        notifyDataSetChanged()
    }

    inner class SenderMessageViewHolder(val binding: SampleSenderMessageViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            binding.apply {
                message.apply {
                    val time = measureTimeMillis {
                        val postImageVideoModel: List<PostImageVideoModel>? = when (chatType) {
                            Constants.PostType.OnlyText.name -> {
                                tvMessage.myShow()
                                constMedia.gone()
                                null
                            }

                            Constants.PostType.OnlyImage.name -> {
                                dotsIndicator.gone()
                                tvMessage.gone()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    )
                                )

                            }

                            Constants.PostType.OnlyVideo.name -> {
                                dotsIndicator.gone()
                                tvMessage.gone()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            Constants.PostType.TextAndImage.name -> {
                                dotsIndicator.gone()
                                constMedia.myShow()
                                tvMessage.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    )
                                )
                            }

                            Constants.PostType.TextAndVideo.name -> {
                                dotsIndicator.gone()
                                constMedia.myShow()
                                tvMessage.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            Constants.PostType.ImageAndVideo.name -> {
                                tvMessage.gone()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    ),
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            Constants.PostType.All.name -> {
                                constMedia.myShow()
                                tvMessage.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    ),
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            else -> {
                                constMedia.gone()
                                tvMessage.myShow()
                                null
                            }
                        }

                        if (postImageVideoModel != null) {
                            val viewPagerAdapter = PostImageVideoAdapter(
                                chatMessageOption.onImageClick(),
                                chatMessageOption.onVideoClick()
                            )
                            viewPagerChat.adapter = viewPagerAdapter
                            dotsIndicator.attachTo(viewPagerChat)

                            viewPagerAdapter.submitList(postImageVideoModel)
                        }
                        tvMessage.text = text
                        tvTime.text = Helper.getTimeForChat(messageSentTimeInTimeStamp!!)

                        when (seenStatus) {
                            Constants.SeenStatus.Sending.status -> {
                                Glide.with(ivMessageSeenStatus.context)
                                    .load(R.drawable.ic_message_sending).into(ivMessageSeenStatus)
                            }

                            Constants.SeenStatus.Send.status -> {
                                Glide.with(ivMessageSeenStatus.context)
                                    .load(R.drawable.ic_message_sent)
                                    .into(ivMessageSeenStatus)
                            }

                            Constants.SeenStatus.Received.status -> {
                                Glide.with(ivMessageSeenStatus.context)
                                    .load(R.drawable.ic_message_received).into(ivMessageSeenStatus)
                            }

                            Constants.SeenStatus.MessageSeen.status -> {
                                Glide.with(ivMessageSeenStatus.context)
                                    .load(R.drawable.ic_message_seen)
                                    .into(ivMessageSeenStatus)
                            }
                        }

                        //This help to recalculate text view width which is current text base
                        tvMessage.requestLayout()
                    }

//                    MyLogger.v(tagChat, msg = "${message.text}  , Time taken to set is :-> $time ")

                    root.setSafeOnClickListener {
                        chatMessageOption.onMessageClick(this)
                    }

                    root.setOnLongClickListener {
                        chatMessageOption.onLongMessageClick(this)
                        true
                    }
                }
            }
        }
    }

    inner class ReceiverMessageViewHolder(val binding: SampleReceiverMessageViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {

            binding.apply {
                message.apply {
                    val time = measureTimeMillis {
                        val postImageVideoModel: List<PostImageVideoModel>? = when (chatType) {
                            Constants.PostType.OnlyText.name -> {
                                tvMessage.myShow()
                                constMedia.gone()
                                null
                            }

                            Constants.PostType.OnlyImage.name -> {
                                dotsIndicator.gone()
                                tvMessage.gone()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    )
                                )

                            }

                            Constants.PostType.OnlyVideo.name -> {
                                dotsIndicator.gone()
                                tvMessage.gone()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            Constants.PostType.TextAndImage.name -> {
                                dotsIndicator.gone()
                                constMedia.myShow()
                                tvMessage.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    )
                                )
                            }

                            Constants.PostType.TextAndVideo.name -> {
                                dotsIndicator.gone()
                                constMedia.myShow()
                                tvMessage.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            Constants.PostType.ImageAndVideo.name -> {
                                tvMessage.gone()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    ),
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            Constants.PostType.All.name -> {
                                tvMessage.myShow()
                                constMedia.myShow()
                                listOf(
                                    PostImageVideoModel(
                                        imageUri, true
                                    ),
                                    PostImageVideoModel(
                                        videoUri, false
                                    )
                                )
                            }

                            else -> {
                                constMedia.gone()
                                tvMessage.myShow()
                                null
                            }
                        }

                        if (postImageVideoModel != null) {
                            val viewPagerAdapter = PostImageVideoAdapter(
                                chatMessageOption.onImageClick(),
                                chatMessageOption.onVideoClick()
                            )
                            viewPagerChat.adapter = viewPagerAdapter
                            dotsIndicator.attachTo(viewPagerChat)

                            viewPagerAdapter.submitList(postImageVideoModel)
                        }
                        tvMessage.text = text
                        tvTime.text = Helper.getTimeForChat(messageSentTimeInTimeStamp!!)

                        user?.let {
                            it.userProfileImage?.let { profilePic ->
                                Glide.with(ivProfileImage.context).load(profilePic)
                                    .placeholder(R.drawable.ic_user).error(R.drawable.ic_user)
                                    .into(ivProfileImage)
                            }
                        } ?: run {
                            Glide.with(ivProfileImage.context).load(R.drawable.ic_user)
                                .into(ivProfileImage)
                        }

                        //This help to recalculate text view width which is current text base
                        tvMessage.requestLayout()
                    }

//                    MyLogger.v(tagChat, msg = "${message.text}  , Time taken to set is :-> $time ")


                    ivProfileImage.setSafeOnClickListener {
                        chatMessageOption.onProfileClick()
                    }
                    root.setSafeOnClickListener {
                        chatMessageOption.onMessageClick(this)
                    }
                    root.setOnLongClickListener {
                        chatMessageOption.onLongMessageClick(this)
                        true
                    }
                }
            }

        }
    }

    inner class InfoMessageViewHolder(val binding: SampleGroupInfoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            binding.apply {
                val isSenderIsMe = AuthManager.currentUserId() == message.senderId
                val message = when (message.infoMessageType) {
                    InfoType.GroupCreated.name -> {
                        val prefix =
                            if (isSenderIsMe) "You" else userDetails[message.senderId]?.userName
                        val suffix = " created this group"
                        "$prefix$suffix"
                    }

                    InfoType.GroupNameChange.name -> {
                        val prefix =
                            if (isSenderIsMe) "You" else userDetails[message.senderId]?.userName
                        val suffix = " changed group name to ${message.text}"
                        "$prefix$suffix"
                    }

                    InfoType.GroupDescChange.name -> {
                        val prefix =
                            if (isSenderIsMe) "You" else userDetails[message.senderId]?.userName
                        val suffix = " changed group description to ${message.text}"
                        "$prefix$suffix"
                    }

                    InfoType.GroupPicChange.name -> {
                        val prefix =
                            if (isSenderIsMe) "You" else userDetails[message.senderId]?.userName
                        val suffix = " changed group profile picture !"
                        "$prefix$suffix"
                    }

                    InfoType.MemberAdded.name -> {
                        val prefix =
                            if (isSenderIsMe) "You" else userDetails[message.senderId]?.userName
                        val suffix = " added ${message.text} to group !"
                        "$prefix$suffix"
                    }

                    InfoType.MemberRemoved.name -> {
                        val prefix =
                            if (isSenderIsMe) "You" else userDetails[message.senderId]?.userName
                        val suffix = " removed ${message.text} from group!"
                        "$prefix$suffix"
                    }

                    else -> {
                        ""
                    }
                }

                tvInfoMessage.text = message
            }
        }
    }

    inner class DateHeaderViewHolder(val binding: ChatMessageDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            binding.apply {
                message.apply {
                    tvDateHeader.text = text
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = differ.currentList[position]
        val type = message.messageType!!
        return when (type) {
            MessageType.Chat.type -> {
                if (message.senderId!! == AuthManager.currentUserId()!!) {
                    SENDER_VIEW_TYPE
                } else {
                    RECEIVER_VIEW_TYPE
                }
            }

            MessageType.Info.type -> {
                INFO_VIEW_TYPE
            }

            else -> {
                DATE_HEADER_VIEW_TYPE
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SENDER_VIEW_TYPE -> {
                SenderMessageViewHolder(
                    SampleSenderMessageViewBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }

            RECEIVER_VIEW_TYPE -> {
                ReceiverMessageViewHolder(
                    SampleReceiverMessageViewBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }

            DATE_HEADER_VIEW_TYPE -> {
                DateHeaderViewHolder(
                    ChatMessageDateHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            INFO_VIEW_TYPE -> {
                InfoMessageViewHolder(
                    SampleGroupInfoItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                SenderMessageViewHolder(
                    SampleSenderMessageViewBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        differ.currentList[position].let {
            when (holder) {
                is SenderMessageViewHolder -> {
                    (holder as SenderMessageViewHolder).bind(it)
                }

                is ReceiverMessageViewHolder -> {
                    (holder as ReceiverMessageViewHolder).bind(it)
                }

                is DateHeaderViewHolder -> {
                    (holder as DateHeaderViewHolder).bind(it)
                }

                is InfoMessageViewHolder -> {
                    (holder as InfoMessageViewHolder).bind(it)
                }
            }
        }
    }


    fun findMessageIndex(message: GroupMessage) = differ.currentList.indexOf(message)
    fun giveMeSecondLastMessage(): GroupMessage =
        differ.currentList[differ.currentList.size - 2].let {
            if (it.messageType == Constants.MessageType.DateHeader.type) {
                differ.currentList[differ.currentList.size - 3]
            } else {
                it
            }
        }

}