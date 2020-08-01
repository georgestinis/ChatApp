package com.example.chatapp.Adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.MyViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private MediaPlayer mediaPlayer;
    private Timer timer;

    private Context mContext;
    private List<GroupChat> mGroupChat;

    private FirebaseUser firebaseUser;

    public GroupMessageAdapter(Context mContext, List<GroupChat> mGroupChat) {
        this.mContext = mContext;
        this.mGroupChat = mGroupChat;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get the view type and create the right view
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.group_chat_item_right, parent, false);
        }
        else {
            view = LayoutInflater.from(mContext).inflate(R.layout.group_chat_item_left, parent, false);
        }
        return new MyViewHolder(view);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        GroupChat groupChat = mGroupChat.get(position);

        if (groupChat.getType().equals("text")) {
            holder.audio_message.setVisibility(View.GONE);
            holder.image_message.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText(groupChat.getMessage());
        }
        else if (groupChat.getType().equals("image")){
            holder.audio_message.setVisibility(View.GONE);
            holder.image_message.setVisibility(View.VISIBLE);
            holder.show_message.setVisibility(View.GONE);
            if ("default".equals(groupChat.getMessage())) {
                holder.image_message.setImageResource(R.drawable.ic_image_black);
            }
            else {
                Glide.with(mContext).load(groupChat.getMessage()).into(holder.image_message);
            }
        }
        else if (groupChat.getType().equals("audio")) {
            holder.image_message.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.GONE);
            holder.audio_message.setVisibility(View.VISIBLE);
            holder.time_audio.setText(String.format("%2d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(groupChat.getLength()),
                    TimeUnit.MILLISECONDS.toSeconds(groupChat.getLength()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(groupChat.getLength()))
            ));
        }

        holder.audio_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(groupChat.getMessage());
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                        }
                    });
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        setUserName(groupChat ,holder);
    }

    private void setUserName(GroupChat groupChat, MyViewHolder holder) {
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Users")
                .child(groupChat.getSender());
        reference1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    holder.name.setText(user.getUsername());
                    if (user.getImageURL().equals("default")) {
                        holder.profile_image.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
                    }
                }
                else {
                    holder.name.setText("User");
                    holder.profile_image.setImageResource(R.mipmap.ic_launcher);
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

        private ImageView profile_image;
        private TextView name, show_message, time_audio;
        private ImageView image_message;
        private RelativeLayout audio_message;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            name = itemView.findViewById(R.id.name);
            image_message = itemView.findViewById(R.id.image_message);
            audio_message = itemView.findViewById(R.id.audio_message);
            time_audio = itemView.findViewById(R.id.time_audio);

        }
    }

    @Override
    public int getItemViewType(int position) {
        // If i'm the sender show message on right else show it on left
        if (mGroupChat.get(position).getSender().equals(firebaseUser.getUid())) {
            return  MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }
}
