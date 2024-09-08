package com.aditya.socialguru.ui_layer.adapter.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.SampleUserListBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.bumptech.glide.Glide

class UserAdapter(val itemClick: (user: User, view: View) -> Unit) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        private var groupInfo: GroupInfo? = null

    companion object {
        val callback = object : DiffUtil.ItemCallback<FriendCircleData>() {
            override fun areItemsTheSame(
                oldItem: FriendCircleData,
                newItem: FriendCircleData
            ): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(
                oldItem: FriendCircleData,
                newItem: FriendCircleData
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

    private val differ = AsyncListDiffer(this, callback)

    fun submitList(list: List<FriendCircleData>) {
        differ.submitList(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: SampleUserListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: FriendCircleData) {
            data.let {
                binding.apply {
                    data.user?.let {
                        if (it.userProfileImage==null){
                            tvInitial.myShow()
                            ivUserProfileImage.gone()
                            tvInitial.text = it.userName?.get(0).toString()
                            tvInitial.setCircularBackground(Helper.setUserProfileColor(it))
                        }else{
                            tvInitial.gone()
                            ivUserProfileImage.myShow()
                            Glide.with(ivUserProfileImage).load(data.user?.userProfileImage).placeholder(R.drawable.ic_user).error(
                                R.drawable.ic_user
                            ).into(ivUserProfileImage)
                        }

                        tvUserName.text = it.userName
                    }

                    root.setSafeOnClickListener {
                        data.user?.let {user->
                            itemClick(user, it)
                        }
                    }


                    val isThisUserCreator = groupInfo?.creatorId == it.userId
                    if(isThisUserCreator){
                        tvCreator.myShow()
                    } else tvCreator.gone()

                    val isThisUserAdmin = groupInfo?.groupAdmins?.contains(it.userId)
                    if(isThisUserAdmin == true){
                        tvAdmin.myShow()
                    } else tvAdmin.gone()

                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SampleUserListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        differ.currentList[position].let { holder.bind(it) }
    }

    fun setGroupInfo(groupInfo: GroupInfo) {
        this.groupInfo = groupInfo
        notifyDataSetChanged()
    }

}