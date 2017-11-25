package edu.umd.mindlab.androidservicetest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

// MainActivity which handles all services and broadcast receivers.
public class MainActivity extends AppCompatActivity implements TaskCompleted {

    // GLOBAL VARIABLES
    public static final String LOC_ACTION = "LOCATION";
    private static final String TERMS_ACCEPT = "Are_Terms_Accepted";

    // logging
    private static final String TAG = "MainActivity"; // for logging

    // location and network permissions
    private static final int REQUEST_LOC = 0;
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE
    };

    // switch button persistence variables
    private ToggleButton toggleLoc;
    private static final String SHARE_LOC_STATUS = "Sharing_Location_Status";
    private static final String LUID_STORE = "The_LUID_is_stored";
    private static final String COUNT_STORE = "Counter_Stored";

    // Used for making a note that the user has accepted terms if they have made it to this page

    private Button logOutButton;
    private Button snoozeButton;
    private Button resetAppButton;
    private EditText hours;
    private EditText minutes;
    private Button viewPDF;
    private Button resetCounter;
    private TextView countDisplay;
    private CountReceiver countR;

    // Called when the activity is first created. This is where you should do all of your normal
    // static set up: create views, bind data to lists, etc. This method also provides you with a
    // Bundle containing the activity's previously frozen state, if there was one.
    // Always followed by onStart().
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // standard procedures DO NOT DELETE
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // if the user comes back to the app (and never logged out) and it tries to take them to the login activity it will redirect here.
        LoggedIn log = LoggedIn.getLog();
        log.setLoggedIn(true);

        Log.i(TAG, "OnCreate");

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent serviceIntent = new Intent(this, LocationService.class);

        // set the counter when the activity is started
        countDisplay = (TextView) findViewById(R.id.dataView);
        setCount();

        //switchButton = (Switch)findViewById(R.id.enableloc);
        toggleLoc = (ToggleButton) findViewById(R.id.enableToggle);
        toggleLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            if(toggleLoc.isChecked()) {

                // START LOCATION SERVICE
                startService(serviceIntent);
                Log.i(TAG, "Switch button clicked on -> start service");

                // tell server status is 'collecting: on'
                sendStatus("on");

                // record that the app is sending the location
                LoggedIn log = LoggedIn.getLog();
                log.setSending(true);

                // store that the app is sharing location in sharedPref so it will remember this when returning
                SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
                editor.putBoolean(SHARE_LOC_STATUS, true);
                editor.commit();

                TextView tv = (TextView) findViewById(R.id.textLocation);
                tv.setText("Currently sharing your location");

            } else {

                // STOP LOCATION SERVICE
                // Note: Location service does not terminate unless this toggle is turned off.
                stopService(serviceIntent);
                Log.i(TAG, "Switch button clicked on -> stop service");

                // tell server that status is 'collecting: off'
                sendStatus("off");

                // Let the app know that it is no longer sending location
                LoggedIn log = LoggedIn.getLog();
                log.setSending(false);

                SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
                editor.putBoolean(SHARE_LOC_STATUS, false);
                editor.commit();

                TextView tv = (TextView) findViewById(R.id.textLocation);
                tv.setText("Not sharing your location");

            }
            }
        });

        logOutButton = (Button) findViewById(R.id.logOutButton);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // set that the user is not logged in and that they should be able to get back to main
                LoggedIn log = LoggedIn.getLog();
                log.setLoggedIn(false);

                stopService(serviceIntent);
                Log.i(TAG, "Logged out -> stop service");

                // tell server that status is 'collecting: off'
                sendStatus("logged_out");

                // tell the app that we are no longer sending
                log.setSending(false);

                Intent logIntent = new Intent(v.getContext(), CASLoginActivity.class);
                startActivity(logIntent);

            }
        });

        // if the user clicks on View Terms, creates the fragment that shows the pdf
        viewPDF = (Button) findViewById(R.id.viewTermsButton);
        viewPDF.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Replace the contents of the container with the new fragment
                ft.replace(R.id.place_holder2, new TermsFrag());
                // Complete the changes added above
                ft.commit();

            }
        });

        // check permissions
        if (notGranted()) { // permission has not been granted
            requestLocPermissions();
        }

        // the user clicked to snooze, need the time they want to snooze for
        hours = (EditText) findViewById(R.id.hourEdit);
        minutes = (EditText) findViewById(R.id.minutesEdit);
        snoozeButton = (Button) findViewById(R.id.snoozeButton);
        snoozeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                int hrs;
                // if the number was blank, use default hours
                try{
                    hrs = Integer.parseInt(hours.getText().toString());
                } catch(NumberFormatException e){
                    hrs = -1;
                }

                int mns;
                // if the number was blank, use default mins
                try{
                    mns = Integer.parseInt(minutes.getText().toString());
                } catch(NumberFormatException e){
                    mns = -1;
                }

                stopService(serviceIntent);
                Log.i(TAG, "Snoozed -> stop service");
                unReg();

                // tell the app that we are no longer sending
                LoggedIn log = LoggedIn.getLog();
                log.setSending(false);

                // tell the location server that the app is snoozed
                sendStatus("snoozed");

                // call the snooze activity and pass the time to snooze
                Intent snoozeIntent = new Intent(v.getContext(), Snooze.class);
                snoozeIntent.putExtra("hours", hrs);
                snoozeIntent.putExtra("mins", mns);
                startActivity(snoozeIntent);

            }
        });

        // if the user wants to reset the counter
        resetCounter = (Button) findViewById(R.id.resetCountButton);
        resetCounter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                resetCount();

            }
        });

        // changeLogButton is a terrible name, this is for changing the Terms Accepted status (for testing purposes)
        resetAppButton = (Button) findViewById(R.id.testButton);
        resetAppButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Resetting App");
                builder.setMessage("Your identifier will be lost and you will have to enter " +
                        "your information again to create a new one. Are you sure you want to reset the app?");
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button

                        // change Terms_Accepted to false
                        SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
                        editor.putBoolean(TERMS_ACCEPT, false);
                        // it should simulate that the user has just installed the app, which means this should default to true
                        editor.putBoolean(SHARE_LOC_STATUS, true);
                        editor.commit();

                        Log.v(TAG, "It will now act as no consent has been done on login.");

                        stopService(serviceIntent);
                        Log.i(TAG, "Logged out -> stop service");

                        unReg();

                        // tell server that status is 'collecting: off'
                        sendStatus("removed");

                        // reset the counter
                        resetCount();

                        LoggedIn log = LoggedIn.getLog();
                        log.setLoggedIn(false);

                        // tell the app that we are no longer sending
                        log.setSending(false);

                        Intent logIntent = new Intent(MainActivity.this, CASLoginActivity.class);
                        startActivity(logIntent);

                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the request, do nothing
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });
    }

    public boolean requestLocPermissions() {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.

            Log.i(TAG, "need to show reasons for permissions");
            return false;

        } else {

            ActivityCompat.requestPermissions(this,
                    PERMISSIONS_LOCATION,
                    REQUEST_LOC);

            toggleLoc = (ToggleButton) findViewById(R.id.enableToggle);
            toggleLoc.setChecked(true);

            //switchButton = (Switch)findViewById(R.id.enableloc);
            //switchButton.setChecked(true);

            Log.i(TAG, "requesting permissions");

            return true;
        }
    }

    @Override
    protected void onResume()
    {
        Log.e(TAG, "OnResume");

        countR = new CountReceiver();

        // register the receiver which updates the counter
        LocalBroadcastManager.getInstance(this).registerReceiver(
                countR, new IntentFilter("countSent"));

        setCount();

        toggleLoc = (ToggleButton) findViewById(R.id.enableToggle);
        SharedPreferences sharedPrefs = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
        toggleLoc.setChecked(sharedPrefs.getBoolean(SHARE_LOC_STATUS, true));

        LoggedIn log = LoggedIn.getLog();

        TextView tv = (TextView) findViewById(R.id.textLocation);
        if(toggleLoc.isChecked()) {

            // if the app is already sending data, don't start the service again
            //log = LoggedIn.getLog();
            if (!log.getSending()) {

                final Intent serviceIntent = new Intent(this, LocationService.class);
                // restart service
                stopService(serviceIntent);
                startService(serviceIntent);

                // tell server that status is 'collecting: on'
                sendStatus("on");

                log.setSending(true);
            }

            tv.setText("Currently sharing your location");
        } else{
            tv.setText("Not sharing your location");
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "OnPause");
        super.onPause();
        unReg();
    }

    @Override
    protected void onStop()
    {
        Log.i(TAG, "OnStop");
        super.onStop();
        unReg();
    }

    // Do we have permissions access the location?
    private boolean notGranted() {
        Log.i(TAG, "Check if permissions NotGranted");

        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED;
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){}

    // This method sends the collecting status to the server
    public void sendStatus(String status){

        // get the LUID from shared preferences
        SharedPreferences sharedPref = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
        String LUID = sharedPref.getString(LUID_STORE, "doesn't exist");

        // create the JSON to send to the server
        JSONObject disconJ = new JSONObject();
        try{
            disconJ.put("LUID", LUID);
            disconJ.put("collecting",status);
        }catch(JSONException e){
            Log.v(TAG, "JSON problem?");
        }

        Log.v(TAG, "Right before sending the info");
        // send the JSON
        (new SendInfo(MainActivity.this)).execute(disconJ);

    }

    public void onDestroy(){
        super.onDestroy();
        unReg();
    }

    // unregister the broadcast receiver
    public void unReg(){

        if (countR != null){
            Log.v(TAG, "unregistering receiver");
            LocalBroadcastManager.getInstance(this).unregisterReceiver(countR);
            countR = null;
        }

    }

    // when the location service sends a message that it successfully sent the location, update the counter
    public void setCount(){

        // get the counter from shared preferences
        SharedPreferences sharedPref = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
        int count = sharedPref.getInt(COUNT_STORE, 0);

        // display the new count to the user
        String display = "" + count;
        countDisplay.setText(display);

        Log.v(TAG, "The count was: " + count);

    }

    // reset the data points counter
    public void resetCount(){

        SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
        editor.putInt(COUNT_STORE, 0);
        editor.commit();

        countDisplay.setText("0");

    }

    @Override
    public void onTaskCompleted(String result) {}

    // the receiver which listens for the location service to say it's sent a data point
    public class CountReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            setCount();
        }
    }
}
