package com.example.my;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FakeCallActivity extends Activity {

    private Ringtone ringtone;
    private TextView callerNameTextView, callDurationTextView;
    private Button answerButton, declineButton, endCallButton;
    private boolean isCallActive = false;
    private Handler handler = new Handler();
    private int callDurationSeconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        // Initialize the UI elements
        callerNameTextView = findViewById(R.id.caller_name);
        callDurationTextView = findViewById(R.id.call_duration);
        answerButton = findViewById(R.id.answer_button);
        declineButton = findViewById(R.id.decline_button);
        endCallButton = findViewById(R.id.end_call_button);

        // Get the fake caller name from the intent
        String callerName = getIntent().getStringExtra("CALLER_NAME");
        if (callerName != null) {
            callerNameTextView.setText(callerName);
        }

        // Get the default ringtone URI
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        // Play the default ringtone
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        ringtone.play();

        // Handle the answer button click
        answerButton.setOnClickListener(v -> {
            if (ringtone.isPlaying()) {
                ringtone.stop();
            }
            startFakeCall();
        });

        // Handle the decline button click
        declineButton.setOnClickListener(v -> endFakeCall());

        // Handle the end call button click
        endCallButton.setOnClickListener(v -> endFakeCall());

        // Automatically finish the activity after 30 seconds if not answered
        new Handler().postDelayed(() -> {
            if (!isCallActive && ringtone.isPlaying()) {
                ringtone.stop();
                finish();
            }
        }, 30000); // 30 seconds delay
    }

    private void startFakeCall() {
        isCallActive = true;
        answerButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);
        endCallButton.setVisibility(View.VISIBLE);
        callDurationTextView.setVisibility(View.VISIBLE);

        // Start the call duration timer
        handler.postDelayed(callDurationRunnable, 1000);
    }

    private void endFakeCall() {
        isCallActive = false;
        handler.removeCallbacks(callDurationRunnable);
        finish();
    }

    // Runnable to update call duration
    private final Runnable callDurationRunnable = new Runnable() {
        @Override
        public void run() {
            callDurationSeconds++;
            callDurationTextView.setText(formatDuration(callDurationSeconds));
            handler.postDelayed(this, 1000); // Update every second
        }
    };

    // Helper method to format duration in minutes and seconds
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        handler.removeCallbacks(callDurationRunnable);
    }
}
