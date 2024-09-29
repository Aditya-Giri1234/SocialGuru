package com.aditya.socialguru.ui_layer.adapter.profile.friend_circle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.SampleFriendRequestListBinding
import com.aditya.socialguru.databinding.SamplePendingRequestItemBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide

class PendingFriendRequestAdapter(val itemClick:(userId:String)->Unit, val onResult:(userId:String)->Unit) : RecyclerView.Adapter<PendingFriendRequestAdapter.ViewHolder>(){

    companion object{
        val callback=object : DiffUtil.ItemCallback<FriendCircleData>(){
            override fun areItemsTheSame(oldItem: FriendCircleData, newItem: FriendCircleData): Boolean {
                return oldItem.userId==newItem.userId
            }

            override fun areContentsTheSame(oldItem: FriendCircleData, newItem: FriendCircleData): Boolean {
                return oldItem==newItem
            }

        }
    }

    private val differ= AsyncListDiffer(this, callback)

    fun submitList(list: List<FriendCircleData>){
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SamplePendingRequestItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(user: User){
            user.apply {
                binding.apply {
                    if (userProfileImage==null){
                        tvInitial.myShow()
                        ivFriendProfilePic.gone()
                        tvInitial.text = userName?.get(0).toString()
                        tvInitial.setCircularBackground(Helper.setUserProfileColor(user))
                    }else{
                        tvInitial.gone()
                        ivFriendProfilePic.myShow()
                        Glide.with(ivFriendProfilePic).load(userProfileImage).placeholder(R.drawable.ic_user).error(
                            R.drawable.ic_user).into(ivFriendProfilePic)
                    }
                    tvFriendName.text=userName


                    btnWithdraw.setSafeOnClickListener {
                        onResult(user.userId!!)
                    }

                    root.setSafeOnClickListener {
                        itemClick(userId!!)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return  ViewHolder(
            SamplePendingRequestItemBinding.inflate(
                LayoutInflater.from(parent.context) ,parent,false
            ))
    }

    override fun getItemCount(): Int {
        return  differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        differ.currentList[position].user?.let { holder.bind(it) }
    }

    fun getData():List<FriendCircleData> = differ.currentList
}