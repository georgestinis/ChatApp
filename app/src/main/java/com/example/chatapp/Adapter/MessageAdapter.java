package com.example.chatapp.Adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private MediaPlayer mediaPlayer;
    private Timer timer;

    private Context mContext;
    private List<Chat> mChat;
    private String imageurl;

    private FirebaseUser firebaseUser;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl) {
        this.mContext = mContext;
        this.mChat = mChat;
        this.imageurl = imageurl;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get the view type and create the right view
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
        }
        else {
            view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
        }
        return new MyViewHolder(view);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    // Method to get each chat message and show it
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Chat chat = mChat.get(position);

        if (imageurl.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            Glide.with(mContext).load(imageurl).into(holder.profile_image);
        }

        if (chat.getType().equals("text")) {
            holder.image_message.setVisibility(View.GONE);
            holder.audio_message.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.VISIBLE);
            holder.show_message.setText(chat.getMessage());
        }
        else if (chat.getType().equals("image")) {
            holder.image_message.setVisibility(View.VISIBLE);
            holder.audio_message.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.GONE);
            if ("default".equals(chat.getMessage())) {
                holder.image_message.setImageResource(R.drawable.ic_image_black);
            }
            else {
                Glide.with(mContext).load(chat.getMessage()).into(holder.image_message);
            }
        }
        else if (chat.getType().equals("audio")) {
            holder.image_message.setVisibility(View.GONE);
            holder.show_message.setVisibility(View.GONE);
            holder.audio_message.setVisibility(View.VISIBLE);
            holder.time_audio.setText(String.format("%2d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(chat.getLength()),
                    TimeUnit.MILLISECONDS.toSeconds(chat.getLength()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(chat.getLength()))
            ));
        }

        holder.audio_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //timer != null &&
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    // timer.cancel();
                    // holder.counter = 0;
                }
                //timer = new Timer();
                mediaPlayer = new MediaPlayer();
                //holder.audio_length.setProgress(holder.counter);
                try {
                    mediaPlayer.setDataSource(chat.getMessage());
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();

//                            TimerTask timerTask = new TimerTask() {
//                                @Override
//                                public void run() {
//                                    holder.counter += (int) (100/(TimeUnit.MILLISECONDS.toSeconds(chat.getLength()) + 2));
//                                    holder.audio_length.setProgress(holder.counter);
//                                    if (holder.counter >= 100){
//                                        timer.cancel();
//                                        holder.counter = 0;
//                                    }
//                                }
//                            };
//                            timer.schedule(timerTask, 0, 1000);
                        }
                    });
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

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
//        private int counter = 0;
        private TextView show_message, txt_seen, time_audio;
        private ImageView profile_image, image_message, play_audio;
        private RelativeLayout audio_message;
        private ProgressBar audio_length;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            image_message = itemView.findViewById(R.id.image_message);
            audio_message = itemView.findViewById(R.id.audio_message);
            audio_length = itemView.findViewById(R.id.audio_length);
            play_audio = itemView.findViewById(R.id.play_audio);
            time_audio = itemView.findViewById(R.id.time_audio);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // If i'm the sender show message on right else show it on left
        if (mChat.get(position).getSender().equals(firebaseUser.getUid())) {
            return  MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }

}
