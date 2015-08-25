package com.sotatek.androidboost;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by nguyen.an on 8/23/15.
 */
public class BaseActivity extends AppCompatActivity implements IRequestHandler {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RequestFactory.getInstance().setCurrentActivity(this);
    }

    @Override
    public void onRequestFinish(Request request, RequestResult result) {
        Log.d(Const.APP_TAG, "onRequestFinish: id=" + request.getId() + ", message=" + result.getMessage());
    }
}
