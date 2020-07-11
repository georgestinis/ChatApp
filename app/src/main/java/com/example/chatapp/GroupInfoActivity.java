package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Adapter.ParticipantAdapter;
import com.example.chatapp.Model.Participant;
import com.example.chatapp.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class GroupInfoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageView groupIcon;
    private TextView createdBy;
    private TextView edit_group;
    private TextView add_participant;
    private TextView leave_group;
    private TextView participants;

    private String groupId;
    private String myGroupRole;

    private FirebaseUser fuser;

    private List<User> allParticipants;
    private ParticipantAdapter participantAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        allParticipants = new ArrayList<>();

        recyclerView = findViewById(R.id.show_participants);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupIcon = findViewById(R.id.group_icon);
        createdBy = findViewById(R.id.created_by);
        edit_group = findViewById(R.id.edit_group);
        add_participant = findViewById(R.id.add_participant);
        leave_group = findViewById(R.id.leave_group);
        participants = findViewById(R.id.participants);

        groupId = getIntent().getStringExtra("groupId");

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        loadGroupInfo();

        add_participant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupInfoActivity.this, GroupParticipantsAddActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            }
        });
    }

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");

        reference.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get Group Info
                String groupId = (String) dataSnapshot.child("groupId").getValue();
                String groupTitle = (String) dataSnapshot.child("groupTitle").getValue();
                String groupIconS = (String) dataSnapshot.child("groupIcon").getValue();
                String createdByS = (String) dataSnapshot.child("createdBy").getValue();
                Long timestamp = (Long) dataSnapshot.child("time").getValue();

                // Convert timestamp
                Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                calendar.setTimeInMillis(Long.parseLong(groupId));
                String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                getSupportActionBar().setTitle(groupTitle);
                loadCreatorInfo(dateTime, createdByS);

                if ("default".equals(groupIconS)) {
                    groupIcon.setImageResource(R.drawable.ic_group_primary);
                }
                else {
                    Glide.with(getApplicationContext()).load(groupIconS).into(groupIcon);
                }

                if (fuser != null) {
                    reference1.child(groupId).child("Participants").child(fuser.getUid())
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        myGroupRole = (String) dataSnapshot.child("role").getValue();
                                        getSupportActionBar().setSubtitle(fuser.getEmail() + " (" + myGroupRole + ")");
                                        if (myGroupRole.equals("participant")) {
                                            edit_group.setVisibility(View.GONE);
                                            add_participant.setVisibility(View.GONE);
                                            leave_group.setText("Leave Group");
                                        }
                                        else if (myGroupRole.equals("admin")) {
                                            edit_group.setVisibility(View.GONE);
                                            add_participant.setVisibility(View.VISIBLE);
                                            leave_group.setText("Leave Group");
                                        }
                                        else if (myGroupRole.equals("creator")) {
                                            edit_group.setVisibility(View.VISIBLE);
                                            add_participant.setVisibility(View.VISIBLE);
                                            leave_group.setText("Delete Group");
                                        }
                                        loadParticipants();
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

    private void loadParticipants() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants");
        if (fuser != null) {
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                    allParticipants.clear();
                    // For every user except me add to list
                    final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                    while (iterator.hasNext()) {
                        final Participant participant = iterator.next().getValue(Participant.class);
                        assert participant != null;
                        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("id").equalTo(participant.getUid());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                                for (DataSnapshot snapshot1 : dataSnapshot1.getChildren()) {
                                    User user = snapshot1.getValue(User.class);
                                    if (!fuser.getUid().equals(user.getId())) {
                                        if(!allParticipants.contains(user)) {
                                            allParticipants.add(user);
                                        }
                                    }
                                }
                                if (!iterator.hasNext()) {
                                    participantAdapter = new ParticipantAdapter(GroupInfoActivity.this, allParticipants, groupId, myGroupRole);
                                    participants.setText("Participants (" + allParticipants.size() + ")");
                                    recyclerView.setAdapter(participantAdapter);
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
    }

    private void loadCreatorInfo(String dateTime, String createdBys) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(createdBys).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = (String) dataSnapshot.child("username").getValue();
                createdBy.setText("Created by " + name + " on" + dateTime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}