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
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.databinding.SampleShowMyStatusBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hide
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class MyStoryAdapter(val onItemClick:()->Unit) : RecyclerView.Adapter<MyStoryAdapter.ViewHolder>() {

    companion object {
        val callback = object : DiffUtil.ItemCallback<Stories>() {
            override fun areItemsTheSame(oldItem: Stories, newItem: Stories): Boolean {
                return oldItem.storyId == newItem.storyId
            }

            override fun areContentsTheSame(oldItem: Stories, newItem: Stories): Boolean {
                return oldItem == newItem
            }

        }
    }


    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<Stories>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }


    inner class ViewHolder(val binding: SampleShowMyStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(stories: Stories) {
            when (stories.storyType){
                Constants.StoryType.Image.name,Constants.StoryType.Video.name->{
                    binding.ivStory.myShow()
                    binding.tvStory.gone()
                    Glide.with(binding.ivStory).load(stories.storyUri).addListener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.linearloader.gone()
                            binding.ivStory.gone()
                            binding.linearNoImage.myShow()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.linearloader.gone()
                            return false
                        }
                    }).into(binding.ivStory)
                }
                else->{
                    binding.ivStory.hide()
                    binding.linearloader.gone()
                    binding.tvStory.myShow()
                    binding.tvStory.apply {
                        setBackgroundColor(stories.textBackGroundColor ?: Color.BLACK)
                        typeface = ResourcesCompat.getFont(
                            this.context,
                            stories.textFontFamily ?: R.font.roboto_medium
                        )
                        text = stories.text
                    }
                }
            }

            binding.tvStatusUploadingDuration.text= stories.storyUploadingTimeInTimeStamp?.let {
                Helper.timeDifference(
                    it
                )
            }

            binding.ivStory.setSafeOnClickListener {
                onItemClick()
            }
            binding.tvStory.setSafeOnClickListener {
                onItemClick()
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SampleShowMyStatusBinding.inflate(LayoutInflater.from(parent.context) ,parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
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