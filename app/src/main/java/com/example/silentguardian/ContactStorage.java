package com.example.silentguardian;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ContactStorage {

    private static final String PREFS_NAME = "silent_guardian_prefs";
    private static final String KEY_CONTACTS = "contacts_json";

    public static List<Contact> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CONTACTS, "[]");
        List<Contact> contacts = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                contacts.add(new Contact(obj.getString("name"), obj.getString("phone")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public static void save(Context context, List<Contact> contacts) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        try {
            for (Contact c : contacts) {
                JSONObject obj = new JSONObject();
                obj.put("name", c.name);
                obj.put("phone", c.phoneNumber);
                array.put(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefs.edit().putString(KEY_CONTACTS, array.toString()).apply();
    }
}
