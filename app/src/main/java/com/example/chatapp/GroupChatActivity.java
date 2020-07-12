package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {

    private FirebaseUser fuser;

    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;
    private StorageReference storageReference;

    private ProgressDialog progressDialog;

    private String mUri;

    private ImageView groupIcon;
    private EditText groupTitle;
    private Button groupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Group Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GroupChatActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        groupIcon = findViewById(R.id.groupIcon);
        groupTitle = findViewById(R.id.groupTitle);
        groupBtn = findViewById(R.id.createGroupBtn);

        // Get a reference from firebase storage
        storageReference = FirebaseStorage.getInstance().getReference("GroupImages");

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (fuser != null) {
            getSupportActionBar().setSubtitle(fuser.getDisplayName());
        }

        groupIcon.setOnClickListener(v -> openImage());

        groupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startCreatingGroup();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void startCreatingGroup() throws IOException {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating Group");

        String groupTitle = this.groupTitle.getText().toString().trim();

        if (TextUtils.isEmpty(groupTitle)) {
            Toast.makeText(this, "Please Enter group title...", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (imageUri == null) {
            createGroup(groupTitle, "default");
        } else {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    + "." + getFileExtension(imageUri));

            // Compress the image
            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 15, baos);
            byte[] data = baos.toByteArray();

            // Upload the image to data base
            uploadTask = fileReference.putBytes(data);

            //uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Return the image url
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        mUri = downloadUri.toString();
                        createGroup(groupTitle, mUri);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(GroupChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            });

        }
    }

    private void createGroup(String groupTitle, String groupIcon) {
        HashMap<String, Object> hashMap = new HashMap<>();
        final long time = System.currentTimeMillis();
        hashMap.put("groupId", time + "");
        hashMap.put("groupTitle", groupTitle);
        hashMap.put("groupIcon", groupIcon);
        hashMap.put("time", time);
        hashMap.put("createdBy", fuser.getUid());

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(time + "").setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                HashMap<String, Object> hashMap1 = new HashMap<>();
                hashMap1.put("uid", fuser.getUid());
                hashMap1.put("role", "creator");
                hashMap1.put("timestamp", time);

                DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");
                reference1.child(time + "").child("Participants").child(fuser.getUid()).setValue(hashMap1)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(GroupChatActivity.this, "Groupd created successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(GroupChatActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(GroupChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(GroupChatActivity.this, "Failed to create Group", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        // Allow the user to select a particular kind of data and return it
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    // Get file extension
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    // Receive the result from a previous call to startActivityForResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if requestCode is 1 and result code is -1 and there is some data (image selected)
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            // Set imageUri with the given URI data
            imageUri = data.getData();
            groupIcon.setImageURI(imageUri);
            // If you already uploading show a toast message
            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(GroupChatActivity.this, "Uploading in progress", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void status(String status) {
        if (fuser != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

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