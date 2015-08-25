package com.sotatek.androidboost;

import android.util.Log;

import org.json.JSONObject;

import java.util.Hashtable;

/**
 * Created by nguyen.an on 8/23/15.
 */
public class RequestFactory {

    private BaseActivity currentActivity;
    private Hashtable<Integer, Request> requests = new Hashtable<>();

    private RequestFactory() {}

    private static RequestFactory instance;
    public static RequestFactory getInstance() {
        if (instance == null) {
            instance = new RequestFactory();
        }
        return instance;
    }


    public void setCurrentActivity(BaseActivity a) {
        currentActivity = a;
    }
    public BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    public Request createRequest(JSONObject data) {
        Request request = new Request(Const.SERVER_ADDRESS, Const.SERVER_PORT, data);

        int requestId = request.getId();
        requestAsync(requestId, Const.SERVER_ADDRESS, Const.SERVER_PORT, data.toString());
        requests.put(requestId, request);

        return request;
    }

    public Request createRequest(String host, int port, JSONObject data) {
        Request request = new Request(host, port, data);

        int requestId = request.getId();
        requestAsync(requestId, Const.SERVER_ADDRESS, Const.SERVER_PORT, data.toString());
        requests.put(requestId, request);

        return request;
    }

    public Request getRequest(int requestId) throws Exception {
        return requests.get(requestId);
    }

    public static void onJNICallback(int id, String data) {
        Log.d(Const.APP_TAG, "RequestFactory onJNICallback: id=" + id + ", data=" + data);
        try {
            JSONObject json = new JSONObject(data);
            Request request = getInstance().getRequest(id);
            getInstance().getCurrentActivity().onRequestFinish(request, new RequestResult(json.getInt("code"), json.getString("msg"), json.optJSONObject("data")));
        }
        catch (Exception e) {
            e .printStackTrace();
        }
    }

    private native boolean requestAsync(int id, String host, int port, String data);

    static {
        System.loadLibrary("test-boost");
    }
}
