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
import com.example.chatapp.Model.GroupChat;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.MyViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<GroupChat> mGroupChat;

    private FirebaseUser fuser;

    public GroupMessageAdapter(Context mContext, List<GroupChat> mGroupChat) {
        this.mContext = mContext;
        this.mGroupChat = mGroupChat;
        fuser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get the view type and create the right view
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.group_chat_item_right, parent, false);
            return new MyViewHolder(view);
        }
        else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.group_chat_item_left, parent, false);
            return new MyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        GroupChat groupChat = mGroupChat.get(position);

        holder.show_message.setText(groupChat.getMessage());
        setUserName(groupChat ,holder);

//        // Check if it's the last message
//        if (position == mGroupChat.size() - 1) {
//            // Check if the message is seen and se a text
//            if (groupChat.isIsseen()) {
//                holder.txt_seen.setText("Seen");
//            }
//            else {
//                holder.txt_seen.setText("Delivered");
//            }
//        }
//        else {
//            holder.txt_seen.setVisibility(View.GONE);
//        }
    }

    private void setUserName(GroupChat groupChat, MyViewHolder holder) {
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Users")
                .child(groupChat.getSender());
        reference1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                holder.name.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    holder.profile_image.setImageResource(R.mipmap.ic_launcher);
                }
                else {
                    Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mGroupChat.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public ImageView profile_image;
        public TextView name;
        public TextView show_message;
        public TextView txt_seen;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            name = itemView.findViewById(R.id.name);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // If i'm the sender show message on right else show it on left
        if (mGroupChat.get(position).getSender().equals(fuser.getUid())) {
            return  MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }
}