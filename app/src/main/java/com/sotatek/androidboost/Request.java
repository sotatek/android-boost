package com.sotatek.androidboost;

import org.json.JSONObject;

/**
 * Created by nguyen.an on 8/23/15.
 */
public class Request {

    public static int nextId;

    public Request(String _host, int _port, JSONObject _data) {
        id = nextId;
        nextId++;

        host = _host;
        port = _port;
        data = _data;
    }

    int id;
    String host;
    int port;
    JSONObject data;

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public JSONObject getData() {
        return data;
    }
}
