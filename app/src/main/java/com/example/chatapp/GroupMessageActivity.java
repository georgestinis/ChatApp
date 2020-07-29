package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Adapter.GroupMessageAdapter;
import com.example.chatapp.Fragments.APIService;
import com.example.chatapp.Model.Group;
import com.example.chatapp.Model.GroupChat;
import com.example.chatapp.Model.Participant;
import com.example.chatapp.Model.User;
import com.example.chatapp.Notifications.Client;
import com.example.chatapp.Notifications.Data;
import com.example.chatapp.Notifications.MyResponse;
import com.example.chatapp.Notifications.Sender;
import com.example.chatapp.Notifications.Token;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupMessageActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    private CircleImageView group_icon;
    private TextView group_title;
    private String groupId, myGroupRole = "", groupTitle = "";

    private DatabaseReference reference;

    private ImageButton btn_send, btn_attach;
    private EditText text_send;

    private List<GroupChat> mGroupChat;
    private GroupMessageAdapter messageAdapter;

    private FirebaseUser firebaseUser;

    // Permissions request constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    // Image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    private static final int MICROPHONE_REQUEST_CODE = 500;

    // Permissions to be requested
    private String[] cameraPermission;
    private String[] storagePermission;
    private String[] microphonePermission;

    // Uri of picked image
    private Uri imageUri = null;

    private StorageReference storageReference;
    private StorageTask uploadTask;

    private ProgressDialog progressDialog;

    private APIService apiService;

    private boolean notify = false;

    private MediaRecorder mRecorder;
    private String mFileName = null;

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
                if (messageAdapter != null && messageAdapter.getMediaPlayer() != null &&
                        messageAdapter.getMediaPlayer().isPlaying()) {
                    messageAdapter.getMediaPlayer().stop();
                }
                startActivity(new Intent(GroupMessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/recorded_audio.3gpp";

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

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
        btn_attach = findViewById(R.id.btn_attach);

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");
        groupTitle = intent.getStringExtra("groupTitle");

        // Get the logged in user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        cameraPermission = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        storagePermission = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        microphonePermission = new String[] {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        loadGroupInfo();
        loadMyGroupRole();

        // If text is empty change drawable to mic
        text_send.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s.toString().trim())) {
                    btn_send.setBackgroundResource(R.drawable.ic_action_name);
                }
                else {
                    btn_send.setBackgroundResource(R.drawable.ic_mic_primary);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // When you hold btn_send start recording, when you let it stop recording
        btn_send.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (TextUtils.isEmpty(text_send.getText().toString().trim())) {
                    if (!checkMicrophonePermission()) {
                        requestMicrophonePermission();
                    }
                    else {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            startRecording();
                            Toast.makeText(GroupMessageActivity.this, "Recording started ...", Toast.LENGTH_SHORT).show();
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            stopRecording();
                            Toast.makeText(GroupMessageActivity.this, "Recording stopped ...", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                return false;
            }
        });

        // When you click the button to send a message
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                // Get the message
                String msg = text_send.getText().toString();
                if (!msg.equals("")) {
                    // Call send message function with your user id, your sender id and your message
                    sendMessage(firebaseUser.getUid(), msg);
                }
                text_send.setText("");
            }
        });

        btn_attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                // Pick image from gallery/camera
                showImageImportDialog();
            }
        });
    }

    private void loadMyGroupRole() {
        reference = FirebaseDatabase.getInstance().getReference("Groups");
        reference.child(groupId).child("Participants").child(firebaseUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        myGroupRole = (String) dataSnapshot.child("role").getValue();
                        invalidateOptionsMenu();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendMessage(String sender, String msg) {
        reference = FirebaseDatabase.getInstance().getReference("Groups");
        long time = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("message", msg);
        hashMap.put("length", 0);
        hashMap.put("time", time);
        hashMap.put("type", "text");

        reference.child(groupId).child("time").setValue(time);

        reference.child(groupId).child("Messages").child(time+"").setValue(hashMap)
        .addOnFailureListener(e -> Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());

        final String message = msg;

        reference = FirebaseDatabase.getInstance().getReference("Users").child(sender);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");
                    reference1.child(groupId).child("Participants").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Participant participant = snapshot.getValue(Participant.class);
                                if (!participant.getUid().equals(firebaseUser.getUid())) {
                                    sendNotifications(participant.getUid(), user.getUsername(), message, time);
                                }
                            }
                            notify = false;
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
                    readMessages(firebaseUser.getUid());
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

    private void sendNotifications(String receiver, final String username, final String msg, long time) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(firebaseUser.getUid(), R.mipmap.ic_launcher, username + ": " + msg, "New Group Message - " + group_title.getText(), receiver, time);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
                        @Override
                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                            if (response.code() == 200) {
                                if (response.body().success != 1) {
                                    Toast.makeText(GroupMessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call<MyResponse> call, Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void currentGroup(String groupId) {
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentgroup", groupId);
        editor.apply();
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentGroup(groupTitle);
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
        currentGroup("none");
    }

    /************** For image sending **************/

    private void sendImageMessage() throws IOException {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Sending Image...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        storageReference = FirebaseStorage.getInstance().getReference("ChatImages").child(System.currentTimeMillis()+ "." + getFileExtension(imageUri));
        // Compress the image
        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 15, baos);
        byte[] data = baos.toByteArray();

        uploadTask = storageReference.putBytes(data);

        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Return the image url
                return storageReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String mUri = downloadUri.toString();
                    String sender = firebaseUser.getUid();

                    reference = FirebaseDatabase.getInstance().getReference("Groups");
                    long time = System.currentTimeMillis();

                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("sender", sender);
                    hashMap.put("message", mUri);
                    hashMap.put("time", time);
                    hashMap.put("type", "image");
                    hashMap.put("length", 0);

                    reference.child(groupId).child("time").setValue(time);

                    reference.child(groupId).child("Messages").child(time+"").setValue(hashMap)
                            .addOnSuccessListener(e -> {
                                progressDialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            });

                    reference = FirebaseDatabase.getInstance().getReference("Users").child(sender);
                    reference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            User user = dataSnapshot.getValue(User.class);
                            if (notify) {
                                DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");
                                reference1.child(groupId).child("Participants").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            Participant participant = snapshot.getValue(Participant.class);
                                            if (!participant.getUid().equals(firebaseUser.getUid())) {
                                                sendNotifications(participant.getUid(), user.getUsername(), "sent a photo.", time);
                                            }
                                        }
                                        notify = false;
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
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

    private void showImageImportDialog() {
        // Options to display
        String options[] = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image").setItems(options, (dialog, which) -> {
            // Handle clicks
            if (which == 0) {
                // Camera clicked
                if (!checkCameraPermission()) {
                    requestCameraPermission();
                }
                else {
                    pickCamera();
                }
            }
            else {
                // Gallery clicked
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                }
                else {
                    pickGallery();
                }
            }
        }).show();
    }

    private void pickGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "GroupImage");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "GroupImageDescription");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result =  ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    // Get file extension
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE && data != null && data.getData() != null) {
                // Picked from gallery
                imageUri = data.getData();
                try {
                    sendImageMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // Picked from camera
                try {
                    sendImageMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    System.out.println(grantResults.length);
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    
                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    }
                    else {
                        Toast.makeText(this, "Camera & Storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickGallery();
                    }
                    else {
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    /************** For audio sending **************/

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, microphonePermission, MICROPHONE_REQUEST_CODE);
    }

    private boolean checkMicrophonePermission() {
        boolean result =  ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Toast.makeText(this, "Audio prepare failed", Toast.LENGTH_SHORT).show();
        }
        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        uploadAudio(firebaseUser.getUid());
    }

    private static int getDuration(File file) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(file.getAbsolutePath());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(durationStr);
    }

    private void uploadAudio(String sender) {
        storageReference = FirebaseStorage.getInstance().getReference("GroupAudioRecorded").child(System.currentTimeMillis()+ ".3gpp");
        File file = new File(mFileName);
        Uri uri = Uri.fromFile(file);
        int duration = getDuration(file);
        // Max 5min duration
        if (duration <= 300000) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Sending Audio...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            StorageTask uploadTask;

            uploadTask = storageReference.putFile(uri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

                        reference = FirebaseDatabase.getInstance().getReference("Groups");
                        long time = System.currentTimeMillis();

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("sender", sender);
                        hashMap.put("message", mUri);
                        hashMap.put("time", time);
                        hashMap.put("type", "audio");
                        hashMap.put("length", duration);
                        reference.child(groupId).child("time").setValue(time);

                        reference.child(groupId).child("Messages").child(time+"").setValue(hashMap)
                                .addOnSuccessListener(e -> {
                                    progressDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                });

                        reference = FirebaseDatabase.getInstance().getReference("Users").child(sender);
                        reference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                if (notify) {
                                    DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Groups");
                                    reference1.child(groupId).child("Participants").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                Participant participant = snapshot.getValue(Participant.class);
                                                if (!participant.getUid().equals(firebaseUser.getUid())) {
                                                    sendNotifications(participant.getUid(), user.getUsername(), "sent a voice message.", time);
                                                }
                                            }
                                            notify = false;
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
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            });
        }
        else {
            Toast.makeText(this, "You've exceeded the allowed voice message duration, 5min.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.logout).setVisible(false);
        menu.findItem(R.id.create_group).setVisible(false);
        menu.findItem(R.id.show_participants).setVisible(true);
        if (myGroupRole != null) {
            if (myGroupRole.equals("creator") || myGroupRole.equals("admin")) {
                menu.findItem(R.id.add_participant).setVisible(true);
            } else {
                menu.findItem(R.id.add_participant).setVisible(false);
            }
        }
        return true;
    }

    // Selecting anything from navigation menu will trigger this method
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.add_participant:
                if (messageAdapter != null && messageAdapter.getMediaPlayer() != null &&
                        messageAdapter.getMediaPlayer().isPlaying()) {
                    messageAdapter.getMediaPlayer().stop();
                }
                intent = new Intent(GroupMessageActivity.this, GroupParticipantsAddActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
                break;
            case R.id.show_participants:
                if (messageAdapter != null && messageAdapter.getMediaPlayer() != null &&
                        messageAdapter.getMediaPlayer().isPlaying()) {
                    messageAdapter.getMediaPlayer().stop();
                }
                intent = new Intent(GroupMessageActivity.this, GroupParticipantsShowActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
                break;
            case R.id.groupinfo:
                if (messageAdapter != null && messageAdapter.getMediaPlayer() != null &&
                        messageAdapter.getMediaPlayer().isPlaying()) {
                    messageAdapter.getMediaPlayer().stop();
                }
                intent = new Intent(GroupMessageActivity.this, GroupInfoActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
