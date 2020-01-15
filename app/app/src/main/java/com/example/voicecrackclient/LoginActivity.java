package com.example.voicecrackclient;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class LoginActivity extends AppCompatActivity {
    private final int RECORD_N_SECONDS = 4;
    private MediaPlayer player;
    private Button recordButton;
    private TextView resultView;
    private TextView infoView;
    private RecordingTask recordingTask;
    private String username;
    private int timerCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Record voice");
        recordButton = findViewById(R.id.recordButton);
        resultView = findViewById(R.id.resultView);
        infoView = findViewById(R.id.loginInfoView);
        username = Objects.requireNonNull(getIntent().getExtras()).getString("USER");
    }

    public void recordButtonPressed(View view){
        playAndRecord();
    }

    void playAudio(File file, Boolean loop){
        if(file.exists()) {
            player = new MediaPlayer();
            try {
                FileInputStream fis = new FileInputStream(file);
                player.setDataSource(fis.getFD());
                player.setLooping(loop);
                player.prepare();
                player.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("ERROR", "No audio recording found");
            show("No audio recording found");
        }
    }

    private void sendToServer(){
        infoView.setText("");
        try {
            Looper.prepare();
            final String s = new UploadTask().execute(Global.storageDir + "/" + Global.recordingName, Global.serverUrl + "/authenticate/" + username).get();
            showResult(s);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void showResult(final String result){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //show("Response: " + result);
                switch(result){
                    case "0":
                        resultView.setText("Accepted");
                        break;
                    case "1":
                        resultView.setText("Denied by MS");
                        break;
                    case "2":
                        resultView.setText("Denied: missing tone");
                        break;
                    case "3":
                        resultView.setText("Denied: detected replay attack");
                        break;
                    case "-1":
                        resultView.setText("An error occurred");
                        break;
                }
            }
        });
    }

    private void showInfo(final String info){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultView.setText(info);
            }
        });
    }

    public void playAndRecord(){
        recordButton.setVisibility(View.INVISIBLE);
        final Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                playAudio(new File(Global.storageDir + "/" + Global.solidToneName), true);
            }
        });
        playThread.start();

        recordingTask = new RecordingTask();
        recordingTask.record(Global.storageDir + "/" + Global.recordingName);
        showInfo("Recording (" + RECORD_N_SECONDS + ")");

        final Timer timer = new Timer();


        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerCounter++;
                String timeLeft = Integer.toString(RECORD_N_SECONDS-timerCounter);
                showInfo("Recording (" + timeLeft + ")");

                if (timerCounter >= RECORD_N_SECONDS){
                    showInfo("Waiting for response...");
                    timer.cancel();

                    player.stop();
                    player.reset();

                    if(recordingTask != null) {
                        recordingTask.stop();
                        recordingTask = null;
                    }

                    playThread.interrupt();

                    sendToServer();


                    timerCounter = 0;
                }
            }

        },1000, 1000);


    }

    public void show(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause(){
        super.onPause();
        if (recordingTask != null){
            recordingTask.stop();
            recordingTask = null;
        }

        if (player != null && player.isPlaying()){
            player.stop();
        }
    }
}
