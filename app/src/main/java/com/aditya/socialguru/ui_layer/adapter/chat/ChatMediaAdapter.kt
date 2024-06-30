package com.aditya.socialguru.ui_layer.adapter.chat

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.data_layer.model.chat.ChatMediaData
import com.aditya.socialguru.databinding.SampleMediaDateItemBinding
import com.aditya.socialguru.databinding.SampleMediaItemBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class ChatMediaAdapter(
    val onImageClick: (uri: Uri) -> Unit,
    val onVideoClick: (uri: Uri) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {



    companion object {
        val MediaView: Int = 0
        val DateView: Int = 1
        private val callback = object : DiffUtil.ItemCallback<ChatMediaData>() {
            override fun areItemsTheSame(
                oldItem: ChatMediaData,
                newItem: ChatMediaData
            ): Boolean {
                return oldItem.mediaId == newItem.mediaId
            }

            override fun areContentsTheSame(
                oldItem: ChatMediaData,
                newItem: ChatMediaData
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

    private var differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<ChatMediaData>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class MediaViewHolder(val binding: SampleMediaItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: ChatMediaData) {
            binding.apply {
                ivVideoPost.setSafeOnClickListener {
                    data.mediaUri?.let {
                        onImageClick(it.convertParseUri())
                    }
                }
                constVideo.setSafeOnClickListener {
                    data.mediaUri?.let {
                        onVideoClick(it.convertParseUri())
                    }
                }
                Glide.with(ivVideoPost).load(data.mediaUri ?: "")
                    .addListener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            MyLogger.e(Constants.LogTag.Post, msg = "${e?.message}")
                            linearNoImage.myShow()
                            linearloader.gone()
                            ivVideoPost.gone()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            linearloader.gone()
                            constVideo.isGone = data.isImage ?: false
                            return false
                        }
                    }).into(ivVideoPost)
            }
        }
    }

    inner class DateViewHolder(val binding: SampleMediaDateItemBinding) : RecyclerView.ViewHolder
        (binding.root) {
        fun bind(data: ChatMediaData) {
            binding.tvDate.text = data.date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MediaView -> {
                MediaViewHolder(
                    SampleMediaItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            DateView -> {
                DateViewHolder(
                    SampleMediaDateItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            else -> {
                MediaViewHolder(
                    SampleMediaItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is MediaViewHolder->{
                (holder as MediaViewHolder).bind(differ.currentList[position])
            }
            is DateViewHolder->{
                (holder as DateViewHolder).bind(differ.currentList[position])
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (differ.currentList[position].mediaUri != null) {
            MediaView
        } else {
            DateView
        }
    }
}