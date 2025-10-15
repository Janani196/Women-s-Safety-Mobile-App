package com.example.my;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_SMS_PERMISSION = 201;
    private static final int REQUEST_LOCATION_PERMISSION = 202;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference contactsDatabaseReference;
    private ImageButton addContactButton, panicButton, fakeCallButton, trackMeButton, recordIncidentButton, selfDefenseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Women's Safety");

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        contactsDatabaseReference = FirebaseDatabase.getInstance().getReference("contacts");

        if (user == null) {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        } else {
            checkLocationSettings();
            loadMainContent();
            sendLocationToContacts();  // Send location on app entry
        }
    }

    private void loadMainContent() {
        addContactButton = findViewById(R.id.add_contact_button);
        panicButton = findViewById(R.id.panic_button);
        fakeCallButton = findViewById(R.id.fake_call_button);
        trackMeButton = findViewById(R.id.track_me_button);
        recordIncidentButton = findViewById(R.id.record_incident_button);
        selfDefenseButton = findViewById(R.id.self_defense_button);

        addContactButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddContactActivity.class);
            startActivity(intent);
        });

        panicButton.setOnClickListener(v -> {
            if (checkSMSPermission() && checkLocationPermission()) {
                sendPanicAlerts();
            } else {
                requestPermissions();
            }
        });

        fakeCallButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FakeCallActivity.class);
            startActivity(intent);
        });

        trackMeButton.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                Intent intent = new Intent(MainActivity.this, TrackMeActivity.class);
                startActivity(intent);
            } else {
                requestLocationPermission();
            }
        });

        recordIncidentButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                Intent intent = new Intent(MainActivity.this, RecordIncidentActivity.class);
                startActivity(intent);
            } else {
                requestPermissions();
            }
        });

        selfDefenseButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelfDefenseActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
    }

    private boolean checkSMSPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSMSPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MainActivity.this, RecordIncidentActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Camera and storage permissions are required to record incidents.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "SMS permission is required to send panic messages.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Location permission is required to track your location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // Location settings are OK, no need to navigate to TrackMeActivity here
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Toast.makeText(MainActivity.this, "Location settings are not satisfied.", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        });
    }

    private void sendLocationToContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    String locationUri = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();

                    contactsDatabaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Contact contact = snapshot.getValue(Contact.class);
                                if (contact != null) {
                                    sendSms(contact.getPhone(), "Here's my location: " + locationUri);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(MainActivity.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendPanicAlerts() {
        if (checkLocationPermission() && checkSMSPermission()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    String locationUri = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                    fetchContactsAndSendSms(locationUri);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "Permissions are required to send panic alerts.", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchContactsAndSendSms(String locationUri) {
        contactsDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Contact contact = snapshot.getValue(Contact.class);
                    if (contact != null) {
                        sendSms(contact.getPhone(), "Panic Alert! Here's my location: " + locationUri);
                    }
                }
                // Toast message after sending all SMS messages
                Toast.makeText(MainActivity.this, "Panic alerts sent to all contacts.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSms(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }
}
