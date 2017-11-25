package edu.umd.mindlab.androidservicetest;

/**
 * Created by User on 9/24/2017.
 */

public class LoggedIn {

    // single instance of the class
    private static LoggedIn logged = new LoggedIn();
    private boolean isLoggedIn;
    private String fullName;
    private boolean goToMain;
    private boolean snoozed;
    private boolean isSendingLoc;

    private LoggedIn(){
        isLoggedIn = false;
        fullName = "No name provided";
        goToMain = true;
        snoozed = false;
        isSendingLoc = false;
    }

    // returns the only instance of this class
    public static LoggedIn getLog(){
        return logged;
    }

    // when the user is logged in this will set isLoggedIn to true
    public void setLoggedIn(boolean bool){
        isLoggedIn = bool;
    }

    // returns the status of whether the user is logged in
    public boolean getLoggedIn(){
        return isLoggedIn;
    }

    // return the name of the user, after the consent email is sent, the name will be destroyed
    public String getName(){
        return fullName;
    }

    // sets the name of the user, so we can include it in the consent email
    public void setName(String fName){
        fullName = fName;
    }

    // after the consent email is sent, the name of the user is destroyed
    public void destroyName(){
        fullName = "DESTROYED";
    }

    // set the status for the app to snoozed
    public void setSnoozed(boolean value){
        snoozed = value;
    }

    // return whether or not the app is snoozed
    public boolean getSnoozed(){
        return snoozed;
    }

    // set the boolean for whether or not the location services is running
    public void setSending(boolean value){
        isSendingLoc = value;
    }

    // return the boolean for whether or not the location services is running
    public boolean getSending(){
        return isSendingLoc;
    }
}
