package org.kpcc.api;

import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;
import org.kpcc.android.HttpRequest;
import org.kpcc.android.RequestManager;

import java.util.HashMap;
import java.util.Map;

// Provides basic functionality for retrieving data from the API.
//
// Example (Basic Usage):
//
//   public class Article {
//     public static BaseApiClient Client = new BaseApiClient("articles");
//   }
//
// Example (Custom client):
//
//   public class Event {
//     public static EventApiClient Client = new EventApiClient("events");
//
//     protected class EventApiClient extends BaseApiClient {
//       public EventApiClient(String endpoint) { super(endpoint) }
//       public void getCurrentEvent() { ... }
//     }
//   }
public class BaseApiClient {
    public static final String API_ROOT = "http://www.scpr.org/api/v3/";

    protected String mEndpoint;

    public BaseApiClient(String endpoint) {
        mEndpoint = endpoint;
    }


    public void get(String relativePath,
                    Map<String, String> params,
                    Response.Listener<JSONObject> listener,
                    Response.ErrorListener errorListener) {

        HttpRequest.get(buildUrl(relativePath, params), null, null, listener, errorListener);
    }


    public void getCollection(Map<String, String> params,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {

        HttpRequest.get(buildUrl("", params), null, null, listener, errorListener);
    }


    private String buildUrl(String path, Map<String, String> params) {
        Uri.Builder builder = Uri.parse(API_ROOT).buildUpon()
                .appendPath(mEndpoint)
                .appendPath(path);

        return HttpRequest.addQueryParams(builder.build().toString(), params);
    }
}
