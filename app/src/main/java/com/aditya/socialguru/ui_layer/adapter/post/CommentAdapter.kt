package com.aditya.socialguru.ui_layer.adapter.post

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Comment
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.databinding.SampleCommentLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.bumptech.glide.Glide
import kotlin.system.measureTimeMillis

class CommentAdapter(val chatMessageOption: ChatMessageOption) :
    RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private val tagComment = Constants.LogTag.Comment
    private var userDetails: Map<String, User> = emptyMap()
    private var userProfileColor: Map<String, Int> = emptyMap()

    private val differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    })

    fun submitList(list: List<Comment>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    fun submitUser(user: Map<String, User>) {
        this.userDetails = user
        userProfileColor = Helper.setUserProfileColor(user)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SampleCommentLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Comment) {
            binding.apply {
                message.apply {
                    val time = measureTimeMillis {
                        val postImageVideoModel: List<PostImageVideoModel>? = when (commentType) {
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
                        tvTime.text =
                            Helper.getTimeForPostAndComment(commentUploadingTimeInTimestamp!!)
                        val userName = userDetails[userId]?.userName ?: userName
                        tvSenderName.text = if (userId == AuthManager.currentUserId()!!){
                                "~ $userName (You) "
                            } else {
                                "~ $userName"
                            }
                        tvInitial.text = userName?.first().toString()


                        if (userDetails[userId]?.userProfileImage != null) {
                            Glide.with(ivProfileImage.context)
                                .load(userDetails[userId]?.userProfileImage)
                                .error(R.drawable.ic_user)
                                .placeholder(R.drawable.ic_user).into(ivProfileImage)
                        } else {
                            Glide.with(ivProfileImage.context).load(R.drawable.ic_user)
                                .into(ivProfileImage)
                        }

                        when{
                            userDetails[userId]==null ->{
                                // profile pic available then show
                                ivProfileImage.myShow()
                                tvInitial.gone()
                            }
                            userDetails[userId]?.userProfileImage==null ->{
                                // Profile Pic not available show initial
                                ivProfileImage.gone()
                                tvInitial.myShow()
                            }
                            else ->{
                                // profile pic available then show
                                ivProfileImage.myShow()
                                tvInitial.gone()
                            }
                        }

                        userProfileColor[userId]?.let {
                            tvSenderName.setTextColor(it)
                            tvInitial.setCircularBackground(it)
                        } ?: run {
                            tvSenderName.setTextColor(Color.WHITE)
                            tvInitial.setCircularBackground(Color.GREEN)
                        }

                        //This help to recalculate text view width which is current text base
                        tvMessage.requestLayout()
                    }

//                    MyLogger.v(tagChat, msg = "${message.text}  , Time taken to set is :-> $time ")

                    ivProfileImage.setSafeOnClickListener {
                        chatMessageOption.onMessageClick(this)
                    }

                    tvSenderName.setSafeOnClickListener {
                        chatMessageOption.onMessageClick(this)
                    }

                    root.setOnLongClickListener {
                        chatMessageOption.onLongMessageClick(this)
                        true
                    }
                    viewPagerChat.setOnLongClickListener {
                        chatMessageOption.onLongMessageClick(this)
                        true
                    }
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentAdapter.ViewHolder {
        return ViewHolder(
            SampleCommentLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: CommentAdapter.ViewHolder, position: Int) {
        differ.currentList[position].let {
            holder.bind(it)
        }
    }



}