package com.beerme.beerme;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("MainActiviy", "oncreate");
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser()==null) auth();
        SharedPreferences sharedPreferences = getSharedPreferences(SettingsActivity.PREFERENCES,MODE_PRIVATE);
        if(!sharedPreferences.contains(SettingsActivity.WEIGHT)){
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);

        }
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager =  findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);



        TabLayout tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        }
        else if(id == R.id.Disclaimer){
            new Disclaimer().show(getSupportFragmentManager(),"Disclaimer");
            return true;
        }
        else if(id == R.id.addFriend){
            //TODO: create the intent that starts the add friends page
            Intent i = new Intent(this, AddFriends.class);
            this.startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position){
                case 0: return new Drinking_fragment();

                case 1: return new FriendsFragment();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.

            return 2;
        }

        @Override
        public String getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Drinking";
                case 1:
                    return "Friends";
                default:
                    return null;
            }
        }
    }
    public static class Disclaimer extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.Full_Disclaimer)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
            return builder.create();
        }
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


    @Override
    /**
     * when the activity completes, it checks to see if there is already a user account in the
     * database with that email, if there is, then nothing happends, if there isnt then it creates
     * a new document with basic data, isnt drinking, no friends and no requests
     *
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
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

}
