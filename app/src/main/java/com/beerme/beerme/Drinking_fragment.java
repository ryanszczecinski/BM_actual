package com.beerme.beerme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class Drinking_fragment extends Fragment implements View.OnClickListener{
    Button startBtn, addDrinkBtn, removeDrinkBtn;
    TextView estimatedBACView, numberOfDrinks, previousDrinks, timeElapsedView;
    Messenger mService = null;
    boolean mIsBound, editingText = false;
    Thread drinkingThread;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    //same as the service
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DrinkingService.MSG_BAC:
                    double bac = msg.arg1/(1000.0);
                   estimatedBACView.setText("Estimated BAC: "+bac);
                   numberOfDrinks.setText(""+msg.arg2);
                   break;
                case DrinkingService.MSG_CLOCK:
                    if(!editingText) setTimeElapsedView(msg.arg1);
                    Log.v("Clock message", "" +editingText);
                    numberOfDrinks.setText(""+msg.arg2);
                    break;
                case DrinkingService.MSG_PREVIOUS:
                    Log.v("MyFrag","recieved message");
                    SharedPreferences sharedPreferences = getContext().getSharedPreferences(SettingsActivity.PREFERENCES,Context.MODE_PRIVATE);
                    previousDrinks.setText("Previous Number of Drinks: "+sharedPreferences.getInt(SettingsActivity.LAST_TIME_DRINKING,0));
                    break;
                case DrinkingService.MSG_UNREGISTER_CLIENT:
                    doUnbindService();
                    resetViews();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    //not exactly sure, creates a service connection. this is the client for the service and sends stuff through here
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, DrinkingService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;

        }
    };
    private void setTimeElapsedView(int secondsElapsed){
        int hours = 0;
        int minutes = 0;
        String time ="";
        int remainderSeconds = secondsElapsed;
        if(secondsElapsed/3600>=1){
            hours = secondsElapsed/3600;
            secondsElapsed -= hours*3600;
        }
        if(remainderSeconds/60>=1){
            minutes = secondsElapsed/60;
            secondsElapsed -= minutes*60;
        }
        if (hours>0){
            time +=hours;
            time += ":";
        }
        if (hours>0&&minutes<10) {
            time+="0";
            if(minutes==0)time+="0:";
        }
        if (minutes>0){
            time+=minutes;
            time+=":";
        }
        if(secondsElapsed>=0&&secondsElapsed<10&&(minutes>0||hours>0))time+="0";

        time +=secondsElapsed;

        timeElapsedView.setText(time);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v("oncreate","happened");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_drinking, container, false);
        startBtn =  v.findViewById(R.id.start_drinking_button);
        startBtn.setOnClickListener(this);
        addDrinkBtn =  v.findViewById(R.id.add_beer);
        addDrinkBtn.setOnClickListener(this);
        removeDrinkBtn =  v.findViewById(R.id.remove_drink);
        removeDrinkBtn.setOnClickListener(this);
        estimatedBACView = v.findViewById(R.id.estimatedBAC);
        numberOfDrinks = v.findViewById(R.id.number_of_drinks);
        previousDrinks = v.findViewById(R.id.previous_number_of_drinks);
        timeElapsedView = v.findViewById(R.id.time_elapsed);
        timeElapsedView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    // Perform action on key press
                    if(!timeElapsedView.getText().toString().equals("")&&timeElapsedView.getText().toString().length()<3)setCurrentTime(Integer.valueOf(timeElapsedView.getText().toString()));
                    editingText = false;
                    timeElapsedView.setHint("");
                    return true;
                }
                return false;
            }
        });
        timeElapsedView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Toast.makeText(getContext(), timeElapsedView.getText(), Toast.LENGTH_LONG).show();
                timeElapsedView.setText("");
                timeElapsedView.setHint("Number of hours");
                editingText = true;
            }
        });
        CheckIfServiceIsRunning();
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SettingsActivity.PREFERENCES,Context.MODE_PRIVATE);
        if(sharedPreferences.contains(SettingsActivity.LAST_TIME_DRINKING)&&!DrinkingService.isRunning()) previousDrinks.setText("Previous Number of Drinks: "+sharedPreferences.getInt(SettingsActivity.LAST_TIME_DRINKING,0));


        //this is the new thread for the service to run on, maybe overkill but whatever
        //could be wrong here but if the service is already started/bound the new thread will bind the service and then kill the old one...maybe...
        drinkingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v("MyThread", "Thread run");
                if(DrinkingService.isRunning()) doBindService();
                else{
                Intent intent = new Intent(getContext(), DrinkingService.class);
                intent.setAction(DrinkingService.ACTION_START_THIS_BITCH_UP);
                getContext().startService(intent);
                }
            }
        });
        return v;
    }
    //this is how to process click events within a fragment
    @Override
    public void onClick(View v){
        if(v !=null){
            switch (v.getId()){
                case R.id.start_drinking_button:
                    startDrinkingService();
                    break;
                case R.id.add_beer:
                    addBeer();
                    break;
                case R.id.remove_drink:
                    removeBeer();
                    break;


            }
        }

    }
    //starts the thread which starts the service
    //also will kill the service once the user is done drinking
    private void startDrinkingService(){
        if(startBtn.getText().equals("It's Five O'Clock Somewhere")){
            startBtn.setText("Time to sober up");
            drinkingThread.run();
            doBindService();
            mIsBound = true;
        }
        else{
            sendPreviousMessageToService();
            removeServiceFromForeground();
            doUnbindService();
            getContext().stopService(new Intent(getContext(),DrinkingService.class));
            resetViews();
        }

    }
    //sends a message to the service to increment the number of beers
    private void addBeer(){
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, DrinkingService.MSG_ADD_BEER);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }
    //sends a message to the service to decrement the number of beers
    private void removeBeer(){
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, DrinkingService.MSG_REMOVE_BEER);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }
    private void sendPreviousMessageToService(){
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, DrinkingService.MSG_PREVIOUS);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    private void removeServiceFromForeground(){
        if (mIsBound) {

            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, DrinkingService.MSG_REMOVE_FROM_FOREGROUND);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }
    private void setCurrentTime(int hours){
        if (mIsBound) {

            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, DrinkingService.MSG_SET_TIME);
                    msg.replyTo = mMessenger;
                    msg.arg1 = hours;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    //this method will tell the service to remove the connection form the clients list
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, DrinkingService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            getContext().unbindService(mConnection);
            mIsBound = false;

        }
    }
    //binds the service to the UI, will allow the service to send the fragment messages
    //for this app we always bind right after we start the service
    void doBindService() {
        getContext().bindService(new Intent(getContext(), DrinkingService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
       // textStatus.setText("Binding.");
    }
    //when the fragment is destroyed we want to unbind from the service and then stop the service
    //once the service is stopped the the thread will die...i think
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("frag","destroyed");
        try {
            doUnbindService();
        }
        catch (Throwable t) {
           // Log.e("MainActivity", "Failed to unbind from the service", t);
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        doUnbindService();
    }
    @Override
    public void onResume(){
        super.onResume();
        CheckIfServiceIsRunning();
    }
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (DrinkingService.isRunning()) {
            doBindService();
            startBtn.setText("Time to sober up");
        }
        else{
            resetViews();
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(SettingsActivity.PREFERENCES,Context.MODE_PRIVATE);
            previousDrinks.setText("Previous Number of Drinks: "+sharedPreferences.getInt(SettingsActivity.LAST_TIME_DRINKING,0));

        }
    }
    public void resetViews(){
        startBtn.setText("It's Five O'Clock Somewhere");
        numberOfDrinks.setText("0");
        estimatedBACView.setText("Estimated BAC: 0.0");
        timeElapsedView.setText("");
    }

}

