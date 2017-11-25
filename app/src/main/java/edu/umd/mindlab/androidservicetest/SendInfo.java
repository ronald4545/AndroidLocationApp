package edu.umd.mindlab.androidservicetest;

// This task sends the device ID info and LUID to the location server. I should probably merge this with the
// other task that sends to the location server, but I'll do that later.

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendInfo extends AsyncTask<JSONObject, Void, String> {

    // public final String URI = "https://safe-scrubland-41744.herokuapp.com/";
    public final String URIloc = "http://atif01.cs.umd.edu:8009/LocationServercont2/ContFindLocation";
    public final String URIluid = "http://atif01.cs.umd.edu:8009/LocationServercont2/Login";
    public final String URIstatus = "http://atif01.cs.umd.edu:8009/LocationServercont2/Status";
    final String TAG = "Send Info";

    private Context mContext;
    private TaskCompleted mCallback;

    public SendInfo(Context context) {
        this.mContext = context;
        this.mCallback = (TaskCompleted) context;
    }

    @Override
    protected String doInBackground(JSONObject... jObjs) {

        Log.v(TAG, "in SendInfo");

        String URI;

        // checking to see what kind of data we have, to know where to send it
        if (jObjs[0].length() == 3){
            URI = URIluid;
        } else if(jObjs[0].length() == 2){
            URI = URIstatus;
        } else{
            URI = URIloc;
        }

        String data = "";
        HttpURLConnection httpURLConnection = null;

        try {

            // establish a connection to the server
            httpURLConnection = (HttpURLConnection) new URL(URI).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            if (jObjs != null && jObjs.length > 0) {
                final JSONObject realjObject = jObjs[0];
                Log.i(TAG, realjObject.toString());
                wr.writeBytes(realjObject.toString());
            }
            wr.flush();
            wr.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (httpURLConnection.getInputStream())));

            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
                response.append('\r');
            }
            data = response.toString();
            httpURLConnection.disconnect();

            Log.i(TAG, "Data gotten: " + data);
        } catch (Exception ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            ex.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        } // end try-catch

        return data;

    } // end doInBackground

    @Override
    protected void onPostExecute(String result) {

        Log.i(TAG, result);

        // when the task is completed send the result directly to whoever wanted to send the data
        mCallback.onTaskCompleted(result);

    } // end onPostExecute

}// end sendData