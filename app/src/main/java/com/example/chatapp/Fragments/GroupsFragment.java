package com.example.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.chatapp.Adapter.GroupAdapter;
import com.example.chatapp.Model.Group;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupsFragment extends Fragment {

    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private List<Group> mGroups;
    private EditText search_groups;

    private FirebaseUser firebaseUser;
    private DatabaseReference reference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);

        recyclerView = view.findViewById(R.id.group_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        loadGroupChatsList();

        search_groups = view.findViewById(R.id.search_groups);
        search_groups.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When you change the text, call search users method
                if (!TextUtils.isEmpty(s.toString().trim())) {
                    searchGroupChatsList(s.toString());
                }
                else {
                    loadGroupChatsList();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    private void loadGroupChatsList() {
        mGroups = new ArrayList<>();

        if (firebaseUser != null) {
            reference = FirebaseDatabase.getInstance().getReference("Groups");
            Query query = reference.orderByChild("time");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    mGroups.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child("Participants").child(firebaseUser.getUid()).exists()) {
                            Group group = snapshot.getValue(Group.class);
                            mGroups.add(group);
                        }
                    }
                    Collections.reverse(mGroups);
                    groupAdapter = new GroupAdapter(getContext(), mGroups);
                    recyclerView.setAdapter(groupAdapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void searchGroupChatsList(String search_query) {
        mGroups = new ArrayList<>();

        if (firebaseUser != null) {
            reference = FirebaseDatabase.getInstance().getReference("Groups");
            Query query = reference.orderByChild("time");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    mGroups.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child("Participants").child(firebaseUser.getUid()).exists()) {
                            if (snapshot.child("groupTitle").toString().toLowerCase().contains(search_query.toLowerCase())) {
                                Group group = snapshot.getValue(Group.class);
                                mGroups.add(group);
                            }
                        }
                    }
                    Collections.reverse(mGroups);
                    groupAdapter = new GroupAdapter(getContext(), mGroups);
                    recyclerView.setAdapter(groupAdapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
}