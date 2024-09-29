package com.aditya.socialguru.ui_layer.adapter.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.SampleGroupMemberItemBinding
import com.aditya.socialguru.databinding.SampleUserStartChatItemBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide

class GroupMemberChatAdapter : RecyclerView.Adapter<GroupMemberChatAdapter.ViewHolder>() {

    private val differ= AsyncListDiffer(this, object : DiffUtil.ItemCallback<User>(){
        override fun areItemsTheSame(
            oldItem: User,
            newItem: User
        ): Boolean {
            return oldItem.userId==newItem.userId
        }

        override fun areContentsTheSame(
            oldItem: User,
            newItem: User
        ): Boolean {
            return oldItem.userId==newItem.userId
        }
    })

    fun submitList(list: List<User>){
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SampleGroupMemberItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(user: User){
            binding.apply {
                user.run{
                    if (user.userProfileImage==null){
                        tvInitial.myShow()
                        tvInitial.text = user.userName?.get(0).toString()
                        tvInitial.setCircularBackground(Helper.setUserProfileColor(user))
                    }else{
                        tvInitial.gone()
                        Glide.with(ivProfileImage).load(userProfileImage).placeholder(R.drawable.ic_user).error(
                            R.drawable.ic_user).into(ivProfileImage)
                    }

                    tvMemberName.text=userName
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SampleGroupMemberItemBinding.inflate(LayoutInflater.from(parent.context) , parent,false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        differ.currentList[position].let { holder.bind(it) }
    }


}