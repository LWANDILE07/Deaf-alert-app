package com.example.silentguardian;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioRecord;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioDetectionService extends Service {

    private static final String TAG =
            "AudioDetectionService";

    private static final String CHANNEL_ID =
            "audio_detection_channel";

    private static final int NOTIF_ID = 42;

    private static final String MODEL_FILE =
            "yamnet.tflite";

    /*
     * Minimum confidence required before we
     * consider a sound.
     */
    private static final float THRESHOLD = 0.4f;

    /*
     * Prevent the same sound from repeatedly
     * sending alerts.
     */
    private static final long DEDUP_MS = 8000L;

    /*
     * Number of consecutive detections required
     * before triggering an emergency.
     *
     * The classifier runs every 250 ms.
     *
     * 3 detections = about 750 ms.
     */
    private static final int REQUIRED_DETECTIONS = 3;

    /*
     * Change this to false once you are finished
     * testing YAMNet's labels.
     */
    private static final boolean DEBUG_LOG_ALL_CATEGORIES = true;

    private AudioClassifier classifier;

    private ScheduledExecutorService scheduler;

    private AudioRecord audioRecord;

    private long lastAlertAt = 0L;

    /*
     * Keeps track of the sound currently being detected.
     */
    private String lastDetectedType = null;

    /*
     * Counts how many times that sound has been
     * detected consecutively.
     */
    private int consecutiveDetections = 0;


    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        startForegroundWithNotification();

        startListening();
    }


    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        /*
         * If Android stops the service, ask Android
         * to recreate it when possible.
         */
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    @Override
    public void onDestroy() {

        if (scheduler != null) {

            scheduler.shutdownNow();
        }

        if (audioRecord != null) {

            try {

                audioRecord.stop();

            } catch (Exception ignored) {
            }

            audioRecord.release();

            audioRecord = null;
        }

        if (classifier != null) {

            classifier.close();

            classifier = null;
        }

        super.onDestroy();
    }


    private void startForegroundWithNotification() {

        Intent openIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        openIntent,
                        PendingIntent.FLAG_IMMUTABLE
                );

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "Silent Guardian"
                        )
                        .setContentText(
                                "Listening for emergency sounds"
                        )
                        .setSmallIcon(
                                android.R.drawable.ic_btn_speak_now
                        )
                        .setOngoing(true)
                        .setContentIntent(pendingIntent)
                        .build();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );

        } else {

            startForeground(
                    NOTIF_ID,
                    notification
            );
        }
    }


    private void createNotificationChannel() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Audio Detection",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {

            manager.createNotificationChannel(
                    channel
            );
        }
    }


    private void startListening() {

        try {

            /*
             * Load YAMNet.
             */
            classifier =
                    AudioClassifier.createFromFile(
                            this,
                            MODEL_FILE
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to load YAMNet model",
                    e
            );

            stopSelf();

            return;
        }


        /*
         * Create the audio input.
         */
        TensorAudio tensor =
                classifier.createInputTensorAudio();


        try {

            audioRecord =
                    classifier.createAudioRecord();

            audioRecord.startRecording();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to start microphone",
                    e
            );

            stopSelf();

            return;
        }


        /*
         * Run sound classification repeatedly.
         */
        scheduler =
                Executors.newSingleThreadScheduledExecutor();


        scheduler.scheduleAtFixedRate(
                () -> detectSound(tensor),
                0,
                250,
                TimeUnit.MILLISECONDS
        );
    }


    private void detectSound(
            TensorAudio tensor) {

        try {

            /*
             * Put microphone audio into YAMNet.
             */
            tensor.load(audioRecord);


            /*
             * Ask YAMNet what sound it hears.
             */
            List<Classifications> output =
                    classifier.classify(tensor);


            if (output == null ||
                    output.isEmpty()) {

                return;
            }


            List<Category> categories =
                    output.get(0).getCategories();


            /*
             * Print YAMNet's labels while testing.
             */
            if (DEBUG_LOG_ALL_CATEGORIES) {

                for (Category category : categories) {

                    Log.d(
                            TAG,
                            category.getLabel()
                                    + " = "
                                    + category.getScore()
                    );
                }
            }


            /*
             * Find the strongest dangerous sound.
             */
            String detectedType = null;

            float detectedScore = 0.0f;

            String detectedLabel = null;


            for (Category category : categories) {

                String label =
                        category
                                .getLabel()
                                .toLowerCase();


                float score =
                        category.getScore();


                if (score < THRESHOLD) {

                    continue;
                }


                boolean isFire =
                        label.contains(
                                "smoke detector"
                        )
                                || label.contains(
                                "smoke alarm"
                        )
                                || label.contains(
                                "fire alarm"
                        );


                boolean isGlass =
                        label.contains("glass")
                                || label.contains("shatter");


                boolean isSiren =
                        label.contains("siren")
                                || label.contains(
                                "emergency vehicle"
                        );


                boolean isDoorbell =
                        label.contains(
                                "doorbell"
                        );


                if (isFire) {

                    detectedType =
                            "Smoke Alarm";

                } else if (isGlass) {

                    detectedType =
                            "Breaking Glass";

                } else if (isSiren) {

                    detectedType =
                            "Siren";

                } else if (isDoorbell) {

                    detectedType =
                            "Doorbell";
                }


                /*
                 * Keep the strongest matching sound.
                 */
                if (detectedType != null
                        && score > detectedScore) {

                    detectedScore = score;

                    detectedLabel = label;
                }
            }


            /*
             * Nothing dangerous detected.
             *
             * Reset the consecutive counter.
             */
            if (detectedType == null) {

                lastDetectedType = null;

                consecutiveDetections = 0;

                return;
            }


            /*
             * Is this the same sound we detected
             * during the previous check?
             */
            if (detectedType.equals(
                    lastDetectedType)) {

                consecutiveDetections++;

            } else {

                /*
                 * A different sound started.
                 */
                lastDetectedType =
                        detectedType;

                consecutiveDetections = 1;
            }


            Log.d(
                    TAG,
                    "Potential "
                            + detectedType
                            + " detected. "
                            + "Count="
                            + consecutiveDetections
                            + "/"
                            + REQUIRED_DETECTIONS
                            + " score="
                            + detectedScore
            );


            /*
             * We only trigger the emergency after
             * several consecutive detections.
             */
            if (consecutiveDetections
                    >= REQUIRED_DETECTIONS) {

                long now =
                        System.currentTimeMillis();


                /*
                 * Prevent duplicate alerts.
                 */
                if (now - lastAlertAt
                        >= DEDUP_MS) {

                    lastAlertAt = now;


                    Log.i(
                            TAG,
                            "EMERGENCY SOUND DETECTED: "
                                    + detectedLabel
                                    + " -> "
                                    + detectedType
                                    + " score="
                                    + detectedScore
                    );


                    /*
                     * THIS is where the automatic
                     * alert happens.
                     *
                     * No button is required.
                     */
                    AlertManager.trigger(
                            getApplicationContext(),
                            detectedType
                    );
                }


                /*
                 * Reset after an alert so that the
                 * same continuous sound does not
                 * immediately trigger another alert.
                 */
                consecutiveDetections = 0;
            }


        } catch (Throwable t) {

            Log.w(
                    TAG,
                    "Classification error",
                    t
            );
        }
    }
}