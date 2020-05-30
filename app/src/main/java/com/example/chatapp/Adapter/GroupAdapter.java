package com.example.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
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
import com.example.chatapp.R;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.MyViewHolder>{

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

        if ("default".equals(group.getGroupIcon())) {
            holder.group_icon.setImageResource(R.drawable.ic_group_primary);
        }
        else {
            Glide.with(mContext).load(group.getGroupIcon()).into(holder.group_icon);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, GroupMessageActivity.class);
                intent.putExtra("groupId", group.getGroupId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView group_title;
        public ImageView group_icon;
        private TextView last_msg;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            group_title = itemView.findViewById(R.id.group_title);
            group_icon = itemView.findViewById(R.id.group_icon);
            last_msg = itemView.findViewById(R.id.last_msg);
        }

    }
}