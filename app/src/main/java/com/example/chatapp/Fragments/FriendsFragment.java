package com.example.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.chatapp.Adapter.UserAdapter;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.Friend;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.google.android.gms.tasks.Tasks;
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

import static androidx.constraintlayout.widget.Constraints.TAG;

public class FriendsFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> allUsers;

    private EditText search_users;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        allUsers = new ArrayList<>();
        readUsers();

        search_users = view.findViewById(R.id.search_friends);
        // Trigger this event every time you add a character
        search_users.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When you change the text, call search users method
                if (!TextUtils.isEmpty(s.toString().trim())) {
                    search_users(s.toString().toLowerCase());
                }
                else {
                    readUsers();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    private void search_users(String s) {

        final FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (fuser != null) {
            // Create a query from users reference and order by search value that starts with the given string
            Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("search");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    allUsers.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);

                        assert user != null;
                        if (!user.getId().equals(fuser.getUid())) {
                            if (snapshot.child("search").toString().toLowerCase().contains(s.toLowerCase())) {
                                allUsers.add(user);
                            }
                        }
                    }

                    userAdapter = new UserAdapter(getContext(), allUsers, false, false);
                    recyclerView.setAdapter(userAdapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    // Get the current user and create a reference for every user
    private void readUsers() {
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
                                    userAdapter = new UserAdapter(getContext(), allUsers, false, true);
                                    recyclerView.setAdapter(userAdapter);
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

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int position = -1;
        try {
            position = ((UserAdapter) recyclerView.getAdapter()).getPosition();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage(), e);
            return super.onContextItemSelected(item);
        }
        final User user = allUsers.get(position);
        final FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        switch (item.getItemId()) {
            case R.id.del_friend:
                FirebaseDatabase.getInstance().getReference("Friends").child(fuser.getUid()).child(user.getId()).removeValue();
                FirebaseDatabase.getInstance().getReference("Friends").child(user.getId()).child(fuser.getUid()).removeValue();
            case R.id.deleting:
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot deleteSnapshot: dataSnapshot.getChildren()) {
                            Chat chat = deleteSnapshot.getValue(Chat.class);
                            assert chat != null;
                            if (chat.getReceiver() != null && chat.getSender() != null) {
                                if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(user.getId()) ||
                                        chat.getReceiver().equals(user.getId()) && chat.getSender().equals(fuser.getUid())) {
                                    if (chat.getDeletedfrom().equals("none")) {
                                        HashMap<String, Object> hashMap = new HashMap<>();
                                        hashMap.put("deletedfrom", fuser.getUid());
                                        deleteSnapshot.getRef().updateChildren(hashMap);
                                    } else if (chat.getDeletedfrom().equals(user.getId())) {
                                        deleteSnapshot.getRef().removeValue();
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            case R.id.archiving:
                FirebaseDatabase.getInstance().getReference("Chatlist").child(fuser.getUid()).child(user.getId()).removeValue();
                break;

        }
        return super.onContextItemSelected(item);
    }

}