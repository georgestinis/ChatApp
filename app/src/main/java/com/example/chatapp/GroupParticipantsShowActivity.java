package com.example.chatapp;

import android.os.Bundle;

        import androidx.annotation.NonNull;
        import androidx.appcompat.app.AppCompatActivity;
        import androidx.appcompat.widget.Toolbar;
        import androidx.recyclerview.widget.LinearLayoutManager;
        import androidx.recyclerview.widget.RecyclerView;

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
import java.util.HashMap;
import java.util.Iterator;
        import java.util.List;

public class GroupParticipantsShowActivity extends AppCompatActivity {

    private ParticipantAdapter participantAdapter;
    private RecyclerView recyclerView;
    private List<User> allParticipants;
    private FirebaseUser firebaseUser;

    private String groupId, myGroupRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_participants_add);

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        allParticipants = new ArrayList<>();

        recyclerView = findViewById(R.id.add_participant_rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupId = getIntent().getStringExtra("groupId");
        loadGroupInfo();
    }

    private void getAllUsers() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("Participants");
        if (firebaseUser != null) {
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
                                    if (!firebaseUser.getUid().equals(user.getId())) {
                                        if(!allParticipants.contains(user)) {
                                            allParticipants.add(user);
                                        }
                                    }
                                }
                                if (!iterator.hasNext()) {
                                    participantAdapter = new ParticipantAdapter(GroupParticipantsShowActivity.this, allParticipants, groupId, myGroupRole);
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

                if (firebaseUser != null) {
                    reference1.child(groupId).child("Participants").child(firebaseUser.getUid())
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        myGroupRole = (String) dataSnapshot.child("role").getValue();
                                        getSupportActionBar().setTitle(groupTitle + "(" + myGroupRole + ") - Participants");
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


    private void status(String status) {
        if (firebaseUser != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("status", status);

            reference.updateChildren(hashMap);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }
}