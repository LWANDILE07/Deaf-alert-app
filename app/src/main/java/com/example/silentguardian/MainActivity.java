package com.example.silentguardian;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Start the listening animation
        View pulseRing = findViewById(R.id.listeningStatus);

        Animation pulse = AnimationUtils.loadAnimation(
                this,
                R.anim.pulse_ring
        );

        pulseRing.startAnimation(pulse);

        // Emergency contacts button
        Button contactsButton = findViewById(R.id.contactsButton);

        contactsButton.setOnClickListener(v -> {
            startActivity(
                    new Intent(this, ContactsActivity.class)
            );
        });

        // Ask for permissions.
        // Once permissions are granted, the microphone
        // service starts automatically.
        requestNeededPermissions();
    }

    private void requestNeededPermissions() {

        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
        };

        boolean allGranted = true;

        for (String permission : permissions) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
            ) != PackageManager.PERMISSION_GRANTED) {

                allGranted = false;
                break;
            }
        }

        if (allGranted) {

            // Everything is already allowed.
            // Start listening automatically.
            startAudioDetection();

        } else {

            // Ask the user for the required permissions.
            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == PERMISSION_REQUEST_CODE) {

            boolean microphoneGranted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED;

            if (microphoneGranted) {

                // The user has allowed microphone access.
                // Start listening automatically.
                startAudioDetection();
            }
        }
    }

    private void startAudioDetection() {

        Intent serviceIntent =
                new Intent(
                        this,
                        AudioDetectionService.class
                );

        /*
         * Android 8+ requires a foreground service
         * when we want the microphone to keep working
         * while the app is in the background.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            ContextCompat.startForegroundService(
                    this,
                    serviceIntent
            );

        } else {

            startService(serviceIntent);
        }
    }
}