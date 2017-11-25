package edu.umd.mindlab.androidservicetest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LocationService extends Service implements TaskCompleted
{
    private static final String TAG = "LocationService";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 30000;
    private static final float LOCATION_DISTANCE = 0;
    private final String LUID_STORE = "The_LUID_is_stored";
    private static final String COUNT_STORE = "Counter_Stored";

    public MainActivity mMa;

    // wifi info persistence variables
    private WifiManager wifi;
    private List<ScanResult> mWifiResults;

    // will have to send  the LUID with the location data
    private String LUID;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.i(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.i(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            String deviceID =  Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            if (shouldSend()) {
                try {
                    sendLocation(mLastLocation, deviceID);
                } catch(Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.i(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.i(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.i(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        initializeLocationManager();

        try {
            setUp();
        } catch(Exception e) {
            Log.e(TAG, e.toString());
        }

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy");
        super.onDestroy();

        try {
            mWifiResults = null;
            unregisterReceiver(wifiBroadcastReceiver);
        } catch(Exception e) {
            Log.e(TAG, e.toString());
        }

        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    // Broadcast receiver for Wifi. Changes in the Wifi get broadcast to this receiver
    private BroadcastReceiver wifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // scan for all results
                mWifiResults = wifi.getScanResults();

                Log.i(TAG, "Wifi Broadcast Receiver onReceive fired");
                Log.i(TAG, mWifiResults.toString());
            }
        }
    };

    // start the wifi scan
    private void setUp() throws Exception{
        Log.i(TAG, "SetUp");

        registerReceiver(wifiBroadcastReceiver, new IntentFilter(MainActivity.LOC_ACTION));

        if (!isConnected(getApplicationContext())) {
            Context context = getApplicationContext();
            CharSequence text = "Yo, we need your WiFi on!";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            throw new Exception("Wifi disabled");
        } else {
            wifi.startScan();
        }
    }

    // are we connected to the internet at all??
    private static boolean isConnected(Context context) {
        Log.i(TAG, "isConnected");

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null);
    }

    // check to see if we are able to send or not
    public boolean shouldSend() {
        Log.i(TAG, "ShouldSend");
        return mWifiResults != null && mWifiResults.size() != 0;
    }

    // this is the methodwhich actially sends the data to the server
    public void sendLocation(Location location, String deviceId) throws JSONException {
        Log.i(TAG, "SendLocation");

        JSONObject obj = new JSONObject();
        JSONArray ap_array = new JSONArray();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS").format(new Date());

        for (ScanResult scan : mWifiResults) {
            JSONObject ap = new JSONObject();
            ap.put("ssid", scan.SSID);
            ap.put("mac", scan.BSSID);
            ap.put("rssi", scan.level);
            ap.put("freq", scan.frequency);
            ap_array.put(ap);
        }

        // get the LUID out of sharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
        String LUID = sharedPref.getString(LUID_STORE, "doesn't exist");
        obj.put("LUID", LUID);

        int countToSend = sharedPref.getInt(COUNT_STORE, 0) + 1;
        Log.v(TAG, "The count to be sent is " + countToSend);

        obj.put("deviceID", deviceId.length() > 0 ? deviceId : "No-device-ID");
        obj.put("timestamp", timeStamp);
        obj.put("accessPoints", ap_array);
        obj.put("latitude", location.getLatitude());
        obj.put("longitude", location.getLongitude());
        obj.put("altitude", location.getAltitude());
        obj.put("accuracy", location.getAccuracy());
        obj.put("counter", countToSend);

        (new SendInfo(LocationService.this)).execute(obj);
    }

    private void initializeLocationManager() {
        Log.i(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            // register the WIFI broadcast receiver so we can get info from WIFI Service
            wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            try {
                setUp();
                registerReceiver(wifiBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            }
            catch (Exception e) {
                Log.i(TAG, "Wifi isn't on");
            }
        }
    }

    // deal with the incrementing of the data counter if the result was a valid location
    public void onTaskCompleted(String result){

        // only increment the count if actual data was sent to the server
        if (result != null && !result.contains("null")) {

            SharedPreferences sharedPref = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
            int count = sharedPref.getInt(COUNT_STORE, 0);

            count++;

            SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
            editor.putInt(COUNT_STORE, count);
            editor.commit();

        }

        Log.v(TAG, "in task completed");

        // let the main page know that the count has been increased
        Intent intent = new Intent("countSent");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }
}