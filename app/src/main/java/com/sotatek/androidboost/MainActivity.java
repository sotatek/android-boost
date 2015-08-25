package com.sotatek.androidboost;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

enum AppState {
    LOGIN,
    MAIN
}

public class MainActivity extends BaseActivity {

    ArrayList<String> logs = new ArrayList<>();
    Button mLoginButton;
    Button mChatButton;
    TextView mTitle;
    TextView mChatView;
    TextView mLogContent;
    EditText mNameInput;
    EditText mChatInput;

    String mUserName;
    AppState mState;
    Handler handler = new Handler();

    private Runnable updateData = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(updateData, 5000);
            if (mState == AppState.LOGIN) {
                return;
            }

            tryUpdate();
        }
    };

    private void appendLogs(String text) {
        logs.add(text);
        StringBuilder log = new StringBuilder();
        int len = logs.size();
        for (int i = Math.max(0, len-9); i <= len-1; i++) {
            log.append(logs.get(i) + "\n");
        }

        mLogContent.setText(log.toString());
    }

    private void tryLogin(final String name) {
        JSONObject json = new JSONObject();
        try {
            json.put("action", "login");
            json.put("username", name);
            RequestFactory.getInstance().createRequest(json);
            appendLogs("[CLIENT] Login request sent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tryUpdate() {
        JSONObject json = new JSONObject();
        try {
            json.put("action", "update");
            RequestFactory.getInstance().createRequest(json);
            appendLogs("[CLIENT] Update request sent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void trySendMessage(String name, String msg) {
        JSONObject json = new JSONObject();
        try {
            json.put("action", "chat");
            json.put("username", name);
            json.put("message", msg);
            RequestFactory.getInstance().createRequest(json);
            appendLogs("[CLIENT] Chat request sent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initLoginState() {
        mChatView.setVisibility(View.GONE);
        mChatButton.setVisibility(View.GONE);
        mChatInput.setVisibility(View.GONE);
        mNameInput.setVisibility(View.VISIBLE);
        mLoginButton.setVisibility(View.VISIBLE);
        mTitle.setText("Android Boost Demo");
        mState = AppState.LOGIN;
    }

    private void changeToMainState(String msg) {
        mChatView.setVisibility(View.VISIBLE);
        mChatButton.setVisibility(View.VISIBLE);
        mChatInput.setVisibility(View.VISIBLE);
        mNameInput.setVisibility(View.GONE);
        mLoginButton.setVisibility(View.GONE);
        mTitle.setText(msg);
        mState = AppState.MAIN;
    }

    private void updateChatView(final RequestResult result) {
        StringBuilder s = new StringBuilder();
        try {
            JSONObject data = result.getData();
            JSONArray chatLogs = data.getJSONArray("chatLogs");
            for (int i = 0; i < chatLogs.length(); i++) {
                String chatLog = chatLogs.getString(i);
                s.append(chatLog + "\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        mChatView.setText(s.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = (TextView) findViewById(R.id.titleLabel);
        mChatView = (TextView) findViewById(R.id.chatView);
        mLogContent = (TextView) findViewById(R.id.logContent);
        mLoginButton = (Button) findViewById(R.id.loginButton);
        mChatButton = (Button) findViewById(R.id.chatButton);
        mNameInput = (EditText) findViewById(R.id.nameInput);
        mChatInput = (EditText) findViewById(R.id.chatInput);

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUserName = mNameInput.getText().toString();
                tryLogin(mUserName);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = mChatInput.getText().toString();
                trySendMessage(mUserName, msg);
                mChatInput.setText("");
            }
        });

        initLoginState();
        handler.postDelayed(updateData, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestFinish(final Request request, final RequestResult result) {
        super.onRequestFinish(request, result);
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                String msg = result.getMessage();
                appendLogs("[SERVER] " + msg);
                JSONObject requestData = request.getData();
                String requestAction = "";
                try {
                    requestAction = requestData.getString("action");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (requestAction.equals("login")) {
                    if (result.getCode() == 0) {
                        changeToMainState(msg);
                    } else {
                        Log.d(Const.APP_TAG, "Login failed.");
                    }
                }

                updateChatView(result);
            }
        });
    }
}
