package org.kpcc.android;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;

/**
 * All this class really does is initialize Parse. The functions of Parse (notifications, cloud
 * functions) should be abstracted into different managers, which use Parse if they want to.
*/
public class ParseManager {
    private static ParseManager instance = null;
    public static final String TAG = "ParseManager";
    private static final String PARSE_APP_ID = AppConfiguration.getInstance().getConfig("parse.applicationId");
    private static final String PARSE_CLIENT_KEY = AppConfiguration.getInstance().getConfig("parse.clientKey");

    public static ParseManager getInstance() {
        return instance;
    }

    public static void setupInstance(Application application) {
        if (instance == null) {
            instance = new ParseManager(application);
        }
    }


    protected ParseManager(Application application) {
        Parse.initialize(application, PARSE_APP_ID, PARSE_CLIENT_KEY);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

}
