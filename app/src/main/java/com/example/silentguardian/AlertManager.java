package com.example.silentguardian;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;

public class AlertManager {
    private static final String TAG = "AlertManager";
    public static void trigger(Context context, String emergencyType) {
        vibrateThreeTimes(context);
        flashThreeTimes(context);
        sendSmsWithLocation(context, emergencyType);

        Intent intent = new Intent(context, AlertActivity.class);
        intent.putExtra("emergencyType", emergencyType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void vibrateThreeTimes(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;

        // pattern: wait, vibrate, pause, vibrate, pause, vibrate
        long[] pattern = {0, 400, 200, 400, 200, 400};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)); // -1 = do not repeat
    }

    private static void flashThreeTimes(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) return;

        String cameraId = null;
        try {
            for (String id : cameraManager.getCameraIdList()) {
                Boolean hasFlash = cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return;
        }

        if (cameraId == null) return; // device has no flash

        final String finalCameraId = cameraId;
        Handler handler = new Handler(Looper.getMainLooper());

        // matches the vibration timing: on-off-on-off-on-off
        int[] delays = {0, 400, 600, 1000, 1200, 1600};
        boolean[] states = {true, false, true, false, true, false};

        for (int i = 0; i < delays.length; i++) {
            boolean turnOn = states[i];
            handler.postDelayed(() -> {
                try {
                    cameraManager.setTorchMode(finalCameraId, turnOn);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }, delays[i]);
        }
    }
    private static void sendSmsWithLocation(Context context, String emergencyType) {
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasSmsPermission) {
            Log.w(TAG, "SMS permission not granted, skipping SMS");
            return;
        }

        List<Contact> contacts = ContactStorage.load(context);
        if (contacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts saved, skipping SMS");
            return;
        }

        if (!hasLocationPermission) {
            sendToAllContacts(context, contacts, emergencyType, "location unavailable");
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        try {
            client.getCurrentLocation(request, new CancellationTokenSource().getToken())
                    .addOnSuccessListener(location -> {
                        String locationText = (location != null)
                                ? "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude()
                                : "location unavailable";
                        sendToAllContacts(context, contacts, emergencyType, locationText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location", e);
                        sendToAllContacts(context, contacts, emergencyType, "location unavailable");
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing at call time", e);
            sendToAllContacts(context, contacts, emergencyType, "location unavailable");
        }
    }

    private static void sendToAllContacts(Context context, List<Contact> contacts, String emergencyType, String locationText) {
        String message = "EMERGENCY: " + emergencyType + " detected. My location: " + locationText;
        SmsManager smsManager = SmsManager.getDefault();
        int successCount = 0;
        for (Contact contact : contacts) {
            try {
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null);
                Log.i(TAG, "SMS sent to " + contact.phoneNumber);
                successCount++;
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS to " + contact.phoneNumber, e);
            }
        }

        int finalSuccessCount = successCount;
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, finalSuccessCount + "/" + contacts.size() + " SMS sent", Toast.LENGTH_LONG).show()
        );
    }
    public static void sendSafeMessage(Context context) {
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasSmsPermission) {
            Log.w(TAG, "SMS permission not granted, skipping safe message");
            return;
        }

        List<Contact> contacts = ContactStorage.load(context);
        if (contacts.isEmpty()) return;

        String message = "SILENT GUARDIAN: I am safe. The earlier alert was a false positive.";
        SmsManager smsManager = SmsManager.getDefault();

        for (Contact contact : contacts) {
            try {
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null);
                Log.i(TAG, "Safe message sent to " + contact.phoneNumber);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send safe message to " + contact.phoneNumber, e);
            }
        }
    }
}
