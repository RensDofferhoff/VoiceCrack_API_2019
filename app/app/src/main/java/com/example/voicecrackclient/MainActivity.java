package com.example.voicecrackclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    private Boolean recordingPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Welcome to Voice Crack");

        Global.serverUrl = "https://fd6a136f-1492-4038-ab66-b130b94402f3.pub.cloud.scaleway.com:/app";

        requestPermissions();

    }

    public void loginButtonPressed(View view) {
        if (!checkPermissions())
            return;

        Intent intent = new Intent(this, RequestLoginActivity.class);
        startActivity(intent);
    }

    public void toRegisterButtonPressed(View view){
        if (!checkPermissions())
            return;

        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private Boolean checkPermissions(){
        if (recordingPermission){
            return true;
        } else {
            Toast.makeText(getApplicationContext(), "Need recording permission to continue", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private void requestPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        } else {
            recordingPermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    recordingPermission = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
