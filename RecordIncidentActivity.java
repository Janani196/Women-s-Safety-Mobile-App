package com.example.my;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecordIncidentActivity extends AppCompatActivity {

    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private StorageReference storageRef;
    private ListView listView;
    private ArrayList<FileItem> fileItems;
    private FileListAdapter adapter;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_incident);

        // Initialize Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize ListView and Adapter
        listView = findViewById(R.id.file_list_view);
        fileItems = new ArrayList<>();
        adapter = new FileListAdapter(this, fileItems);
        listView.setAdapter(adapter);

        // Add "Start Recording" button functionality
        ImageButton startRecordingButton = findViewById(R.id.start_recording_button);
        startRecordingButton.setOnClickListener(v -> openCameraForRecording());

        // Set item click listener for the ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileItem item = fileItems.get(position);
            openMedia(item.getUrl());
        });

        // Load existing files
        loadFiles();
    }

    private void openCameraForRecording() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        } else {
            Toast.makeText(this, "No app available to record video", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_CAPTURE && data != null) {
                fileUri = data.getData();
                if (fileUri != null) {
                    uploadFile(fileUri);
                } else {
                    Toast.makeText(this, "Error: File URI is null", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                fileUri = data.getData();
                uploadFile(fileUri);
            }
        }
    }

    private void uploadFile(Uri fileUri) {
        if (fileUri != null) {
            // Create a reference to the file with the format "yearmonthdate.mp4"
            String fileName = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date()) + ".mp4";
            StorageReference fileRef = storageRef.child("media/" + fileName);

            // Upload the file
            UploadTask uploadTask = fileRef.putFile(fileUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(RecordIncidentActivity.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                loadFiles(); // Refresh the file list
            }).addOnFailureListener(exception -> {
                Toast.makeText(RecordIncidentActivity.this, "Upload failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Error: No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFiles() {
        storageRef.child("media").listAll().addOnSuccessListener(listResult -> {
            fileItems.clear();
            for (StorageReference item : listResult.getItems()) {
                String name = item.getName();
                item.getDownloadUrl().addOnSuccessListener(url -> {
                    fileItems.add(new FileItem(name, url.toString()));
                    adapter.notifyDataSetChanged();
                }).addOnFailureListener(exception -> {
                    Toast.makeText(RecordIncidentActivity.this, "Failed to get file URL: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(exception -> {
            Toast.makeText(RecordIncidentActivity.this, "Failed to load files: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void openMedia(String url) {
        try {
            // Create an intent to view the video file
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Set the data and type to point to the video file
            intent.setDataAndType(Uri.parse(url), "video/mp4");

            // Grant permission to the intent to read the Uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start the activity to open the video
            startActivity(intent);
        } catch (Exception e) {
            // Show an error message if the video cannot be opened
            Toast.makeText(this, "Cannot open video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteFile(FileItem fileItem) {
        // Create a reference to the file
        StorageReference fileRef = storageRef.child("media/" + fileItem.getName());

        // Delete the file
        fileRef.delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();
            loadFiles(); // Refresh the file list
        }).addOnFailureListener(exception -> {
            Toast.makeText(this, "Delete failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
