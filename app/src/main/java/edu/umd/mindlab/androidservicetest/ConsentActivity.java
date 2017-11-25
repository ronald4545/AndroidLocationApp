package edu.umd.mindlab.androidservicetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

public class ConsentActivity extends AppCompatActivity  {

    public static final String SAMPLE_FILE = "Consent_Smartphone_App2.pdf";
    private final String TAG = "Consent Activity";
    private final String TIME_MESSAGE = "time_sent";
    String pdfFileName;
    Integer pageNumber = 0;

    private Button agreeButton;
    private Button disAgreeButton;
    PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // register the receiver which will tell the app when to check if at bottom of page
        LocalBroadcastManager.getInstance(this).registerReceiver(
                timeReceiver, new IntentFilter(TIME_MESSAGE));

        pdfView = (PDFView)findViewById(R.id.pdfView);
        displayFromAsset(SAMPLE_FILE);

        agreeButton = (Button) findViewById(R.id.agreeBtn);
        agreeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ((pdfView.getCurrentPage()) >= (pdfView.getPageCount()-2)) {
                    Intent emailIntent = new Intent(v.getContext(), SendEmail.class);
                    startActivity(emailIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "Please scroll down to read the entire consent form",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        disAgreeButton = (Button) findViewById(R.id.disagreeBtn);
        disAgreeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent logIntent = new Intent(v.getContext(), CASLoginActivity.class);
                logIntent.putExtra("ConsentFailed", false);
                startActivity(logIntent);
            }
        });

/*
        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setOnClickListener(new View.OnClickListener() {
            //@Override
            //public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        //.setAction("Action", null).show();
            //}
        //}); */

        // this thread checks if we are at the bottom page every half a second
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    // should continue checking as long as the user has not reached the end
                    while(true){

                        Thread.sleep(1000);
                        Intent intent = new Intent(TIME_MESSAGE);
                        int curr = pdfView.getCurrentPage();
                        int total = pdfView.getPageCount();

                        // if the user has reached the end, send a message to make the buttons visible.
                        // cannot make them visible from here directly because background threads can't affect the UI
                        if (curr >= total - 2){
                            Intent sendIntent = new Intent(TIME_MESSAGE);
                            LocalBroadcastManager.getInstance(ConsentActivity.this).sendBroadcast(sendIntent);
                            break;
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "problem with the thread?");
                    e.printStackTrace();
                }
            }

        }).start();
    }

    // this method gets the pdf from the assets folder and loads it
    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;

        pdfView.fromAsset(SAMPLE_FILE)
                .defaultPage(pageNumber)
                .enableSwipe(true)

                .swipeHorizontal(false)
                .enableAnnotationRendering(true)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();

    }

    // this receiver waits for the thread to tell it that the end of the pdf has been reached
    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            disAgreeButton.setVisibility(View.VISIBLE);
            agreeButton.setVisibility(View.VISIBLE);

        }
    };

    // it's important to unregister a receiver when it will no longer be needed
    protected void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timeReceiver);
    }

    @Override
    public void onBackPressed(){}

}
