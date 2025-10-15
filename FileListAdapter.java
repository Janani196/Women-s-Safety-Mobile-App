package com.example.my;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

public class FileListAdapter extends ArrayAdapter<FileItem> {

    private final Context context;
    private final ArrayList<FileItem> fileItems;

    public FileListAdapter(Context context, ArrayList<FileItem> fileItems) {
        super(context, R.layout.file_item, fileItems);
        this.context = context;
        this.fileItems = fileItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.file_item, parent, false);
        }

        FileItem fileItem = fileItems.get(position);

        TextView fileNameTextView = convertView.findViewById(R.id.file_name_text_view);
        fileNameTextView.setText(fileItem.getName());

        convertView.setOnLongClickListener(v -> {
            showDeleteConfirmationDialog(fileItem);
            return true;
        });

        return convertView;
    }

    private void showDeleteConfirmationDialog(FileItem fileItem) {
        new AlertDialog.Builder(context)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFile(fileItem))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFile(FileItem fileItem) {
        // Notify the activity to delete the file
        ((RecordIncidentActivity) context).deleteFile(fileItem);
    }
}
