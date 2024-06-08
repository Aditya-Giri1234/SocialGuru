package com.aditya.socialguru.ui_layer.adapter.profile.friend_circle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.FollowerData
import com.aditya.socialguru.databinding.SampleFollowerListBinding
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide

class FollowerAdapter(val itemClick:(userId:String)->Unit,val onRemoveClick:(userId:String)->Unit) : RecyclerView.Adapter<FollowerAdapter.ViewHolder>(){

    companion object{
        val callback=object : DiffUtil.ItemCallback<FollowerData>(){
            override fun areItemsTheSame(oldItem: FollowerData, newItem: FollowerData): Boolean {
                return oldItem.userId==newItem.userId
            }

            override fun areContentsTheSame(oldItem: FollowerData, newItem: FollowerData): Boolean {
                return oldItem==newItem
            }

        }
    }

    private val differ=AsyncListDiffer(this, callback)

    fun submitList(list: List<FollowerData>){
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding:SampleFollowerListBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(user: User){
            user.apply {
                binding.apply {
                    userProfileImage?.let {
                        Glide.with(ivFollowerProfilePic).load(it).placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(ivFollowerProfilePic)
                    }
                    tvFollowerName.text=userName

                    btnFollow.setSafeOnClickListener {
                        onRemoveClick(user.userId!!)
                    }

                    root.setSafeOnClickListener {
                        itemClick(userId!!)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return  ViewHolder(SampleFollowerListBinding.inflate(LayoutInflater.from(parent.context) ,parent,false
        ))
    }

    override fun getItemCount(): Int {
        return  differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        differ.currentList[position].user?.let { holder.bind(it) }
    }
}