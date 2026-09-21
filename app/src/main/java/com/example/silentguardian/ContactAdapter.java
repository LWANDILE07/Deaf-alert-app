package com.example.silentguardian;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder>{

    public interface OnDeleteClickListener {
        void onDelete(int position);
    }

    private final List<Contact> contacts;
    private final OnDeleteClickListener listener;

    public ContactAdapter(List<Contact> contacts, OnDeleteClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.name.setText(contact.name);
        holder.phone.setText(contact.phoneNumber);
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, phone;
        android.widget.Button deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contactName);
            phone = itemView.findViewById(R.id.contactPhone);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
