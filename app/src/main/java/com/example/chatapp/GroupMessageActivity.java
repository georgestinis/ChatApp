package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Adapter.GroupMessageAdapter;
import com.example.chatapp.Model.Group;
import com.example.chatapp.Model.GroupChat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMessageActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    private CircleImageView group_icon;
    private TextView group_title;
    private String groupId;

    private DatabaseReference reference;

    private ImageButton btn_send;
    private EditText text_send;

    private List<GroupChat> mGroupChat;
    private GroupMessageAdapter messageAdapter;

    private FirebaseUser fuser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GroupMessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        // Creates a linear layout for all messages and show them from bottom to start
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        group_icon = findViewById(R.id.groupIcon);
        group_title = findViewById(R.id.groupTitle);
        text_send = findViewById(R.id.text_send);
        btn_send = findViewById(R.id.btn_send);

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");
        // Get the logged in user
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        loadGroupInfo();

        // When you click the button to send a message
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 // Get the message
                String msg = text_send.getText().toString();
                if (!msg.equals("")) {
                    // Call send message function with your user id, your sender id and your message
                    sendMessage(fuser.getUid(), msg);
                }
                else {
                    Toast.makeText(GroupMessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }
                text_send.setText("");
            }
        });
    }

    private void sendMessage(String sender, String msg) {
        reference = FirebaseDatabase.getInstance().getReference("Groups");
        long time = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("message", msg);
        hashMap.put("time", time);

        reference.child(groupId).child("time").setValue(time);

        reference.child(groupId).child("Messages").child(time+"").setValue(hashMap)
        .addOnFailureListener(e -> Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());


    }

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups").child(groupId);
        //reference.orderByChild("groupId").equalTo(groupId)
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Group group = dataSnapshot.getValue(Group.class);
                    group_title.setText(group.getGroupTitle());
                    if ("default".equals(group.getGroupIcon())) {
                        group_icon.setImageResource(R.drawable.ic_group);
                    }
                    else {
                        Glide.with(getApplicationContext()).load(group.getGroupIcon()).into(group_icon);
                    }
                    readMessages(fuser.getUid());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void readMessages(String myId) {
        mGroupChat = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Groups")
                .child(groupId).child("Messages");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mGroupChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GroupChat groupChat = snapshot.getValue(GroupChat.class);
                    mGroupChat.add(groupChat);
                }
                messageAdapter = new GroupMessageAdapter(GroupMessageActivity.this, mGroupChat);
                recyclerView.setAdapter(messageAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //reference.removeEventListener(seenListener);
        status("offline");
    }
}