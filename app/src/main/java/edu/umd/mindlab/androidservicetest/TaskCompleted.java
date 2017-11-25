package edu.umd.mindlab.androidservicetest;

/**
 * Created by User on 9/24/2017.
 */

public interface TaskCompleted {

    // this interface is used when getting a result back from the server
    void onTaskCompleted(String result);

}
