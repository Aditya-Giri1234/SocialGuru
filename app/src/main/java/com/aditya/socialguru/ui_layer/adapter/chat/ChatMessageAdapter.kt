package com.aditya.socialguru.ui_layer.adapter.chat

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.databinding.ChatMessageDateHeaderBinding
import com.aditya.socialguru.databinding.SampleReceiverMessageViewBinding
import com.aditya.socialguru.databinding.SampleSenderMessageViewBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.StoryAdapter
import com.aditya.socialguru.ui_layer.adapter.post.PostImageVideoAdapter

class ChatMessageAdapter(val chatMessageOption: ChatMessageOption) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val SENDER_VIEW_TYPE=0
        private val RECEIVER_VIEW_TYPE=1
        private val DATE_HEADER_VIEW_TYPE=2

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId==newItem.messageId
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId==newItem.messageId
        }
    })

    fun submitList(list: List<Message>){
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class SenderMessageViewHolder(val binding:SampleSenderMessageViewBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){

            binding.apply {
                message.apply {
                    val postImageVideoModel: List<PostImageVideoModel>? = when (chatType) {
                        Constants.PostType.OnlyText.name -> {
                            constMedia.gone()
                            null
                        }

                        Constants.PostType.OnlyImage.name -> {
                            dotsIndicator.gone()
                            tvMessage.gone()

                            listOf(
                                PostImageVideoModel(
                                    imageUri, true
                                )
                            )

                        }

                        Constants.PostType.OnlyVideo.name -> {
                            dotsIndicator.gone()
                            tvMessage.gone()

                            listOf(
                                PostImageVideoModel(
                                    videoUri, false
                                )
                            )
                        }

                        Constants.PostType.TextAndImage.name -> {
                            dotsIndicator.gone()
                            listOf(
                                PostImageVideoModel(
                                    imageUri, true
                                )
                            )
                        }

                        Constants.PostType.TextAndVideo.name -> {
                            dotsIndicator.gone()
                            listOf(
                                PostImageVideoModel(
                                    videoUri, false
                                )
                            )
                        }

                        Constants.PostType.ImageAndVideo.name -> {
                            tvMessage.gone()

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
                    tvMessage.text=text
                    tvTime.text= Helper.getTimeForChat(messageSentTimeInTimeStamp!!)
                    when(seenStatus){
                        Constants.SeenStatus.Sending.status->{
                            ivMessageSeenStatus.setImageResource(R.drawable.ic_message_sending)
                        }
                        Constants.SeenStatus.Send.status->{
                            ivMessageSeenStatus.setImageResource(R.drawable.ic_message_sent)
                        }
                        Constants.SeenStatus.Received.status->{
                            ivMessageSeenStatus.setImageResource(R.drawable.ic_message_received)
                        }
                        Constants.SeenStatus.MessageSeen.status->{
                            ivMessageSeenStatus.setImageResource(R.drawable.ic_message_received)
                            ivMessageSeenStatus.setColorFilter(ContextCompat.getColor(ivMessageSeenStatus.context, R.color.yellow), android.graphics.PorterDuff.Mode.SRC_IN)
                        }
                    }
                }
            }

        }
    }
    inner class ReceiverMessageViewHolder(val binding:SampleReceiverMessageViewBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){

            binding.apply {
                message.apply {
                    val postImageVideoModel: List<PostImageVideoModel>? = when (chatType) {
                        Constants.PostType.OnlyText.name -> {
                            constMedia.gone()
                            null
                        }

                        Constants.PostType.OnlyImage.name -> {
                            dotsIndicator.gone()
                            tvMessage.gone()

                            listOf(
                                PostImageVideoModel(
                                    imageUri, true
                                )
                            )

                        }

                        Constants.PostType.OnlyVideo.name -> {
                            dotsIndicator.gone()
                            tvMessage.gone()

                            listOf(
                                PostImageVideoModel(
                                    videoUri, false
                                )
                            )
                        }

                        Constants.PostType.TextAndImage.name -> {
                            dotsIndicator.gone()
                            listOf(
                                PostImageVideoModel(
                                    imageUri, true
                                )
                            )
                        }

                        Constants.PostType.TextAndVideo.name -> {
                            dotsIndicator.gone()
                            listOf(
                                PostImageVideoModel(
                                    videoUri, false
                                )
                            )
                        }

                        Constants.PostType.ImageAndVideo.name -> {
                            tvMessage.gone()

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
                    tvMessage.text=text
                    tvTime.text= Helper.getTimeForChat(messageSentTimeInTimeStamp!!)
                }
            }

        }
    }
    inner class DateHeaderViewHolder(val binding:ChatMessageDateHeaderBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){
            binding.apply {
                message.apply {
                    tvDateHeader.text=text
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message=differ.currentList[position]
        val type=message.messageType!!
        return if (type==Constants.MessageType.Chat.type){
            if (message.senderId!! == AuthManager.currentUserId()!!){
                SENDER_VIEW_TYPE
            }else{
                RECEIVER_VIEW_TYPE
            }
        }else{
            DATE_HEADER_VIEW_TYPE
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            SENDER_VIEW_TYPE->{
                SenderMessageViewHolder(SampleSenderMessageViewBinding.inflate(LayoutInflater.from(parent.context) ,parent,false))
            }
            RECEIVER_VIEW_TYPE->{
                ReceiverMessageViewHolder(SampleReceiverMessageViewBinding.inflate(LayoutInflater.from(parent.context) ,parent,false))
            }
            DATE_HEADER_VIEW_TYPE->{
                DateHeaderViewHolder(ChatMessageDateHeaderBinding.inflate(LayoutInflater.from(parent.context) ,parent,false))
            }
            else->{
                SenderMessageViewHolder(SampleSenderMessageViewBinding.inflate(LayoutInflater.from(parent.context) ,parent,false))
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        differ.currentList[position]?.let {
            when (holder) {
                is SenderMessageViewHolder -> {
                    (holder as SenderMessageViewHolder).bind(it)
                }

                is ReceiverMessageViewHolder -> {
                    (holder as ReceiverMessageViewHolder).bind(it)
                }
                is DateHeaderViewHolder ->{
                    (holder as DateHeaderViewHolder).bind(it)
                }
            }
        }
    }

}