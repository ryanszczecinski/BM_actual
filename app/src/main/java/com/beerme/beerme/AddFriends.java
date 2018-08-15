package com.beerme.beerme;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;

public class AddFriends extends AppCompatActivity {
private FirebaseAuth mAuth;
private FirebaseUser mUser;
private FirebaseFirestore db;
private FriendRequestAdapter adapter;
private ArrayList<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        if (mUser!=null){
            getSupportActionBar().setTitle(mUser.getEmail());
            dbInteractions();
        }
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //Query for friend requests
        DocumentReference documentReference = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mAuth.getCurrentUser().getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        list = (ArrayList<String>) document.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                        ListView listView = findViewById(R.id.addFriendLV);
                        adapter = new FriendRequestAdapter(getApplicationContext(),list);
                        listView.setAdapter(adapter);
                    }
                    else list = new ArrayList<String>();
                }
                else list = new ArrayList<String>();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            this.startActivity(i);
            Log.v("settings", "settings selected");
            return true;
        } else if (id == R.id.Disclaimer) {
            new MainActivity.Disclaimer().show(getSupportFragmentManager(), "Disclaimer");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void dbInteractions(){
        DocumentReference friends = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mUser.getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
        friends.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>(){
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("FriendsQuery", "DocumentSnapshot data: " + document.getData());
                        //TODO: put the array into an arrayadapter and attach a list view to it
                        ArrayList<String> requests = (ArrayList<String>)document.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                    } else {
                        Log.d("FriendsQuery", "No such document");
                    }
                }
            }
        });
        friends.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("FriendsSnapShot", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d("FriendsSnapShot", "Current data: " + snapshot.getData());
                    //TODO: add data to the arraylist adapter
                } else {
                    Log.d("FriendsSnapShot", "Current data: null");
                }
            }
        });
    }
}