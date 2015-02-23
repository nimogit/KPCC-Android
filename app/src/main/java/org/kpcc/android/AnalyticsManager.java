package org.kpcc.android;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;

/**
 * Created by rickb014 on 2/15/15.
 */
public class AnalyticsManager {
    private static AnalyticsManager INSTANCE = null;
    public static final String TAG = "AnalyticsManager";
    private static final String MIXPANEL_TOKEN = AppConfiguration.getInstance().getConfig("mixpanel.token");

    private MixpanelAPI mMixpanelAPI;

    public static AnalyticsManager getInstance() {
        return INSTANCE;
    }

    public static void setupInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsManager(context);
        }
    }


    protected AnalyticsManager(Context context) {
        mMixpanelAPI = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
    }


    public void logEvent(String name, JSONObject parameters) {
        mMixpanelAPI.track(name, parameters);
    }

    public void logEvent(String name) {
        // Send empty parameters
        JSONObject parameters = new JSONObject();
        mMixpanelAPI.track(name, parameters);
    }

    public void flush() {
        mMixpanelAPI.flush();
    }
}
