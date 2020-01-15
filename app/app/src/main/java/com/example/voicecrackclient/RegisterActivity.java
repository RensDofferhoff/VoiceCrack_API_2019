package com.example.voicecrackclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class RegisterActivity extends AppCompatActivity {
    private EditText usernameField;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setTitle("Register");

        usernameField =  findViewById(R.id.usernameField);

        username = getUsername();
        if (!username.equals("")){
            usernameField.setText(username);
        }
    }

    public void registerButtonPressed(View view){
        final String user = usernameField.getText().toString();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);


        StringRequest stringRequest = new StringRequest(Request.Method.GET, Global.serverUrl + "/register/" + user,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        if (response.equals("0")) {
                            setUsername(user);
                            Intent intent = new Intent(getApplicationContext(), EnrollActivity.class);
                            intent.putExtra("USER", user);
                            startActivity(intent);
                        } else if (response.equals("2")){
                            setUsername(user);
                            Intent intent = new Intent(getApplicationContext(), EnrollActivity.class);
                            intent.putExtra("USER", user);
                            startActivity(intent);
                        } else {
                            //Code 1
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "An unexpexted error occured", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(stringRequest);
    }

    public void show(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
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
