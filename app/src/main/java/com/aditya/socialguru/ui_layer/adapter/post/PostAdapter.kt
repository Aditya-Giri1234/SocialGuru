package com.aditya.socialguru.ui_layer.adapter.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.SamplePostLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.bumptech.glide.Glide
import com.google.rpc.Help

class PostAdapter(val onClick: OnPostClick , val isIAmCurrentSeeingOtherPost:Boolean=false) :
    RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    companion object {
        private val callback = object : DiffUtil.ItemCallback<UserPostModel>() {
            override fun areItemsTheSame(oldItem: UserPostModel, newItem: UserPostModel): Boolean {
                return oldItem.post?.postId == newItem.post?.postId
            }

            override fun areContentsTheSame(
                oldItem: UserPostModel,
                newItem: UserPostModel
            ): Boolean {
                return oldItem == newItem
            }

        }
    }


    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<UserPostModel>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SamplePostLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(userPost: UserPostModel) {
            userPost.post?.run {
                binding.apply {
                    postType?.let {
                        val postImageVideoModel: List<PostImageVideoModel>? = when (it) {
                            Constants.PostType.OnlyText.name -> {
                                tvPost.myShow()
                                constMedia.gone()
                                linearBottomHeader.context.apply {
                                    linearBottomHeader.setMargin(
                                        com.intuit.sdp.R.dimen._5sdp,
                                        com.intuit.sdp.R.dimen._5sdp,
                                        com.intuit.sdp.R.dimen._5sdp,
                                        com.intuit.sdp.R.dimen._1sdp
                                    )
                                }
                                tvPostBottom.gone()
                                tvPost.text = text
                                null
                            }

                            Constants.PostType.OnlyImage.name -> {
                                tvPostBottom.gone()
                                dotsIndicator.gone()
                                linearBottomHeader.context.apply {
                                    linearBottomHeader.setMargin(
                                        com.intuit.sdp.R.dimen._1sdp,
                                        com.intuit.sdp.R.dimen._5sdp,
                                        com.intuit.sdp.R.dimen._1sdp,
                                        com.intuit.sdp.R.dimen._1sdp
                                    )
                                }

                                listOf(
                                    PostImageVideoModel(
                                        imageUrl, true
                                    )
                                )

                            }

                            Constants.PostType.OnlyVideo.name -> {
                                tvPostBottom.gone()
                                dotsIndicator.gone()
                                linearBottomHeader.context.apply {
                                    linearBottomHeader.setMargin(
                                        com.intuit.sdp.R.dimen._1sdp,
                                        com.intuit.sdp.R.dimen._5sdp,
                                        com.intuit.sdp.R.dimen._1sdp,
                                        com.intuit.sdp.R.dimen._1sdp
                                    )
                                }

                                listOf(
                                    PostImageVideoModel(
                                        videoUrl, false
                                    )
                                )
                            }

                            Constants.PostType.TextAndImage.name -> {
                                tvPostBottom.text = text
                                dotsIndicator.gone()
                                listOf(
                                    PostImageVideoModel(
                                        imageUrl, true
                                    )
                                )
                            }

                            Constants.PostType.TextAndVideo.name -> {
                                tvPostBottom.text = text
                                dotsIndicator.gone()
                                listOf(
                                    PostImageVideoModel(
                                        videoUrl, false
                                    )
                                )
                            }

                            Constants.PostType.ImageAndVideo.name -> {
                                tvPostBottom.gone()
                                linearBottomHeader.context.apply {
                                    linearBottomHeader.setMargin(
                                        com.intuit.sdp.R.dimen._1sdp,
                                        com.intuit.sdp.R.dimen._5sdp,
                                        com.intuit.sdp.R.dimen._1sdp,
                                        com.intuit.sdp.R.dimen._1sdp
                                    )
                                }

                                listOf(
                                    PostImageVideoModel(
                                        imageUrl, true
                                    ),
                                    PostImageVideoModel(
                                        videoUrl, false
                                    )
                                )
                            }

                            Constants.PostType.All.name -> {
                                tvPostBottom.text = text
                                listOf(
                                    PostImageVideoModel(
                                        imageUrl, true
                                    ),
                                    PostImageVideoModel(
                                        videoUrl, false
                                    )
                                )
                            }

                            else -> {
                                null
                            }
                        }

                        if (postImageVideoModel != null) {
                            val viewPagerAdapter = PostImageVideoAdapter(
                                onClick.onImageClick(),
                                onClick.onVideoClick()
                            )
                            viewPagerPost.adapter = viewPagerAdapter
                            dotsIndicator.attachTo(viewPagerPost)

                            viewPagerAdapter.submitList(postImageVideoModel)
                        }

                        tvLike.text = "$likeCount Likes"
                        tvComment.text = "$commentCount Comments"

                        if (isIAmCurrentSeeingOtherPost){
                            ivSetting.gone()
                        }

                        var isLiked =
                            likedUserList?.contains(AuthManager.currentUserId()!!) ?: false

                        // This is for when click like button then result show as soon as possible so that below calculation help to fast calculation
                        MyLogger.w(Constants.LogTag.Post, msg = "Index :- $absoluteAdapterPosition and isLiked $isLiked and liked User list := $likedUserList")
                        val countExceptLoginUser=if (isLiked) (likeCount?.let { it - 1 } ?: 0) else likeCount ?: 0

                        ivLike.setImageResource(if (isLiked) R.drawable.like else R.drawable.not_like)

                        // set time
                        if(postUploadingTimeInTimeStamp!=null){
                            tvPostTime.myShow()
                            tvPostTime.text= Helper.getTimeForPostAndComment(postUploadingTimeInTimeStamp)
                        }else{
                            tvPostTime.gone()
                        }


                        ivLike.setSafeOnClickListener {
                            isLiked = !isLiked
                            val tempCount= if (isLiked) countExceptLoginUser+1 else countExceptLoginUser
                            tvLike.text = "${tempCount} Likes"
                            ivLike.setImageResource(if (isLiked) R.drawable.like else R.drawable.not_like)
                            onClick.onLikeClick(this@run)
                        }

                        ivComment.setSafeOnClickListener {
                            onClick.onCommentClick(postId!!)
                        }

                        ivSend.setSafeOnClickListener {
                            onClick.onSendClick(this@run)
                        }

                        ivSetting.setSafeOnClickListener {
                            onClick.onSettingClick()
                        }

                        viewPagerClickSupport.setOnClickListener {
                            MyLogger.i(
                                Constants.LogTag.Post,
                                msg = "Click on viewPagerClickSupport  !"
                            )
                            onClick.onPostClick(postId!!)
                        }
                        root.setSafeOnClickListener {
                            MyLogger.i(Constants.LogTag.Post, msg = "Click on root !")
                            onClick.onPostClick(postId!!)
                        }

                    }
                }
            }
            userPost.user?.run {
                binding.apply {
                    Glide.with(ivPostUserImage).load(userProfileImage).into(ivPostUserImage)
                    tvPostUserName.text = userName
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SamplePostLayoutBinding.inflate(
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