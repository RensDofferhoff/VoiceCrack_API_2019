package com.example.voicecrackclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class RequestLoginActivity extends AppCompatActivity {
    private InputStreamVolleyRequest request;
    private EditText usernameField;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_login);
        setTitle("Request Login");
        Global.storageDir = getFilesDir();

        username = getUsername();
        usernameField = findViewById(R.id.requestLoginUsernameField);
        usernameField.setText(username);
    }

    public void toRecordActivity(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("USER", username);

        startActivity(intent);
    }

    //Obtain solid tone
    public void requestRecoveryButtonPressed(View view) {
        username = usernameField.getText().toString();
        setUsername(username);
        // Instantiate the RequestQueue.
        request = new InputStreamVolleyRequest(Request.Method.GET, Global.serverUrl + "/login/" + username, new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                try {
                    if (response != null) {
                        //Read file name from headers
                        String content = request.responseHeaders.get("Content-Disposition");
                        String filename = Global.solidToneName;

                        try {
                            long lenghtOfFile = response.length;

                            //covert reponse to input stream
                            InputStream input = new ByteArrayInputStream(response);
                            File file = new File(Global.storageDir, filename);
                            map.put("resume_path", file.toString());
                            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                            byte[] data = new byte[1024];

                            long total = 0;

                            int count;
                            while ((count = input.read(data)) != -1) {
                                total += count;
                                output.write(data, 0, count);
                            }

                            output.flush();
                            output.close();
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Unable to download audio file", Toast.LENGTH_LONG).show();
                        }
                    });
                    finish();
                }

                toRecordActivity();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ERROR", error.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "User not found. Please register first.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, null);
        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext(),
                new HurlStack());

        mRequestQueue.add(request);
    }

    private void setUsername(String uname){
        SharedPreferences sharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("username", uname);
        editor.commit();
    }

    private String getUsername(){
        SharedPreferences sharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        return sharedPref.getString("username", "");
    }
}

