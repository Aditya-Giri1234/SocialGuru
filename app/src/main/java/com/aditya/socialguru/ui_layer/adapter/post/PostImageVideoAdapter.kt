package com.aditya.socialguru.ui_layer.adapter.post

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.databinding.SampleImageVideoPostBinding
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

class PostImageVideoAdapter(
    val onImageClick: (uri: Uri) -> Unit,
    val onVideoClick: (uri: Uri) -> Unit
) : RecyclerView.Adapter<PostImageVideoAdapter.ViewHolder>() {

    companion object {
        private val callback = object : DiffUtil.ItemCallback<PostImageVideoModel>() {
            override fun areItemsTheSame(
                oldItem: PostImageVideoModel,
                newItem: PostImageVideoModel
            ): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(
                oldItem: PostImageVideoModel,
                newItem: PostImageVideoModel
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

    private var differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<PostImageVideoModel>) {
        differ.submitList(list)
    }

    inner class ViewHolder(val binding: SampleImageVideoPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: PostImageVideoModel) {
            binding.apply {
                ivVideoPost.setSafeOnClickListener {
                    data.url?.let {
                        onImageClick(it.convertParseUri())
                    }
                }
                constVideo.setSafeOnClickListener {
                    data.url?.let {
                        onVideoClick(it.convertParseUri())
                    }
                }
                Glide.with(ivVideoPost).load(data.url ?: "")
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
                            constVideo.isGone = data.isImage
                            return false
                        }
                    }).into(ivVideoPost)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SampleImageVideoPostBinding.inflate(
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
}