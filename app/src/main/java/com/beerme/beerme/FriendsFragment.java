package com.beerme.beerme;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class FriendsFragment extends Fragment {
private FirebaseAuth mAuth;
private FirebaseUser mUser;
private TextView friendsTV;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

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
        if(mAuth.getCurrentUser()!=null)friendsTV.setText(mAuth.getCurrentUser().getDisplayName());
    }
}
