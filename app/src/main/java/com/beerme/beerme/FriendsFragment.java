package com.beerme.beerme;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
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
private ArrayList<String> friendsEmails;
private FriendsListAdapter adapter;
private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        friendsEmails = new ArrayList<>();
        adapter = new FriendsListAdapter(getContext(),new ArrayList<DrinkingFriend>());

        }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.fragment_friends, container, false);
        friendsTV = v.findViewById(R.id.friendsText);
        listView = v.findViewById(R.id.friendsLV);
        listView.setAdapter(adapter);
        if(mAuth.getCurrentUser()!=null){
            dbInteractions();
        }

        return v;
    }

    @Override
    public void onStart(){
        super.onStart();
        mUser = mAuth.getCurrentUser();
        if(mAuth.getCurrentUser()!=null){
            friendsTV.setText(mAuth.getCurrentUser().getDisplayName());
        }
    }
    private void dbInteractions(){
        //initial query for friends in personal array
        DocumentReference friends = db.collection(DataBaseString.DB_USERS_COLLECTION).document(mUser.getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIENDS_DOCUMENT);
        friends.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>(){
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("FriendsQuery", "DocumentSnapshot data: " + document.getData());
                        final ArrayList<String> friendsArray = (ArrayList<String>)document.get(DataBaseString.DB_FRIENDS_ARRAY);
                        //in this for loop we loop over every friend to see their drinking status
                        for(int i = 0; i< friendsArray.size();i++){
                            final int index = i;
                            DocumentReference friendDrinkingRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(friendsArray.get(i)).collection(DataBaseString.DB_PARTY_COLLECTION).document(DataBaseString.DB_DRINKING_DOCUMENT);
                            friendDrinkingRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()){
                                        DocumentSnapshot snapshot = task.getResult();
                                        if(snapshot.exists()){
                                            //create a drinkingFriend object and add it to the adapter
                                            boolean isDrinking = (boolean)snapshot.get(DataBaseString.DB_IS_DRINKING);
                                            int numDrinks = Math.toIntExact((long)snapshot.get(DataBaseString.DB_NUMBER_OF_DRINKS));
                                            String username = (String)snapshot.get(DataBaseString.DB_USERNAME);
                                            DrinkingFriend df = new DrinkingFriend(username,isDrinking,numDrinks);
                                            if(adapter.getPosition(df)==-1)adapter.add(df);
                                            friendsEmails.add(friendsArray.get(index));
                                            }
                                    }
                                }
                            });
                            //used to listen for changes to their drinking document
                            friendDrinkingRef.addSnapshotListener(getActivity(), new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if(e != null){
                                        Log.w("FriendsSnapShot", "Listen failed.", e);
                                        return;
                                    }

                                    if(documentSnapshot!=null && documentSnapshot.exists()){
                                        boolean isDrinking = (boolean)documentSnapshot.get(DataBaseString.DB_IS_DRINKING);
                                        int numDrinks = Math.toIntExact((long)documentSnapshot.get(DataBaseString.DB_NUMBER_OF_DRINKS));
                                        String username = (String)documentSnapshot.get(DataBaseString.DB_USERNAME);
                                        DrinkingFriend df = new DrinkingFriend(username,isDrinking,numDrinks);
                                        int position = adapter.getPosition(df);
                                        DrinkingFriend drinkingFriend = df;
                                        if(position>=0) {
                                             drinkingFriend = adapter.getItem(position);
                                        }
                                        drinkingFriend.setIsDrinking(isDrinking);
                                        drinkingFriend.setNumberOfDrinks(numDrinks);
                                        adapter.sort(new DrinkingFriendComparator());
                                    }
                                    else{
                                        Log.d("FriendsSnapShot", "Current data: null");
                                    }
                                }
                            });
                        }
                    } else {
                        Log.d("FriendsQuery", "No such document");
                    }
                }
            }
        });
        //when a new friend is added, this will update the friends page
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
                    //compare the emails
                    final ArrayList<String> friendsArray = (ArrayList<String>) snapshot.get(DataBaseString.DB_FRIENDS_ARRAY);
                    for(int i = 0; i<friendsArray.size();i++){
                        //the current adapter does not contain the friend, must add it
                        if(!friendsEmails.contains(friendsArray.get(i))){
                            //must query to get the new friends information
                            final int index = i;
                            DocumentReference friendDrinkingRef = db.collection(DataBaseString.DB_USERS_COLLECTION).document(friendsArray.get(i)).collection(DataBaseString.DB_PARTY_COLLECTION).document(DataBaseString.DB_DRINKING_DOCUMENT);
                            friendDrinkingRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()){
                                        DocumentSnapshot snapshot = task.getResult();
                                        if(snapshot.exists()){
                                            //create a drinkingFriend object and add it to the adapter
                                            boolean isDrinking = (boolean)snapshot.get(DataBaseString.DB_IS_DRINKING);
                                            int numDrinks = Math.toIntExact((long)snapshot.get(DataBaseString.DB_NUMBER_OF_DRINKS));
                                            String username = (String)snapshot.get(DataBaseString.DB_USERNAME);
                                            DrinkingFriend df = new DrinkingFriend(username,isDrinking,numDrinks);
                                            if(adapter.getPosition(df)==-1)adapter.add(df);
                                            friendsEmails.add(friendsArray.get(index));
                                        }
                                    }
                                }
                            });
                            friendDrinkingRef.addSnapshotListener(getActivity(), new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                    if(e != null){
                                        Log.w("FriendsSnapShot", "Listen failed.", e);
                                        return;
                                    }

                                    if(documentSnapshot!=null && documentSnapshot.exists()){
                                        boolean isDrinking = (boolean)documentSnapshot.get(DataBaseString.DB_IS_DRINKING);
                                        int numDrinks = Math.toIntExact((long)documentSnapshot.get(DataBaseString.DB_NUMBER_OF_DRINKS));
                                        String username = (String)documentSnapshot.get(DataBaseString.DB_USERNAME);
                                        DrinkingFriend df = new DrinkingFriend(username,isDrinking,numDrinks);
                                        int position = adapter.getPosition(df);
                                        DrinkingFriend drinkingFriend = df;
                                        if(position>=0) {
                                            drinkingFriend = adapter.getItem(position);
                                        }
                                        drinkingFriend.setIsDrinking(isDrinking);
                                        drinkingFriend.setNumberOfDrinks(numDrinks);
                                        adapter.sort(new DrinkingFriendComparator());
                                    }
                                    else{
                                        Log.d("FriendsSnapShot", "Current data: null");
                                    }
                                }
                            });

                        }
                    }
                } else {
                    Log.d("FriendsSnapShot", "Current data: null");
                }
            }
        });
    }
}
