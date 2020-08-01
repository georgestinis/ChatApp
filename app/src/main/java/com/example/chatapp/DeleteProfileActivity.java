package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.Chatlist;
import com.example.chatapp.Model.Friend;
import com.example.chatapp.Model.Group;
import com.example.chatapp.Model.Participant;
import com.example.chatapp.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

public class DeleteProfileActivity extends AppCompatActivity {
    private Button btn_delete;
    private MaterialEditText email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DeleteProfileActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        btn_delete = findViewById(R.id.btn_delete);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);

        btn_delete.setOnClickListener(v -> {
            if (TextUtils.isEmpty(email.getText().toString()) | TextUtils.isEmpty(password.getText().toString())) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
            else {
                final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                AuthCredential credential = EmailAuthProvider.getCredential(email.getText().toString(), password.getText().toString());

                firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            String dialogTitle;
                            String dialogDescription;
                            String positiveButtonTitle;

                            dialogTitle = "Delete Profile";
                            dialogDescription = "Are you sure you want to Delete your profile permanently?";
                            positiveButtonTitle = "DELETE";

                            AlertDialog.Builder builder = new AlertDialog.Builder(DeleteProfileActivity.this);
                            builder.setTitle(dialogTitle).setMessage(dialogDescription).setPositiveButton(positiveButtonTitle, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String currentUserUid = firebaseUser.getUid();
                                    deleteChats(currentUserUid);
                                    deleteGroups(currentUserUid);
                                    deleteChatlist(currentUserUid);
                                    deleteFriend(currentUserUid);
                                    deleteToken(currentUserUid);
                                    deleteUser(firebaseUser, currentUserUid);
                                }
                            }).setNeutralButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();

                        } else {
                            Toast.makeText(DeleteProfileActivity.this, "Error authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        });

    }

    private void deleteToken(String currentUserUid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens").child(currentUserUid);
        reference.removeValue();
    }

    private void deleteFriend(String currentUserUid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Friends");
        reference.child(currentUserUid).removeValue();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (!user.getId().equals(currentUserUid)) {
                        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Friends").child(user.getId()).child(currentUserUid);
                        reference1.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    reference1.removeValue();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteChatlist(String currentUserUid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chatlist");
        reference.child(currentUserUid).removeValue();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (!user.getId().equals(currentUserUid)) {
                        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Chatlist").child(user.getId()).child(currentUserUid);
                        reference1.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    reference1.removeValue();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteGroups(String currentUserUid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Group group = snapshot.getValue(Group.class);

                    DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups").child(group.getGroupId()).child("Participants").child(currentUserUid);
                    reference1.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Participant participant = dataSnapshot.getValue(Participant.class);
                                if (participant.getUid().equals(currentUserUid)) {
                                    switch (participant.getRole()) {
                                        case "creator":
                                            reference.child(group.getGroupId()).removeValue();
                                            break;
                                        case "admin":
                                        case "participant":
                                            reference1.removeValue();
                                            break;
                                    }
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

    private void deleteChats(String currentUserUid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getSender().equals(currentUserUid) || chat.getReceiver().equals(currentUserUid)) {
                        reference.child("" + chat.getTime()).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteUser(@NonNull FirebaseUser firebaseUser, String currentUserUid) {
        firebaseUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                    reference.child(currentUserUid).removeValue();
                    Toast.makeText(DeleteProfileActivity.this, "User account deleted", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(DeleteProfileActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                } else {
                    Toast.makeText(DeleteProfileActivity.this, "User account deletion unsuccessful", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}