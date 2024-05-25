package com.aditya.socialguru.ui_layer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.databinding.AddStoryLayoutBinding
import com.aditya.socialguru.databinding.SampleStoriesLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger


class StoryAdapter(val onAddStory:()->Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    private val TYPE_AVAIBLE = 0
    private val TYPE_UPLOAD = 1

    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<UserStories>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    private inner class AvailableStoryViewHolder(val view: SampleStoriesLayoutBinding) :
        RecyclerView.ViewHolder(view.root) {

            fun bind(data: UserStories){
                view.apply {
                    profileImage.setImageResource(
                        R.drawable.person
                    )
                    tvProfileName.text = data.user?.userName ?: "Unknown"
                    tvStoryCount.text = "${data.stories?.size ?: 0} stories"
                }
            }

    }

    private inner class UploadStoryViewHolder(val view: AddStoryLayoutBinding) :
        RecyclerView.ViewHolder(view.root) {
        fun bind(data: UserStories){
            view.apply {
                root.setOnClickListener {
                    MyLogger.w(msg = "Story Adapter is clicked!")
                }
                ivAddStory.setOnClickListener {
                    onAddStory()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AVAIBLE) {
            val binding = SampleStoriesLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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