package com.beerme.beerme;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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




public class FriendsFragment extends Fragment {
private FirebaseAuth mAuth;
private FirebaseUser mUser;
private TextView friendsTV;
private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.fragment_friends, container, false);
        friendsTV = v.findViewById(R.id.friendsText);


        return v;
    }

    @Override
    public void onStart(){
        super.onStart();
        mUser = mAuth.getCurrentUser();
        if(mAuth.getCurrentUser()!=null){
            friendsTV.setText(mAuth.getCurrentUser().getDisplayName());
            dbInteractions();
        }
    }
    private void dbInteractions(){
        DocumentReference friends = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mUser.getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIENDS_DOCUMENT);
        friends.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>(){
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("FriendsQuery", "DocumentSnapshot data: " + document.getData());
                        //TODO: put the array into an arrayadapter and attach a list view to it
                        ArrayList<String> friendsArray = (ArrayList<String>)document.get(DataBaseString.DB_FRIENDS_ARRAY);
                    } else {
                        Log.d("FriendsQuery", "No such document");
                    }
                }
            }
        });
        friends.addSnapshotListener(getActivity(),new EventListener<DocumentSnapshot>() {
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
