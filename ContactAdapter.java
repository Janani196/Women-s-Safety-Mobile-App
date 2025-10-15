package com.example.my;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private final List<Contact> contacts;
    private final Context context;

    ContactAdapter(List<Contact> contacts, Context context) {
        this.contacts = contacts;
        this.context = context;
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.bind(contact);

        holder.buttonEdit.setOnClickListener(v -> showEditContactDialog(contact, position));

        holder.buttonDelete.setOnClickListener(v -> {
            contacts.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, contacts.size());
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView phoneView;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;

        ContactViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.contactName);
            phoneView = itemView.findViewById(R.id.contactPhone);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        void bind(Contact contact) {
            nameView.setText(contact.getName());
            phoneView.setText(contact.getPhone());
        }
    }

    private void showEditContactDialog(Contact contact, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);

        EditText editTextName = dialogView.findViewById(R.id.editTextName);
        EditText editTextPhone = dialogView.findViewById(R.id.editTextPhone);

        editTextName.setText(contact.getName());
        editTextPhone.setText(contact.getPhone());

        builder.setTitle("Edit Contact");
        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = editTextName.getText().toString();
            String phone = editTextPhone.getText().toString();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                Toast.makeText(context, "Please enter both name and phone", Toast.LENGTH_SHORT).show();
            } else {
                contact.setName(name);
                contact.setPhone(phone);
                notifyItemChanged(position);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
