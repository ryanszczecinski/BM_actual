package com.beerme.beerme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.beerme.beerme.DataBaseString.DB_DRINKING_DOCUMENT;
import static com.beerme.beerme.DataBaseString.DB_IS_DRINKING;
import static com.beerme.beerme.DataBaseString.DB_NUMBER_OF_DRINKS;
import static com.beerme.beerme.DataBaseString.DB_PARTY_COLLECTION;

public class AddFriends extends AppCompatActivity {
private FirebaseAuth mAuth;
private FirebaseUser mUser;
private FirebaseFirestore db;
private FriendRequestAdapter adapter;
private ArrayList<String> list;
private EditText findFriend;
private int RC_SIGN_IN =  122;
private ArrayList<String> friends;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        adapter = new FriendRequestAdapter(this, new ArrayList<String>());
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        friends = new ArrayList<>();
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
        findFriend = findViewById(R.id.AddFriendEditText);
        findFriend.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(mUser!= null) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        //hides the keyboard
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        //Query for that name
                        DocumentReference friendsRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(findFriend.getText().toString()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIENDS_DOCUMENT);
                        friendsRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    DocumentSnapshot snapshot = task.getResult();
                                    if(snapshot.exists()){
                                        friends =(ArrayList<String>) snapshot.get(DataBaseString.DB_FRIENDS_ARRAY);
                                    }

                                }
                            }
                        });
                        db.runTransaction(new Transaction.Function<Void>() {
                            @Nullable
                            @Override
                            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                                Log.v("transaction", "ran");
                                DocumentReference friendRequestRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(findFriend.getText().toString()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
                                DocumentSnapshot friendRequestSnap = transaction.get(friendRequestRef);
                                Log.v("transaction", "ran"+1);
                                if (friendRequestSnap.exists()) {
                                    //entered correctly
                                    Log.v("transaction", "ran"+2);
                                    ArrayList<String> friendRequests = (ArrayList<String>) friendRequestSnap.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                                    //will add only if not already a friend and not in requests
                                    if(!friendRequests.contains(mUser.getEmail())&&!friends.contains(mUser.getEmail())){
                                        friendRequests.add(mUser.getEmail());
                                        //will save an extra write i guess

                                    }
                                    transaction.update(friendRequestRef, DataBaseString.DB_FRIEND_REQUEST_ARRAY, friendRequests);


                                }
                                else{
                                    Log.v("transaction", "DNE");
                                }
                                return null;
                            }
                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void v) {
                                Toast.makeText(getApplicationContext(), "Request Sent", Toast.LENGTH_LONG).show();
                            }
                        })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(), "User Not Found", Toast.LENGTH_LONG).show();
                                        Log.e("transaction","shit failed lol",e);
                                    }
                                });
                        return true;
                    }
                }
                return false;
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
        else if(id ==R.id.SignOut){
            if(mUser !=null){
                stopDrinkingInDB();
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                auth();
                            }
                        });
            }
        }
        return super.onOptionsItemSelected(item);
    }
    private void auth(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    private void dbInteractions(){
        DocumentReference friendRequests = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mUser.getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
        friendRequests.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>(){
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("FriendsQuery", "DocumentSnapshot data: " + document.getData());
                        //TODO: put the array into an arrayadapter and attach a list view to it
                        list = (ArrayList<String>) document.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                        ListView listView = findViewById(R.id.addFriendLV);
                        adapter = new FriendRequestAdapter(getApplicationContext(),list);
                        listView.setAdapter(adapter);
                    } else {
                        Log.d("FriendsQuery", "No such document");
                    }
                }
            }
        });
        friendRequests.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("FriendsSnapShot", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d("FriendsSnapShot", "Current data: " + snapshot.getData());
                    list = (ArrayList<String>) snapshot.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                    ListView listView = findViewById(R.id.addFriendLV);
                    adapter = new FriendRequestAdapter(getApplicationContext(),list);
                    listView.setAdapter(adapter);

                } else {
                    Log.d("FriendsSnapShot", "Current data: null");
                }
            }
        });
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mAuth = FirebaseAuth.getInstance();
                mUser = mAuth.getCurrentUser();
                if (mUser!=null){
                    getSupportActionBar().setTitle(mUser.getEmail());
                }
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                DocumentReference docRef = db.collection("users").document(user.getEmail());
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d("Query", "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d("Query", "No such document creating one");
                                WriteBatch batch = db.batch();
                                Map<String, Object> userVal = new HashMap<>();
                                userVal.put(DataBaseString.DB_USERNAME,mAuth.getCurrentUser().getDisplayName());
                                DocumentReference userRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mAuth.getCurrentUser().getEmail());
                                batch.set(userRef,userVal);

                                DocumentReference drinkingRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mAuth.getCurrentUser().getEmail()).collection(DataBaseString.DB_PARTY_COLLECTION).document(DataBaseString.DB_DRINKING_DOCUMENT);                                Map<String, Object> drinkingVal = new HashMap<>();
                                drinkingVal.put(DataBaseString.DB_USERNAME,mAuth.getCurrentUser().getDisplayName());
                                drinkingVal.put(DataBaseString.DB_IS_DRINKING,false);
                                drinkingVal.put(DataBaseString.DB_NUMBER_OF_DRINKS,0);
                                batch.set(drinkingRef,drinkingVal);

                                CollectionReference friendCollectionRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mAuth.getCurrentUser().getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION);
                                DocumentReference friendRef = friendCollectionRef.document(DataBaseString.DB_FRIENDS_DOCUMENT);
                                DocumentReference requestRef = friendCollectionRef.document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
                                Map<String, Object> friendsVal = new HashMap<>();
                                Map<String, Object> friendRequestVal = new HashMap<>();
                                friendsVal.put(DataBaseString.DB_FRIENDS_ARRAY,new ArrayList<String>());
                                friendRequestVal.put(DataBaseString.DB_FRIEND_REQUEST_ARRAY,new ArrayList<String>());
                                batch.set(friendRef,friendsVal);
                                batch.set(requestRef,friendRequestVal);
                                batch.commit();

                            }
                        } else {
                            Log.d("Query", "get failed with ", task.getException());
                        }
                    }
                });
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                if (response == null) auth();
            }
        }
    }
    private void stopDrinkingInDB(){
        WriteBatch batch = db.batch();
        DocumentReference userDoc = db.collection("users").document(mAuth.getCurrentUser().getEmail()).collection(DB_PARTY_COLLECTION).document(DB_DRINKING_DOCUMENT);
        batch.update(userDoc,DB_IS_DRINKING, false);
        batch.update(userDoc,DB_NUMBER_OF_DRINKS, 0);
        batch.commit();
    }
}