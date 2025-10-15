package com.example.my;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddContactActivity extends AppCompatActivity {

    private ContactAdapter contactAdapter;
    private List<Contact> contactList;
    private DatabaseReference databaseReference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("contacts");

        // Initialize RecyclerView and set its layout manager
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize contact list and adapter
        contactList = new ArrayList<>();
        contactAdapter = new ContactAdapter(contactList);
        recyclerView.setAdapter(contactAdapter);

        // Load saved contacts from Firebase
        loadContactsFromFirebase();

        // FloatingActionButton to add contacts
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(view -> showAddContactDialog());
    }


    // Method to display dialog for adding a contact
    private void showAddContactDialog() {
        // Create a dialog to input contact information
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);

        EditText editTextName = dialogView.findViewById(R.id.editTextName);
        EditText editTextPhone = dialogView.findViewById(R.id.editTextPhone);

        builder.setTitle("Add Contact");
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = editTextName.getText().toString().trim();
            String phone = editTextPhone.getText().toString().trim();

            // Validate input fields
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                Toast.makeText(AddContactActivity.this, "Please enter both name and phone", Toast.LENGTH_SHORT).show();
            } else {
                // Add contact to Firebase
                Contact contact = new Contact(name, phone);
                addContactToFirebase(contact);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // Method to add a contact to Firebase
    private void addContactToFirebase(Contact contact) {
        databaseReference.push().setValue(contact).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(AddContactActivity.this, "Contact added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddContactActivity.this, "Failed to add contact", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Adapter for the RecyclerView
    private class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

        private final List<Contact> contacts;

        ContactAdapter(List<Contact> contacts) {
            this.contacts = contacts;
        }

        @NonNull
        @Override
        public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
            return new ContactViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
            Contact contact = contacts.get(position);
            holder.bind(contact);

            // Set listeners for Edit and Delete buttons
            holder.buttonEdit.setOnClickListener(v -> showEditContactDialog(contact, position));
            holder.buttonDelete.setOnClickListener(v -> {
                deleteContactFromFirebase(contact);
                deleteContact(position);
                Toast.makeText(AddContactActivity.this, "Contact deleted", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        // ViewHolder class for the contact item
        class ContactViewHolder extends RecyclerView.ViewHolder {

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

        // Method to show a dialog for editing a contact
        private void showEditContactDialog(Contact contact, int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(AddContactActivity.this);
            View dialogView = LayoutInflater.from(AddContactActivity.this).inflate(R.layout.dialog_add_contact, null);
            builder.setView(dialogView);

            EditText editTextName = dialogView.findViewById(R.id.editTextName);
            EditText editTextPhone = dialogView.findViewById(R.id.editTextPhone);

            // Pre-fill the input fields with the existing contact data
            editTextName.setText(contact.getName());
            editTextPhone.setText(contact.getPhone());

            builder.setTitle("Edit Contact");
            builder.setPositiveButton("Update", (dialog, which) -> {
                String newName = editTextName.getText().toString().trim();
                String newPhone = editTextPhone.getText().toString().trim();

                if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newPhone)) {
                    Toast.makeText(AddContactActivity.this, "Please enter both name and phone", Toast.LENGTH_SHORT).show();
                } else {
                    updateContactInFirebase(contact, newName, newPhone);
                    updateContact(contact, position, newName, newPhone);
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        }
    }
    private void loadContactsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                contactList.clear(); // Clear the list before loading new data
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Contact contact = snapshot.getValue(Contact.class);
                    if (contact != null) {
                        contactList.add(contact);
                    }
                }
                // Notify the adapter that data has changed
                contactAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddContactActivity.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Method to update a contact in the list and notify the adapter
    private void updateContact(Contact contact, int position, String name, String phone) {
        contact.setName(name);
        contact.setPhone(phone);
        contactAdapter.notifyItemChanged(position);
    }

    // Method to update a contact in Firebase
    private void updateContactInFirebase(Contact contact, String name, String phone) {
        // This will only work if we uniquely identify the contact in Firebase, which means you need to modify the data structure
        // If you store contacts without an ID, updating specific records becomes difficult
        databaseReference.orderByChild("phone").equalTo(contact.getPhone()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    snapshot.getRef().child("name").setValue(name);
                    snapshot.getRef().child("phone").setValue(phone);
                }
            }
        });
    }

    // Method to delete a contact from Firebase
    private void deleteContactFromFirebase(Contact contact) {
        databaseReference.orderByChild("phone").equalTo(contact.getPhone()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    snapshot.getRef().removeValue();
                }
            }
        });
    }

    // Method to delete a contact from the list
    private void deleteContact(int position) {
        contactList.remove(position);
        contactAdapter.notifyItemRemoved(position);
    }
}
