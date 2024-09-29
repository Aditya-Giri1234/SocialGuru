package com.aditya.socialguru.ui_layer.adapter.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.SampleStartChatDialogBinding
import com.aditya.socialguru.databinding.SampleUserStartChatItemBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide

class StartChatAdapter (val itemClick:(user: User)->Unit) : RecyclerView.Adapter<StartChatAdapter.ViewHolder>() {

    private val differ=AsyncListDiffer(this, object : DiffUtil.ItemCallback<FriendCircleData>(){
        override fun areItemsTheSame(
            oldItem: FriendCircleData,
            newItem: FriendCircleData
        ): Boolean {
            return oldItem.userId==newItem.userId
        }

        override fun areContentsTheSame(
            oldItem: FriendCircleData,
            newItem: FriendCircleData
        ): Boolean {
            return oldItem.userId==newItem.userId
        }
    })

    fun submitList(list: List<FriendCircleData>){
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SampleUserStartChatItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(user:User){
            binding.apply {
                user.run{
                    if (userProfileImage==null){
                        tvInitial.myShow()
                        ivUserProfileImage.gone()
                        tvInitial.text = userName?.get(0).toString()
                        tvInitial.setCircularBackground(Helper.setUserProfileColor(this))
                    }else{
                        tvInitial.gone()
                        ivUserProfileImage.myShow()
                        Glide.with(ivUserProfileImage).load(userProfileImage).placeholder(R.drawable.ic_user).error(
                            R.drawable.ic_user
                        ).into(ivUserProfileImage)
                    }
                    tvUserName.text=userName
                    tvBio.text=userBio
                }
                root.setSafeOnClickListener {
                    itemClick(user)
                }

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SampleUserStartChatItemBinding.inflate(LayoutInflater.from(parent.context) , parent,false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        differ.currentList[position].user?.let { holder.bind(it) }
    }


}