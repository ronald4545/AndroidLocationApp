package edu.umd.mindlab.androidservicetest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


public class SendEmail extends AppCompatActivity {

    // create the views
    private EditText email;
    private Button sendEmail;
    private Button continueButton;
    // create the necessary strings
    private static final String TERMS_ACCEPT = "Are_Terms_Accepted";
    private static final String SHORTCUT_EXISTS = "Does_Shortcut_Exist";
    private final String TAG = "SendEmailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_email);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Storing in shared preferences that the user has accepted terms
        SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
        editor.putBoolean(TERMS_ACCEPT, true);
        editor.commit();

        // the email is never actually stored anywhere so I don't think there is any need to destroy it.
        email = (EditText) findViewById(R.id.emailEdit);
        sendEmail = (Button) findViewById(R.id.sendConfirmation);
        continueButton = (Button) findViewById(R.id.emailContinueButton);

        // the user has selected to send the email
        sendEmail.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // get the bytes corresponding to the Consent Form PDF
                InputStream inputStream = null;
                ByteArrayOutputStream output = null;
                try {
                    inputStream = getAssets().open("Consent_Smartphone_App2.pdf");
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    output = new ByteArrayOutputStream();
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                } catch(IOException e){
                    Log.e(TAG, "Problem getting pdf");
                }

                // create a byte array corresponding to the pdf file
                final byte[] file = output.toByteArray();

                // send the email in the background
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {

                            LoggedIn log = LoggedIn.getLog();
                            String name = log.getName();

                            // send the pdf to the user
                            EmailSender sender = new EmailSender("catchumd@gmail.com",
                                    "RIPley2122$$");
                            sender.sendMailAttach("Prometheus Terms and Conditions PDF - " + name, "Attached is a copy of the consent form",
                                    "catchumd@gmail.com", email.getText().toString() + ", catch@umd.edu", file);

                        } catch (Exception e) {
                            Log.e("SendMail", "Sending didn't work?");
                        }
                    }

                }).start();

                Toast.makeText(v.getContext(), "Email Sent", Toast.LENGTH_SHORT).show();

            }

        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SharedPreferences sharedPrefs = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE);
                boolean shortcut_exists = sharedPrefs.getBoolean(SHORTCUT_EXISTS, false);

                // if the shortcut does not already exist create one
                if (!shortcut_exists){
                    createShortCut();
                    // Storing in shared preferences that the shortcut has been created
                    SharedPreferences.Editor editor = getSharedPreferences("edu.umd.mindlab.androidservicetest", MODE_PRIVATE).edit();
                    editor.putBoolean(SHORTCUT_EXISTS, true);
                    editor.commit();
                }

                // destroy the user's name
                LoggedIn log = LoggedIn.getLog();
                log.destroyName();

                Intent mainIntent = new Intent(v.getContext(), MainActivity.class);
                startActivity(mainIntent);

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

    // creates a shortcut to the app on the phones home page
    public void createShortCut(){
        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        shortcutintent.putExtra("duplicate", false);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.umd_logo);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(getApplicationContext(), CASLoginActivity.class));
        sendBroadcast(shortcutintent);
    }

    @Override
    public void onBackPressed(){}

}
