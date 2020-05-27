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

    FirebaseUser fuser;

    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;
    private StorageReference storageReference;

    private ImageView groupIcon;
    private EditText groupTitle, groupDesc;
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
        groupDesc = findViewById(R.id.groupDesc);
        groupBtn = findViewById(R.id.createGroupBtn);

        // Get a refernce from firebase storage
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (fuser != null) {
            getSupportActionBar().setSubtitle(fuser.getDisplayName());
        }

        groupIcon.setOnClickListener(v -> openImage());

        groupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
        ContentResolver contentResolver = Objects.requireNonNull(GroupChatActivity.this).getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() throws IOException {
        // Show a progress dialog with a message
        final ProgressDialog pd = new ProgressDialog(GroupChatActivity.this);
        pd.setMessage("Uploading");
        pd.show();

        // If imageUri has been initialized
        if (imageUri != null) {
            // Set storage reference
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri));

            // Compress the image
            Bitmap bmp = MediaStore.Images.Media.getBitmap(Objects.requireNonNull(GroupChatActivity.this).getContentResolver(), imageUri);
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
//                        Uri downloadUri = task.getResult();
//                        String mUri = downloadUri.toString();
//                        // Put image url to users reference
//                        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
//                        HashMap<String, Object> map = new HashMap<>();
//                        map.put("imageURL", mUri);
//                        reference.updateChildren(map);
                        // Close the progress dialog
                        Toast.makeText(GroupChatActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                    else {
                        Toast.makeText(GroupChatActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(GroupChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        }
        else {
            Toast.makeText(GroupChatActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
        }
    }

    // Receive the result from a previous call to startActivityForResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if requestCode is 1 and result code is -1 and there is some data (image selected)
        if (requestCode == IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            // Set imageUri with the given URI data
            imageUri = data.getData();

            // If you already uploading show a toast message
            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(GroupChatActivity.this, "Uploading in progress", Toast.LENGTH_SHORT).show();
            }
            // Else call uploadImage
            else {
                try {
                    uploadImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}