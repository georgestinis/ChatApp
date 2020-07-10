package com.example.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private boolean isFriend;
    private int position;

    String theLastMessage;

    public UserAdapter(Context mContext, List<User> mUsers, boolean isFriend) {
        this.mContext = mContext;
        this.mUsers = mUsers;
        this.isFriend = isFriend;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Set users list prototype
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new MyViewHolder(view);
    }

    // Get each user show his username and image, on click open a chat room
    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
        final User user = mUsers.get(position);
        holder.username.setText(user.getUsername());

        if ("default".equals(user.getImageURL())) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }
        if (isFriend) {
            if (user.getStatus().equals("online")) {
                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            }
            else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        }
        else {
            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(holder.getAdapterPosition());
                return false;
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                // Send clicked user's id
                intent.putExtra("userid", user.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull MyViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        public TextView username;
        public ImageView profile_image;
        private ImageView img_on;
        private ImageView img_off;
        private RelativeLayout long_click_menu;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            long_click_menu = itemView.findViewById(R.id.long_click_menu);
            long_click_menu.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

            menu.setHeaderTitle("Select an Option");
            menu.add(Menu.NONE, R.id.archiving, 0 , "Archive this conversation");
            menu.add(Menu.NONE, R.id.deleting, 0 , "Delete this conversation");
            menu.add(Menu.NONE, R.id.del_friend, 0 , "Delete User");

        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

}
