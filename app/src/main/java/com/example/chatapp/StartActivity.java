package com.example.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// TODO CLEAR NOTIFICATIONS FOR GROUP (CAN'T BE DONE BECAUSE OF THE PARTICIPANTS BEING MORE THAN 1)
// TODO WHEN CLICKING IMAGE IN CHAT OPEN A WINDOW FOR IT
// TODO GET CHATS INSIDE CHATLISΤ (PROBABLY NOT WORTH IT)

public class StartActivity extends AppCompatActivity {

    private Button btn_login, btn_register;

    private FirebaseUser firebaseUser;

    @Override
    protected void onStart() {
        super.onStart();

        // Check if the current user is already defined and not null
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        btn_login = findViewById(R.id.login);
        btn_register = findViewById(R.id.register);

        btn_login.setOnClickListener(v -> startActivity(new Intent(StartActivity.this, LoginActivity.class)));

        btn_register.setOnClickListener(v -> startActivity(new Intent(StartActivity.this, RegisterActivity.class)));
    }
}