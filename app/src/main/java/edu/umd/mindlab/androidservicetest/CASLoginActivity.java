package edu.umd.mindlab.androidservicetest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CASLoginActivity extends AppCompatActivity {

    public static final String TAG = "CASLoginActivity";
    private static final String TERMS_ACCEPT = "Are_Terms_Accepted";
    public static WebView mWebView;
    private boolean goToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caslogin);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // check if the user is currently snoozing, or even logged in
        LoggedIn log = LoggedIn.getLog();
        if (log.getSnoozed()){
            Intent snoozeIntent = new Intent(CASLoginActivity.this, Snooze.class);
            startActivity(snoozeIntent);
        }else if (log.getLoggedIn()) {
            Intent mainIntent = new Intent(CASLoginActivity.this, MainActivity.class);
            startActivity(mainIntent);
        }

        // It was necessary to add this reinitializing of the webView for logout to work on my phone
        WebView mWebView = new WebView(CASLoginActivity.this);

        mWebView = (WebView) findViewById(R.id.caswebview);

        mWebView.setInitialScale(1);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // load the login web page
        mWebView.loadUrl("https://login.umd.edu/");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            // when the page is finished, decide what to do
            @Override
            public void onPageFinished(WebView view, String url){

                if (url == null || url == "") {
                    Log.i(TAG, "URL is EMPTY");
                    return;
                }

                Log.i(TAG, url);

                // if we've gotten to the "demo" page, the login was successful
                if (url.equals("https://login.umd.edu/demo/")) {

                    destroyWebView();

                    Log.v(TAG, "the page is finished: " + url);

                    // prevents the next activity from being started twice
                    if (goToMain) {
                        goToMain =false;

                        SharedPreferences sharedPref = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
                        boolean termsAccepted = sharedPref.getBoolean(TERMS_ACCEPT, false);

                        Log.v(TAG, "termsAccepted is " + termsAccepted);

                        if (termsAccepted) {
                            Log.v(TAG, "CASLoginActivity:CreateView:WebViewClient:OnPageFinished TermsAccepted Check, CASLogin Complete");

                            //started making changes
                            Intent mainIntent = new Intent(CASLoginActivity.this, MainActivity.class);
                            //mainIntent.putExtra("CallingActivity", "caslogin");
                            startActivity(mainIntent);


                        } else {
                            Log.v(TAG, "CASLoginActivity:CreateView:WebViewClient:OnPageFinished TermsAccepted False");

                            Intent getInfoIntent = new Intent(CASLoginActivity.this, GetPersonalInfo.class);
                            startActivity(getInfoIntent);

                        }
                    }
                }
            }
        });
    }

    // the webview needs to be destroyed or it will hang around
    public void destroyWebView() {

        // catch anytime it would be trying to destroy a null webview
        if (mWebView == null){
            return;
        }

        ((ViewGroup)mWebView.getParent()).removeView(mWebView);

        // Make sure you remove the WebView from its parent view before doing anything.
        mWebView.removeAllViews();

        mWebView.clearHistory();

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
        mWebView.clearCache(true);

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        mWebView.loadUrl("about:blank");

        mWebView.onPause();
        mWebView.removeAllViews();
        mWebView.destroyDrawingCache();

        // NOTE: This pauses JavaScript execution for ALL WebViews,
        // do not use if you have other WebViews still alive.
        // If you create another WebView after calling this,
        // make sure to call mWebView.resumeTimers().
        mWebView.pauseTimers();

        // NOTE: This can occasionally cause a segfault below API 17 (4.2)
        mWebView.destroy();

        // Null out the reference so that you don't end up re-using it.
        mWebView = null;
    }

    @Override
    protected void onStop(){
        super.onStop();
        destroyWebView();
    }

    @Override
    protected void onResume(){
        super.onResume();
        goToMain = true;
    }

    @Override
    public void onBackPressed(){}
}