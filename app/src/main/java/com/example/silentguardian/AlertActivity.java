package com.example.silentguardian;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alert);

        String emergencyType = getIntent().getStringExtra("emergencyType");

        TextView label = findViewById(R.id.alertLabel);
        label.setText(emergencyType + " detected!");

        TextView timeView = findViewById(R.id.alertTime);
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        timeView.setText("Detected at " + time);

        findViewById(R.id.cancelAlertButton).setOnClickListener(v -> {
            AlertManager.sendSafeMessage(getApplicationContext());
            finish();
        });
    }
}