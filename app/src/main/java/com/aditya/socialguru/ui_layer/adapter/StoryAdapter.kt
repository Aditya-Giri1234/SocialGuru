package com.aditya.socialguru.ui_layer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Stories
import com.aditya.socialguru.databinding.AddStoryLayoutBinding
import com.aditya.socialguru.databinding.SampleStoriesLayoutBinding
import com.aditya.socialguru.domain_layer.helper.Constants


class StoryAdapter(val onAddStory:()->Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val callback = object : DiffUtil.ItemCallback<Stories>() {
            override fun areItemsTheSame(oldItem: Stories, newItem: Stories): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Stories, newItem: Stories): Boolean {
                return oldItem == newItem
            }

        }
    }

    private val TYPE_AVAIBLE = 0
    private val TYPE_UPLOAD = 1

    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<Stories>) = differ.submitList(list)

    private inner class AvailableStoryViewHolder(val view: SampleStoriesLayoutBinding) :
        RecyclerView.ViewHolder(view.root) {

            fun bind(data:Stories){
                view.apply {
                    profileImage.setImageResource(
                        R.drawable.person
                    )
                    tvProfileName.text = data.userName
                    tvStoryCount.text = "${data.storyCount} stories"
                }
            }

    }

    private inner class UploadStoryViewHolder(val view: AddStoryLayoutBinding) :
        RecyclerView.ViewHolder(view.root) {
        fun bind(data:Stories){
            view.apply {
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
            return if (type == Constants.StoryType.UploadStory)
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