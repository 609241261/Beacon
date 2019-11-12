package com.bluetooth.beacon;

import android.support.v7.app.AppCompatActivity;
import android.util.*;
import android.view.View;
import android.widget.*;
import android.os.Bundle;

import com.tsy.sdk.myokhttp.MyOkHttp;
import com.tsy.sdk.myokhttp.response.JsonResponseHandler;
import com.tsy.sdk.myokhttp.response.RawResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    protected static final String TAG = "MonitoringActivity";
    private android.widget.TextView textView;
    private Button login;
    private Button send_request;
    private Button send_message;


    //保存cookie
    CookieJar cookie = new CookieJar() {
        private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<Cookie>();
        }
    };
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .cookieJar(cookie)
            .build();
    MyOkHttp mMyOkHttp = new MyOkHttp(okHttpClient);


    private void initView() {
        login = (Button) findViewById(R.id.login);
        send_request = (Button) findViewById(R.id.send_request);
        send_message = (Button) findViewById(R.id.send_message);
        textView = (TextView) findViewById(R.id.response_data);

        login.setOnClickListener(this);
        send_request.setOnClickListener(this);
        send_message.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();


    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:
                login();
                break;
            case R.id.send_request:
                send_request();
                break;
            case R.id.send_message:
                send_message();
                break;
        }
    }

    public void login(){
        try {
            String url = "http://192.168.137.1:8080/login?name=demo&password=demo";
            Map<String, String> params = new HashMap<>();
            params.put("name", "demo");
            params.put("password", "demo");

            mMyOkHttp.post()
                    .url(url)
                    .params(params)
                    .tag(this)
                    .enqueue(new RawResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, String response) {
                            Log.d(TAG, "doGet onSuccess:" + response);
                            show(response);
                        }

                        @Override
                        public void onFailure(int statusCode, String error_msg) {
                            Log.d(TAG, "doGet onFailure:" + error_msg);
                        }
                    });
        }catch (Exception e){
            Log.d(TAG, "doGet onFailure2:" + e);

        }
    }

    private void send_request() {
        String url = "http://192.168.137.1:8080//api/policy?vid=121234";
        mMyOkHttp.get()
                .url(url)
                .tag(this)
                .enqueue(new JsonResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, JSONObject response) {
                        Log.d(TAG, "doPost onSuccess JSONObject:" + response);
                    }

                    @Override
                    public void onSuccess(int statusCode, JSONArray response) {
                        Log.d(TAG, "doPost onSuccess JSONArray:" + response);
                        show(response);
                    }

                    @Override
                    public void onFailure(int statusCode, String error_msg) {
                        Log.d(TAG, "doPost onFailure:" + error_msg);
                    }
                });
    }

    private void send_message() {
        try {
            Log.d(TAG, "doGet" );
            String url = "http://10.0.1.114:5000/api/register?addr=40:45:da:c7:88:fd&imei=867400021132547";
            Map<String, String> params = new HashMap<>();
            params.put("addr", "40:45:da:c7:88:fd");
            params.put("imei", "867400021132547");

            mMyOkHttp.post()
                    .url(url)
                    .params(params)
                    .tag(this)
                    .enqueue(new RawResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, String response) {
                            Log.d(TAG, "doGet onSuccess:" + response);
                            show(response);
                        }

                        @Override
                        public void onFailure(int statusCode, String error_msg) {
                            Log.d(TAG, "doGet onFailure:" + error_msg);
                        }
                    });
        }catch (Exception e){
            Log.d(TAG, "doGet onFailure2:" + e);

        }
    }

    private void show(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(result);
            }
        });
    }

    private void show(final JSONArray result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(result.toString());
            }
        });
    }
}
