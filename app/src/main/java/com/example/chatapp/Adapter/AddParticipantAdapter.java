package com.example.chatapp.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddParticipantAdapter extends RecyclerView.Adapter<AddParticipantAdapter.MyViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private String groupId, myGroupRole;

    public AddParticipantAdapter(Context mContext, List<User> mUsers, String groupId, String myGroupRole) {
        this.mContext = mContext;
        this.mUsers = mUsers;
        this.groupId = groupId;
        this.myGroupRole = myGroupRole;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.participants_add, parent, false);
        return new AddParticipantAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        User user = mUsers.get(position);

        holder.username.setText(user.getUsername());
        if ("default".equals(user.getImageURL())) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }

        checkIfAlreadyExists(user, holder);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
                reference.child(groupId).child("Participants").child(user.getId())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // User is participant
                                    String previousRole = (String) dataSnapshot.child("role").getValue();
                                    String[] options;

                                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                    builder.setTitle("Choose Option");
                                    if (myGroupRole.equals("creator")) {
                                        if (previousRole.equals("admin")) {
                                            options = new String[]{"Remove Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Handle item clicks
                                                    if (which == 0) {
                                                        // Remove Admin
                                                        removeAdmin(user);
                                                    }
                                                    else {
                                                        // Remove User
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if (previousRole.equals("participant")) {
                                            options = new String[]{"Make Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Handle item clicks
                                                    if (which == 0) {
                                                        // Make Admin
                                                        makeAdmin(user);
                                                    } else {
                                                        // Remove User
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                    else if (myGroupRole.equals("admin")) {
                                        if (previousRole.equals("creator")) {
                                            Toast.makeText(mContext, "Creator of Group...", Toast.LENGTH_SHORT).show();
                                        } else if (previousRole.equals("admin")) {
                                            options = new String[]{"Remove Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Handle item clicks
                                                    if (which == 0) {
                                                        // Remove Admin
                                                        removeAdmin(user);
                                                    } else {
                                                        // Remove User
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        } else if (previousRole.equals("participant")) {
                                            options = new String[]{"Make Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Handle item clicks
                                                    if (which == 0) {
                                                        // Make Admin
                                                        makeAdmin(user);
                                                    } else {
                                                        // Remove User
                                                        removeParticipant(user);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                }
                                else {
                                    // User is not a participant
                                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                    builder.setTitle("Add Participant").setMessage("Add this user in this group?")
                                            .setPositiveButton("Add User", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Add user
                                                    addParticipant(user);
                                                }
                                            })
                                            .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).show();

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });

    }

    private void makeAdmin(User user) {
        // Make a user admin - make his/her admin
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("role", "admin");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(user.getId()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, "The user is now Admin", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addParticipant(User user) {
        // Add this user to this group
        Long timestamp = System.currentTimeMillis();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", user.getId());
        hashMap.put("role", "participant");
        hashMap.put("timestamp", timestamp);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(user.getId()).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, "Added successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeParticipant(User user) {
        // Remove user from this group
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(user.getId()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, "Removed successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void removeAdmin(User user) {
        // Remove admin role - make him/her participant
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("role", "participant");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(user.getId()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, "The user is no longer Admin", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfAlreadyExists(User user, MyViewHolder holder) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(user.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            holder.status.setText("" + dataSnapshot.child("role").getValue());
                        }
                        else {
                            holder.status.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private ImageView profile_image;
        private TextView username;
        private TextView status;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            profile_image = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.username);
            status = itemView.findViewById(R.id.status);
        }
    }
}
