package com.aditya.socialguru.ui_layer.adapter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.databinding.AddStoryLayoutBinding
import com.aditya.socialguru.databinding.SampleStoriesLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hide
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


class StoryAdapter(
    val onAddStory: () -> Unit,
    val onStoryView: (userStories: UserStories) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val callback = object : DiffUtil.ItemCallback<UserStories>() {
            override fun areItemsTheSame(oldItem: UserStories, newItem: UserStories): Boolean {
                return oldItem.user?.userId == newItem.user?.userId
            }

            override fun areContentsTheSame(oldItem: UserStories, newItem: UserStories): Boolean {
                return oldItem == newItem
            }

        }
    }

    private val tagStory=Constants.LogTag.Story
    private val TYPE_AVAIBLE = 0
    private val TYPE_UPLOAD = 1

    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<UserStories>) {
        differ.submitList(list)
    }

    private inner class AvailableStoryViewHolder(val binding: SampleStoriesLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: UserStories) {
            binding.apply {
                profileImage.setImageResource(
                    R.drawable.person
                )
                tvProfileName.text = data.user?.userName ?: "Unknown"
                tvStoryCount.text = "${data.stories?.size ?: 0} stories"
                data.stories?.get(0)?.let { story ->
                    when (story.storyType) {
                        Constants.StoryType.Image.name, Constants.StoryType.Video.name -> {
                            MyLogger.d(tagStory , msg = "This is media type story !")
                            ivStory.myShow()
                            Glide.with(binding.ivStory.context).load(story.storyUri).addListener(object :
                                RequestListener<Drawable?> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    linearloader.gone()
                                    ivStory.gone()
                                    linearNoImage.myShow()
                                    MyLogger.e(tagStory, msg = "Media pic loaded failed !")
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    MyLogger.i(tagStory , msg = "Media pic loaded!")
                                    linearloader.gone()
                                    ivStory.myShow()
                                    return false
                                }
                            }).into(binding.ivStory)
                        }

                        else -> {
                            MyLogger.d(tagStory , msg = "This is text type story !")
                            ivStory.hide()
                            linearloader.gone()
                            tvStory.myShow()
                            tvStory.apply {
                                setBackgroundColor(story.textBackGroundColor ?: Color.BLACK)
                                typeface = ResourcesCompat.getFont(
                                    this.context,
                                    story.textFontFamily ?: R.font.roboto_medium
                                )
                                text = story.text
                            }
                        }
                    }
                }
                root.setSafeOnClickListener {
                    data.stories?.let {
                        onStoryView(data)
                    }
                }
            }
        }

    }

    private inner class UploadStoryViewHolder(val view: AddStoryLayoutBinding) :
        RecyclerView.ViewHolder(view.root) {
        fun bind(data: UserStories) {
            view.apply {
                root.setOnClickListener {
                    MyLogger.w(msg = "Story Adapter is clicked!")
                }
                ivAddStory.setSafeOnClickListener {
                    onAddStory()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AVAIBLE) {
            val binding = SampleStoriesLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            binding.root.setStartingMargin()
            AvailableStoryViewHolder(binding)
        } else {
            UploadStoryViewHolder(AddStoryLayoutBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AvailableStoryViewHolder -> {
                (holder as AvailableStoryViewHolder).bind(differ.currentList[position])
            }

            is UploadStoryViewHolder -> {
                (holder as UploadStoryViewHolder).bind(differ.currentList[position])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        differ.currentList[position].apply {
            return if (user == null)
                TYPE_UPLOAD
            else
                TYPE_AVAIBLE
        }
    }

    private fun View.setStartingMargin() {
        val layoutParams = layoutParams
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            layoutParams.setMargins(
                context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp),
                0,
                0,
                0
            )
            this.layoutParams = layoutParams
        }
    }


}