package org.kpcc.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfiguration {
    public static final String TAG = "AppConfiguration";
    private static AppConfiguration INSTANCE = null;

    private Properties props;

    public static void setupInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AppConfiguration(context);
        }
    }

    public static AppConfiguration getInstance() {
        return INSTANCE;
    }

    // If keys.properties is missing or can't be read, you'll get a big ol' IOException.
    protected AppConfiguration(Context context) {
        props = new Properties();

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("keys.properties");
            props.load(is);
            is.close();
        } catch (IOException e) {
            Log.d(TAG, "keys.properties couldn't be read.");
        }
    }

    public String getConfig(String config) {
        return props.getProperty(config);
    }
}
