package edu.umd.mindlab.androidservicetest;

import android.app.Application;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.sender.HttpSender;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;

// setting up where the crash reports go
@ReportsCrashes(
mailTo = "catchumd@gmail.com",
        customReportContent = {
        ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
        ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.PRODUCT,
        ReportField.BRAND, ReportField.SETTINGS_SYSTEM,
        ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text

)
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
    }
}
