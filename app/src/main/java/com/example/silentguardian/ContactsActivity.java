package com.example.silentguardian;

import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private List<Contact> contacts;
    private ContactAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        contacts = ContactStorage.load(this);

        RecyclerView recyclerView = findViewById(R.id.contactsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ContactAdapter(contacts, position -> {
            contacts.remove(position);
            ContactStorage.save(this, contacts);
            adapter.notifyDataSetChanged();
        });
        recyclerView.setAdapter(adapter);

        EditText nameInput = findViewById(R.id.nameInput);
        EditText phoneInput = findViewById(R.id.phoneInput);
        Button addButton = findViewById(R.id.addContactButton);

        addButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Enter a name and phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            contacts.add(new Contact(name, phone));
            ContactStorage.save(this, contacts);
            adapter.notifyDataSetChanged();

            nameInput.setText("");
            phoneInput.setText("");
        });
    }
}