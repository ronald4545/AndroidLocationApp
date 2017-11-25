package edu.umd.mindlab.androidservicetest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.telephony.TelephonyManager;
import android.os.Build;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class GetPersonalInfo extends AppCompatActivity implements TaskCompleted {

    private EditText fname;
    private EditText lname;
    private EditText dob;
    private EditText uid;
    private Button infoSubmit;
    private String luid;

    private int verifyAttempts = 0;

    private final String TAG = "GetPersonalInfo";

    private static final String LUID_STORE = "The_LUID_is_stored";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_personal_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // initialize the views
        fname = (EditText) findViewById(R.id.firstNameEdit);
        lname = (EditText) findViewById(R.id.lastNameEdit);
        dob = (EditText) findViewById(R.id.dobEdit);
        uid = (EditText) findViewById(R.id.uidEdit);
        infoSubmit = (Button) findViewById(R.id.submitPersInfo);

        // when the user clicks "submit"
        infoSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // retrieve the info
                String first_name = fname.getText().toString();
                String last_name = lname.getText().toString();
                String birth_date = dob.getText().toString();
                String UID = uid.getText().toString();

                // check that the info entered was valid
                String validMessage = validateData(last_name, birth_date, UID);

                // if the info was invalid, make a toast to tell the user
                if (validMessage.length() != 0){

                    Toast.makeText(v.getContext(), validMessage, Toast.LENGTH_LONG).show();

                } else{

                    // sets the name that will be sent with the consent PDF email
                    LoggedIn log = LoggedIn.getLog();
                    log.setName(first_name + " " + last_name);

                    // this method will use the hash function and return the LUID
                    luid = getLUID(last_name, birth_date, UID);

                    // this method will gather the device info and package it (with LUID) in a JSON
                    JSONObject infoJSON = getDeviceInfo(luid);

                    (new SendInfo(GetPersonalInfo.this)).execute(infoJSON);

                }

            }
        });

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }); */
    }

    // This method validates that the user entered data in the correct format
    public String validateData(String last, String date, String id){

        String message = "";
        int messageCounter = 0;

        // no last name given
        if (last.length() == 0){
            message += "Invalid Last Name";
            messageCounter++;
        }

        // checking the right number of digits for birth date
        String[] dateArray = date.split("/");
        if (dateArray[0].length() != 2 || dateArray[1].length() != 2){
            // if there is already a message started
            if (messageCounter > 0){
                message += "\n";
            }
            message += "Invalid Birth Date Format";
            messageCounter++;
        }

        // check UID is 0 digits
        if (id.length() != 9){
            if (messageCounter > 0){
                message += "\n";
            }
            message += "Invalid UID";
        }

        return message;

    }

    // This is the method where the info is hashed and the LUID is returned
    public String getLUID(String lastName, String birthDateString, String UID){

        String newDateString="";

        // formatting the birth date
        DateFormat df = new SimpleDateFormat("MM/dd");
        DateFormat stdDF = new SimpleDateFormat("MM-dd");
        Date birthDate;

        try {
            birthDate = df.parse(birthDateString);
            newDateString = stdDF.format(birthDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String toHash = lastName + newDateString + UID;
        toHash = toHash.replace(" ","").toLowerCase();

        // now we can actually start the hashing
        String generatedHash = null;
        StringBuilder sb = null;
        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(toHash.getBytes());

            // hasing the bytes of the string
            sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            generatedHash = sb.toString();

        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }

        Log.v(TAG, "String to hash is: " + toHash);
        Log.v(TAG, "Generated Hash is: " + generatedHash);

        return generatedHash;

    }

    // Now that we know the LUID is valid, store it in shared preferences
    public void storeLUID(String LUID){

        SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
        editor.putString(LUID_STORE, LUID);
        editor.commit();

    }

    // Retrieve the device info to send to the server
    public JSONObject getDeviceInfo(String luid){

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(serviceName);
        String deviceID =  Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        int simState = m_telephonyManager.getSimState();
        String serviceProvider = m_telephonyManager.getNetworkOperatorName();
        Integer API_Level = Build.VERSION.SDK_INT;
        String apiLevel = API_Level.toString();
        String device = android.os.Build.DEVICE;
        String model = android.os.Build.MODEL;
        String product = android.os.Build.PRODUCT;

        // create a JSON with the device info
        JSONObject infoJSON = new JSONObject();
        try {
            infoJSON.put("LUID", luid);
            infoJSON.put("deviceID", deviceID);
            infoJSON.put("deviceInfo", "Make: " + device + ", Model: " + model + ", Network Provider: " + serviceProvider + ", API: " + apiLevel);
        } catch (JSONException e) {
            Log.e(TAG, "JSON problem");
        }

        return infoJSON;
    }

    @Override
    public void onBackPressed(){}

    // this method executes once the server has sent back data
    @Override
    public void onTaskCompleted(String result) {

        if (result == null){

            Log.v(TAG, "The result was null. Something is wrong.");

        } else if(result.contains("LUID")){
            // if the LUID was valid go to the consent activity

            Log.v(TAG, "The result correct.");

            storeLUID(luid);
            luid = "";

            Intent consentIntent = new Intent(this, ConsentActivity.class);
            startActivity(consentIntent);

        } else{
            // if the result was not valid, ask them to try again

            if (verifyAttempts > 0) {
                // if after trying again, it still doesn't work. Ask them to see a project manager.

                Toast.makeText(this, "Please see project director for assistance", Toast.LENGTH_LONG).show();

            }  else{

                Toast.makeText(this, "Verification failed. Please check info and try again.", Toast.LENGTH_SHORT).show();
                verifyAttempts++;
            }

        }

    }

}
