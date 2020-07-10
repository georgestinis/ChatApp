package com.example.chatapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<Chat> mChat;
    private String imageurl;

    private FirebaseUser fuser;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl) {
        this.mContext = mContext;
        this.mChat = mChat;
        this.imageurl = imageurl;
        fuser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get the view type and create the right view
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new MyViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new MyViewHolder(view);
        }
    }

    // Method to get each chat message and show it
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Chat chat = mChat.get(position);

        holder.show_message.setText(chat.getMessage());

        if (imageurl.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            Glide.with(mContext).load(imageurl).into(holder.profile_image);
        }

        if (chat.getType().equals("text")) {
            holder.image_message.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText(chat.getMessage());
        }
        else if (chat.getType().equals("image")){
            holder.image_message.setVisibility(View.VISIBLE);
            holder.show_message.setVisibility(View.GONE);
            if ("default".equals(chat.getMessage())) {
                holder.image_message.setImageResource(R.drawable.ic_image_black);
            }
            else {
                Glide.with(mContext).load(chat.getMessage()).into(holder.image_message);
            }
        }

        // Check if it's the last message
        if (position == mChat.size() - 1) {
            // Check if the message is seen and se a text
            if (chat.isIsseen()) {
                holder.txt_seen.setText("Seen");
            }
            else {
                holder.txt_seen.setText("Delivered");
            }
        }
        else {
            holder.txt_seen.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView show_message;
        public ImageView profile_image;
        public TextView txt_seen;
        public ImageView image_message;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            image_message = itemView.findViewById(R.id.image_message);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // If i'm the sender show message on right else show it on left
        if (mChat.get(position).getSender().equals(fuser.getUid())) {
            return  MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }
}
