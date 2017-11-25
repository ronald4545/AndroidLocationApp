package edu.umd.mindlab.androidservicetest;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

public class SnoozeService extends Service {

    public final String TAG = "Snooze Service";
    private Counter timeCount;
    private final String TIME_FILTER = "TimeFilter";
    private final String FINISH_FILTER = "FinishFilter";

    public SnoozeService() {
    }

    @Override
    public void onCreate(){
        Log.v(TAG, "In onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        /* TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented"); */
        return null;
    }

    // when the service is started, it starts here
    @Override
    public int onStartCommand(Intent intent, int flags, int startID){

        handleCommand(intent);

        // start_sticky has to do with what you want to estate of the service to be in case of an error
        return START_STICKY;

    }

    // this method creates a timer object and starts it
    public void handleCommand(Intent i){

        int hours = i.getIntExtra("hours", 1);
        int minutes = i.getIntExtra("minutes", 1);

        int ms = ((hours * 3600) + minutes * 60) * 1000;
        timeCount = new Counter(ms,1000);

        timeCount.start();

    }

    // the class for the time counter
    public class Counter extends CountDownTimer {

        public Counter(long millisInFuture, long countDownInterval) {
            super(millisInFuture,countDownInterval);
        }

        public void onTick(long millisUntilFinished) {
            String minsStr;
            String secsStr;

            // from millisUntilFinished figure out how many hours, minutes and seconds to show
            long secsLeft = millisUntilFinished/1000;
            long hours = secsLeft/3600;
            long minsLeft = (secsLeft - (hours * 3600))/60;
            secsLeft = ((secsLeft - (hours * 3600)) - (minsLeft * 60));

            // making sure it shows 05 instead of 5, not necessary for hours
            if (minsLeft < 10){
                minsStr = "0" + minsLeft;
            } else{
                minsStr = "" + minsLeft;
            }
            if (secsLeft < 10) {
                secsStr = "0" + secsLeft;
            } else{
                secsStr = "" + secsLeft;
            }

            sendTime(hours, minsStr, secsStr);

        }

        public void onFinish() {
            Log.v(TAG, "Snoozing finished");
            sendFinished();
        }

    }

    // send a broadcast with the amount of time left
    public void sendTime(long hours, String mins, String secs){

        Intent intent = new Intent("timeSent");
        String time = hours + " : " + mins + " : " + secs;
        intent.putExtra("time", time);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    // send a broadcast when there's no time left
    public void sendFinished(){

        Intent intent = new Intent("finished");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy(){

        timeCount.cancel();
        Log.v(TAG, "in onDestroy");
        stopSelf();
    }
}
