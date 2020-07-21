package com.example.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.GroupMessageActivity;
import com.example.chatapp.Model.Group;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.MyViewHolder> {

    private Context mContext;
    private List<Group> mGroups;

    public GroupAdapter(Context mContext, List<Group> mGroups) {
        this.mContext = mContext;
        this.mGroups = mGroups;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.group_chat_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Group group = mGroups.get(position);
        holder.group_title.setText(group.getGroupTitle());

        holder.group_time.setText("");
        holder.last_msg.setText("");

        loadLastMessage(group, holder);

        if ("default".equals(group.getGroupIcon())) {
            holder.group_icon.setImageResource(R.drawable.ic_group_primary);
        } else {
            Glide.with(mContext).load(group.getGroupIcon()).into(holder.group_icon);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, GroupMessageActivity.class);
                intent.putExtra("groupId", group.getGroupId());
                intent.putExtra("groupTitle", group.getGroupTitle());
                mContext.startActivity(intent);
            }
        });
    }

    private void loadLastMessage(Group group, MyViewHolder holder) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(group.getGroupId()).child("Messages").limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String msg = (String) snapshot.child("message").getValue();
                            Long timestamp = (Long) snapshot.child("time").getValue();
                            String sender = (String) snapshot.child("sender").getValue();
                            String type = (String) snapshot.child("type").getValue();

                            // Convert timestamp
                            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                            calendar.setTimeInMillis(timestamp);
                            String dateTime = DateFormat.format("dd/MM/yy hh:mm aa", calendar).toString();

                            holder.group_time.setText(dateTime);

                            DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Users").child(sender);
                            reference1.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    if (user.getId().equals(sender)) {
                                        if (type.equals("image")) {
                                            holder.last_msg.setText("You sent a photo.");
                                        } else if (type.equals("text")) {
                                            holder.last_msg.setText("You: " + msg);
                                        } else {
                                            holder.last_msg.setText("You sent a voice message");
                                        }
                                    }
                                    else {
                                        if (type.equals("image")) {
                                            holder.last_msg.setText(user.getUsername() + ": sent a photo.");
                                        } else if (type.equals("text")) {
                                            holder.last_msg.setText(user.getUsername() + ": " + msg);
                                        } else {
                                            holder.last_msg.setText(user.getUsername() + ": sent a voice message");
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView group_title;
        private ImageView group_icon;
        private TextView last_msg;
        private TextView group_time;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            group_title = itemView.findViewById(R.id.group_title);
            group_icon = itemView.findViewById(R.id.group_icon);
            last_msg = itemView.findViewById(R.id.last_msg);
            group_time = itemView.findViewById(R.id.group_time);
        }

    }
}
