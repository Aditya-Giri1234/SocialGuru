package com.aditya.socialguru.ui_layer.adapter.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.databinding.SampleImageVideoPostBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide

class PostImageVideoAdapter(val onImageClick:()->Unit,val onVideoClick:()->Unit) : RecyclerView.Adapter<PostImageVideoAdapter.ViewHolder>() {

    companion object{
        private val callback=object : DiffUtil.ItemCallback<PostImageVideoModel>(){
            override fun areItemsTheSame(oldItem: PostImageVideoModel, newItem: PostImageVideoModel): Boolean {
                return oldItem.url==newItem.url
            }

            override fun areContentsTheSame(oldItem: PostImageVideoModel, newItem: PostImageVideoModel): Boolean {
                return oldItem==newItem
            }

        }
    }
    private var differ=AsyncListDiffer(this, callback)

    fun submitList(list:List<PostImageVideoModel>){
        differ.submitList(list)
    }

    inner class ViewHolder(val binding:SampleImageVideoPostBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(data:PostImageVideoModel){
            if (data.isImage){
                binding.constVideo.gone()
            }
            binding.ivVideoPost.setSafeOnClickListener {
                onImageClick()
            }
            binding.constVideo.setSafeOnClickListener {
                onVideoClick()
            }
            Glide.with(binding.ivVideoPost).load(data.url).into(binding.ivVideoPost)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SampleImageVideoPostBinding.inflate(LayoutInflater.from(parent.context) ,parent,false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }
}