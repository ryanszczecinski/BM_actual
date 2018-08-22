package com.beerme.beerme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.beerme.beerme.DataBaseString.DB_DRINKING_DOCUMENT;
import static com.beerme.beerme.DataBaseString.DB_IS_DRINKING;
import static com.beerme.beerme.DataBaseString.DB_NUMBER_OF_DRINKS;
import static com.beerme.beerme.DataBaseString.DB_PARTY_COLLECTION;
import static com.beerme.beerme.DataBaseString.DB_USERS_COLLECTION;

public class DrinkingService extends Service {
    private NotificationManager nm;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Timer timer = new Timer();
    private int counter = 0,BAC = 0, beginningOfIntervalTime = 0,beginningOfIntervalDrinks = 0, numberOfDrinks = 0;
    private static boolean isRunning = false;
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    // Holds last value set by a client.
    static final int MSG_REGISTER_CLIENT = 1, MSG_ADD_BEER = 2,MSG_REMOVE_BEER = 3,MSG_BAC = 4,MSG_UNREGISTER_CLIENT = 5,MSG_CLOCK = 6,
            MSG_PREVIOUS = 7,MSG_GET_CURRENT_STATUS = 8,MSG_REMOVE_FROM_FOREGROUND = 9, MSG_SET_TIME=10;
    static final String ACTION_START_THIS_BITCH_UP = "Start";
    static final String ACTION_ADD_BEER = "Add Beer";
    static final String ACTION_EXIT = "Exit";
    static final String ACTION_REMOVE_BEER = "Remove Beer";
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /*this is used for communication between the service and the activity
       each int code is used by the activity to do something different
       uses the message class to store data
     */
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_ADD_BEER:
                    numberOfDrinks +=1;
                    updateBAC();
                    sendBACMessageToUI();
                    pushNotification();
                    updateDrinksInDB();
                    break;
                case MSG_REMOVE_BEER:
                    if(numberOfDrinks>0) numberOfDrinks -=1;
                    updateBAC();
                    sendBACMessageToUI();
                    pushNotification();
                    updateDrinksInDB();
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_GET_CURRENT_STATUS:
                    sendBACMessageToUI();
                    break;
                case MSG_PREVIOUS:
                    sendPreviousMessageToUI();
                    break;
                case MSG_REMOVE_FROM_FOREGROUND:
                    stopForeground(true);
                    break;
                case MSG_SET_TIME:
                    counter = msg.arg1*3600;
                    updateBAC();
                    pushNotification();
                default:
                    super.handleMessage(msg);
            }
        }
    }
    //this method will send a message to the UI containing the BAC and the number of drinks
    private void sendBACMessageToUI() {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send data as an Integer to be divided by 100 to get BAC value
                //second arg is the number of drinks
                mClients.get(i).send(Message.obtain(null, MSG_BAC, BAC, numberOfDrinks));

                }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    private void sendPreviousMessageToUI() {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_PREVIOUS));
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    private void sendUnbindMessageToUI() {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_UNREGISTER_CLIENT));
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    private void sendClockMessageToUI(int intvaluetosend) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_CLOCK, intvaluetosend, numberOfDrinks));

            }
            catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void pushNotification(){
        Notification notification = buildNotification();
        nm.notify(10,notification);
    }

    private Notification buildNotification() {
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        //creating all of the intents
        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Intent addBeerIntent = new Intent(getApplicationContext(),DrinkingService.class);
        Intent exitIntent = new Intent(getApplicationContext(),DrinkingService.class);
        Intent removeBeerIntent = new Intent(getApplicationContext(),DrinkingService.class);

        // all of the actions are what is used for the onstart command to change behaviour depending what button is pressed
        addBeerIntent.setAction(ACTION_ADD_BEER);
        exitIntent.setAction(ACTION_EXIT);
        removeBeerIntent.setAction(ACTION_REMOVE_BEER);

        //creating the pending intents
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent addBeerPI = PendingIntent.getService(this,0,addBeerIntent,0);
        PendingIntent exitPI = PendingIntent.getService(this,0,exitIntent,0);
        PendingIntent removeBeerPI = PendingIntent.getService(this,0,removeBeerIntent,0);

        //creating actions the non-depreciated way
        NotificationCompat.Action action3 = new NotificationCompat.Action.Builder(R.drawable.ic_add_black_32dp, "Beer Me", addBeerPI).build();
        NotificationCompat.Action action1 = new NotificationCompat.Action.Builder(R.drawable.ic_close_black_32dp, "Exit", exitPI).build();
        NotificationCompat.Action action2 = new NotificationCompat.Action.Builder(R.drawable.ic_remove_black_32dp, "Minus", removeBeerPI).build();
        double bac = BAC/1000.0;
        //this will work, target SDK is 24
        Notification notification = new NotificationCompat.Builder(this)
                .setContentText("Estimated BAC: " + bac)
                .setContentTitle("Number of drinks: "+ numberOfDrinks)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .addAction(action3)
                .addAction(action2)
                .addAction(action1)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0,1,2))
                .setSmallIcon(R.drawable.transparent_beer)
                .build();
        return notification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals(ACTION_START_THIS_BITCH_UP)){
            Log.i("MyService", "Service Started.");
            Notification notification = buildNotification();
            startForeground(10,notification);
            timer.scheduleAtFixedRate(new TimerTask(){ public void run() {onTimerTick();}}, 0, 1000L);
            isRunning = true;
            Log.i("MyService", "Received start id " + startId + ": " + intent);
            initDrinkingInDB();
        }
        else if(intent.getAction().equals(ACTION_ADD_BEER)){
            numberOfDrinks +=1;
            updateBAC();
            sendBACMessageToUI();
            pushNotification();
            updateDrinksInDB();
        }
        else if(intent.getAction().equals(ACTION_EXIT)){
            savePrevious();
            sendPreviousMessageToUI();
            sendUnbindMessageToUI();
            stopForeground(true);
            stopSelf();
        }
        else if(intent.getAction().equals(ACTION_REMOVE_BEER)){
            if(numberOfDrinks>0) numberOfDrinks -=1;
            updateBAC();
            sendBACMessageToUI();
            pushNotification();
            updateDrinksInDB();
        }
        return START_NOT_STICKY; //if it crashes it wont restart
    }

    public static boolean isRunning()
    {
        return isRunning;
    }


    private void onTimerTick() {
      //  Log.i("TimerTick", "Timer doing work." + counter);
        try {
            if(BAC == 0){
                beginningOfIntervalDrinks = numberOfDrinks;
                beginningOfIntervalTime = counter;
            }
            counter += 1;
            updateBAC();
            sendBACMessageToUI();
            sendClockMessageToUI(counter);
            //updates the notification every 10 min
            if(counter%600 == 0) pushNotification();
            Log.v("TimerTick", ""+counter);
        }
        catch (Throwable t) { //you should always ultimately catch all exceptions in timer tasks.
            Log.e("TimerTick", "Timer Tick Failed.", t);
        }
    }

    private void updateBAC() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SettingsActivity.PREFERENCES,MODE_PRIVATE);
        int weight =  sharedPreferences.getInt(SettingsActivity.WEIGHT,1);
        boolean isMale = sharedPreferences.getBoolean(SettingsActivity.GENDER,true);
        double genderMuliplier=.66;
        if(isMale) genderMuliplier =.76;
        /*
         * The intervals are there because if a lot of time passes between drinks,
         * the bac will be inaccurately low
         */
        int drinksSinceSober = numberOfDrinks - beginningOfIntervalDrinks;
        double Oz = drinksSinceSober*12.0*.05;
        int timeSinceSober = counter - beginningOfIntervalTime;
        double BACasDouble = (Oz*5.14)/(weight*genderMuliplier)-.015*(timeSinceSober/3600.0);
        BACasDouble *=1000;
        BAC = (int)Math.round(BACasDouble);
        if (BAC<0) BAC = 0;

    }


    @Override
    public void onDestroy() {
        savePrevious();
        sendPreviousMessageToUI();
        if (timer != null) {timer.cancel();}
        counter=0;
        nm.cancel(10); // Cancel the persistent notification.
        Log.i("MyService", "Service Stopped.");
        isRunning = false;
        stopDrinkingInDB();
        super.onDestroy();
    }
    //this method is called when the app is swiped away from recents screen
    @Override
    public void onTaskRemoved(Intent rootIntent){
        super.onTaskRemoved(rootIntent);
        this.stopSelf();
    }

    private void savePrevious(){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SettingsActivity.PREFERENCES,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SettingsActivity.LAST_TIME_DRINKING,numberOfDrinks);
        editor.commit();
        Log.v("MyService","saved info");
    }
    private void updateDrinksInDB(){
        db.collection(DB_USERS_COLLECTION).document(mAuth.getCurrentUser().getEmail()).collection(DB_PARTY_COLLECTION).document(DB_DRINKING_DOCUMENT)
                .update(DB_NUMBER_OF_DRINKS,numberOfDrinks);
    }
    private void initDrinkingInDB(){
        WriteBatch batch = db.batch();
        DocumentReference userDoc = db.collection("users").document(mAuth.getCurrentUser().getEmail()).collection(DB_PARTY_COLLECTION).document(DB_DRINKING_DOCUMENT);
        batch.update(userDoc, DB_IS_DRINKING, true);
        batch.update(userDoc, DB_NUMBER_OF_DRINKS, 0);
        batch.commit();
    }
    private void stopDrinkingInDB(){
        WriteBatch batch = db.batch();
        DocumentReference userDoc = db.collection("users").document(mAuth.getCurrentUser().getEmail()).collection(DB_PARTY_COLLECTION).document(DB_DRINKING_DOCUMENT);
        batch.update(userDoc,DB_IS_DRINKING, false);
        batch.update(userDoc,DB_NUMBER_OF_DRINKS, 0);
        batch.commit();
    }
}
