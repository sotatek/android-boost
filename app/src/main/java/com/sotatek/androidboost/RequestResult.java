package com.sotatek.androidboost;

import org.json.JSONObject;

/**
 * Created by nguyen.an on 8/23/15.
 */
public class RequestResult {

    public RequestResult(int _code, String _message, JSONObject _data) {
        code = _code;
        message = _message;
        data = _data;
    }

    int code;
    String message;
    JSONObject data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public JSONObject getData() {
        return data;
    }
}
