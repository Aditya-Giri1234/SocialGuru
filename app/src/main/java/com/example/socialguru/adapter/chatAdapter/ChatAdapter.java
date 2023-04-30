package com.example.socialguru.adapter.chatAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.model.ChatModel;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter{
    ArrayList<ChatModel> chatModels;
    Context context;
    String receiverId;

    int SENDER_VIEW_TYPE=1;
    int RECEIVER_VIEW_TYPE=2;
    public ChatAdapter(ArrayList<ChatModel> chatModels, Context context, String receiverId) {
        this.chatModels = chatModels;
        this.context = context;
        this.receiverId = receiverId;
    }

    public ChatAdapter(ArrayList<ChatModel> chatModels, Context context) {
        this.chatModels = chatModels;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==SENDER_VIEW_TYPE){
            View view= LayoutInflater.from(context).inflate(R.layout.chat_sender_sample,parent,false);
            return  new SenderViewHolder(view);
        }

        else{
            View view= LayoutInflater.from(context).inflate(R.layout.chat_receiver_sample,parent,false);
            return  new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatModel chatModel=chatModels.get(position);

        holder.itemView.setOnLongClickListener(view->{

            new AlertDialog.Builder(context).setTitle("Delete").setMessage("Are you sure you want to delete this message ?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FirebaseDatabase database=FirebaseDatabase.getInstance();
                    String senderRoom=FirebaseAuth.getInstance().getUid()+receiverId;
                    database.getReference().child("Chats").child(senderRoom).child(chatModel.getMessageId()).removeValue();

                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
            return  true;
        });

        if(holder.getClass()==SenderViewHolder.class){
            ((SenderViewHolder)holder).senderText.setText(chatModel.getMessage());
            ((SenderViewHolder)holder).senderTime.setText(TimeAgo.using(chatModel.getLastSeen()));

        }
        else{
            ((ReceiverViewHolder)holder).receiverText.setText(chatModel.getMessage());
            ((ReceiverViewHolder)holder).receiverTime.setText(TimeAgo.using(chatModel.getLastSeen()));
        }
    }

    @Override
    public int getItemCount() {
        return chatModels.size();
    }

    @Override
    public int getItemViewType(int position) {

        if(chatModels.get(position).getuId().equals(FirebaseAuth.getInstance().getUid())){
            return SENDER_VIEW_TYPE;
        }
        else
            return RECEIVER_VIEW_TYPE;
    }

    public  class ReceiverViewHolder extends RecyclerView.ViewHolder{

        TextView receiverText,receiverTime;
        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverText=itemView.findViewById(R.id.receiverText);
            receiverTime=itemView.findViewById(R.id.receiverTime);
        }
    }

    public class SenderViewHolder extends RecyclerView.ViewHolder{

        TextView senderText,senderTime;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText=itemView.findViewById(R.id.senderText);
            senderTime=itemView.findViewById(R.id.senderTime);
        }
    }
}
