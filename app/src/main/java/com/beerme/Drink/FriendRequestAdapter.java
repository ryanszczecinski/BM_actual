package com.beerme.Drink;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.widget.ImageButton;
import android.widget.TextView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;

public class FriendRequestAdapter extends ArrayAdapter<String> {
    FirebaseFirestore db;
    public FriendRequestAdapter(Context context, ArrayList<String> friendRequests){
        super(context,0,friendRequests);
        db = FirebaseFirestore.getInstance();
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.friend_request_adapter_view,parent,false);
        }
        TextView name = listItemView.findViewById(R.id.FRTV);
        name.setText(getItem(position));
        ImageButton acceptFriend = listItemView.findViewById(R.id.FRaddFriend);
        acceptFriend.setTag(getItem(position));
        ImageButton deleteRequest = listItemView.findViewById(R.id.FRdeleteRequest);
        deleteRequest.setTag(getItem(position));
        View.OnClickListener addFriendListener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final String tag = (String) view.getTag();
                db.runTransaction(new Transaction.Function<Void>() {
                    @Nullable
                    @Override
                    public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                        //must read before write

                        DocumentReference recieverfriendRequestDoc = db.collection(DataBaseString.DB_USERS_COLLECTION).document(FirebaseAuth.getInstance().getCurrentUser().getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
                        DocumentReference recieverfriendsDoc = db.collection(DataBaseString.DB_USERS_COLLECTION).document(FirebaseAuth.getInstance().getCurrentUser().getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIENDS_DOCUMENT);
                        DocumentReference senderfriendsDoc =  db.collection(DataBaseString.DB_USERS_COLLECTION).document(tag).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIENDS_DOCUMENT);

                        DocumentSnapshot snapshot = transaction.get(recieverfriendRequestDoc);
                        DocumentSnapshot friendsSnapshot = transaction.get(recieverfriendsDoc);
                        DocumentSnapshot senderfriendsSnapshot = transaction.get(senderfriendsDoc);


                        ArrayList<String> friendRequests = (ArrayList<String>) snapshot.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                        ArrayList<String> friends = (ArrayList<String>)  friendsSnapshot.get(DataBaseString.DB_FRIENDS_ARRAY);
                        ArrayList<String> senderfriends = (ArrayList<String>) senderfriendsSnapshot.get(DataBaseString.DB_FRIENDS_ARRAY);

                        friends.add(tag);
                        friendRequests.remove(tag);
                        senderfriends.add(FirebaseAuth.getInstance().getCurrentUser().getEmail());

                        transaction.update(recieverfriendRequestDoc,DataBaseString.DB_FRIEND_REQUEST_ARRAY,friendRequests);
                        transaction.update(recieverfriendsDoc,DataBaseString.DB_FRIENDS_ARRAY,friends);
                        transaction.update(senderfriendsDoc,DataBaseString.DB_FRIENDS_ARRAY,senderfriends);
                        return null;
                    }
                });
                remove(tag);
            }
        };
        View.OnClickListener deleteRequestListener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final String tag = (String) view.getTag();
                db.runTransaction(new Transaction.Function<Void>() {
                    @Nullable
                    @Override
                    public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                        DocumentReference friendRequestDoc = db.collection(DataBaseString.DB_USERS_COLLECTION).document(FirebaseAuth.getInstance().getCurrentUser().getEmail()).collection(DataBaseString.DB_FRIENDS_COLLECTION).document(DataBaseString.DB_FRIEND_REQUEST_DOCUMENT);
                        DocumentSnapshot snapshot = transaction.get(friendRequestDoc);
                        ArrayList<String> friendRequests = (ArrayList<String>) snapshot.get(DataBaseString.DB_FRIEND_REQUEST_ARRAY);
                        friendRequests.remove(tag);
                        transaction.update(friendRequestDoc,DataBaseString.DB_FRIEND_REQUEST_ARRAY,friendRequests);
                        return null;
                    }
                });
                remove((String)view.getTag());
            }
        };
        acceptFriend.setOnClickListener(addFriendListener);
        deleteRequest.setOnClickListener(deleteRequestListener);
        return listItemView;
    }

}
