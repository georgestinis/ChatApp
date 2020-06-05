package com.example.chatapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Adapter.AddParticipantAdapter;
import com.example.chatapp.Model.Friend;
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
import java.util.Iterator;
import java.util.List;

public class GroupParticipantsAddActivity extends AppCompatActivity {

    private AddParticipantAdapter addParticipantAdapter;
    private RecyclerView recyclerView;
    private List<User> allUsers;
    private FirebaseUser fuser;

    private String groupId;
    private String myGroupRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_participants_add);

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add Participants");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        fuser = FirebaseAuth.getInstance().getCurrentUser();

        allUsers = new ArrayList<>();

        recyclerView = findViewById(R.id.add_participant_rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupId = getIntent().getStringExtra("groupId");
        loadGroupInfo();
    }

    private void getAllUsers() {
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Friends").child(firebaseUser.getUid());
        if (firebaseUser != null) {
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                    allUsers.clear();
                    // For every user except me add to list
                    final Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                    while (iterator.hasNext()) {
                        final Friend friend = iterator.next().getValue(Friend.class);
                        assert friend != null;
                        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("id").equalTo(friend.getId());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                                for (DataSnapshot snapshot1 : dataSnapshot1.getChildren()) {
                                    User user = snapshot1.getValue(User.class);
                                    assert friend != null;
                                    if (!firebaseUser.getUid().equals(user.getId())) {
                                        if(!allUsers.contains(user)) {
                                            allUsers.add(user);
                                        }
                                    }
                                }
                                if (!iterator.hasNext()) {
                                    addParticipantAdapter = new AddParticipantAdapter(GroupParticipantsAddActivity.this, allUsers, groupId, myGroupRole);
                                    recyclerView.setAdapter(addParticipantAdapter);
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

    private void loadGroupInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");

        reference.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String groupId = (String) dataSnapshot.child("groupId").getValue();
                String groupTitle = (String) dataSnapshot.child("groupTitle").getValue();
                String groupIcon = (String) dataSnapshot.child("groupIcon").getValue();
                String createdBy = (String) dataSnapshot.child("createdBy").getValue();
                Long timestamp = (Long) dataSnapshot.child("timestamp").getValue();

                getSupportActionBar().setTitle("Add Participants");

                if (fuser != null) {
                    reference1.child(groupId).child("Participants").child(fuser.getUid())
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        myGroupRole = (String) dataSnapshot.child("role").getValue();
                                        getSupportActionBar().setTitle(groupTitle + "(" + myGroupRole + ")");
                                        getAllUsers();
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}