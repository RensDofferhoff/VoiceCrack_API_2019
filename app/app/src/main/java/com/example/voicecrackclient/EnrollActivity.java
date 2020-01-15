package com.example.voicecrackclient;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class EnrollActivity extends AppCompatActivity {
    private TextView enrollInstructionView;
    private String username;
    private final String sentence = "You can get in without your password";

    private MediaPlayer player;
    private RecordingTask recordingTask;
    private Boolean recording = false;

    private Button playButton;
    private Button submitButton;
    private Button recordButton;
    private TextView infoView;
    private TextView sentenceView;

    private int timerCounter = 0;
    private Timer timer;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);
        setTitle("Enroll");
        Global.storageDir = getFilesDir();

        recordButton = findViewById(R.id.enrollRecordButton);
        infoView = findViewById(R.id.enrollInfoView);
        playButton = findViewById(R.id.enrollPlayButton);
        submitButton = findViewById(R.id.submitEnrollButton);
        enrollInstructionView = findViewById(R.id.enrollInstructionView);
        sentenceView = findViewById(R.id.sentenceView);
        username = getIntent().getStringExtra("USER");
        setInstruction(sentence);
        gson = new Gson();
    }

    private void setInstruction(String sentence){
        String text = "Please say a sentence";
        enrollInstructionView.setText(text);
    }

    private void showInfo(final String info){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoView.setText(info);
            }
        });
    }

    public void enrollRecordButtonPressed(View view){
        sentenceView.setText("");
        if (recording){
            recordingTask.stop();
            recordingTask = null;
            playButton.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);

            timer.cancel();
            timerCounter = 0;
            showInfo("Saved recording");
            recordButton.setText("Record");
            recording = false;
        } else {
            recording = true;
            recordButton.setText("Stop recording");
            playButton.setVisibility(View.INVISIBLE);
            submitButton.setVisibility(View.INVISIBLE);
            recordingTask = new RecordingTask();
            recordingTask.record(Global.storageDir + "/" + Global.recordingName);

            timer = new Timer();
            timerCounter = 0;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    showInfo("Recording (" + Integer.toString(timerCounter) + ")");
                    timerCounter++;
                }

            },0, 1000);
        }
    }

    private void processMSResponse(String s){
        String jsonString = s.substring(2, s.length()-1);
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        if (jsonObject.has("error")){
            JsonObject error = jsonObject.getAsJsonObject("error");
            String errorCode = error.getAsJsonPrimitive("code").getAsString();
            String errorMsg = error.getAsJsonPrimitive("message").getAsString();
            showInfo("Error; " + errorCode + ": " + errorMsg);
        } else {
            String enrollmentStatus = jsonObject.getAsJsonPrimitive("enrollmentStatus").getAsString();
            int enrollmentsCount = jsonObject.getAsJsonPrimitive("enrollmentsCount").getAsInt();
            int enrollmentsRemaining = jsonObject.getAsJsonPrimitive("remainingEnrollments").getAsInt();
            String phrase = jsonObject.getAsJsonPrimitive("phrase").getAsString();

            sentenceView.setText("Phrase: \n" + phrase);

            if (enrollmentStatus.equals("Enrolled")){
                showInfo("Enrolled: " + enrollmentsCount + " samples");
            } else {
                showInfo("Remaining: " + enrollmentsRemaining + " samples");
            }
        }
    }

    public void enrollSubmitButtonPressed(View view){
        try {
            final String s = new UploadTask().execute(Global.storageDir + "/" + Global.recordingName, Global.serverUrl + "/enroll/" + username).get();
            processMSResponse(s);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void enrollPlayButtonPressed(View view){
        playRecording(new File(Global.storageDir + "/" + Global.recordingName));
    }

    public void playRecording(File file){
        if(file.exists()) {
            player = new MediaPlayer();
            try {
                FileInputStream fis = new FileInputStream(file);
                player.setDataSource(fis.getFD());
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

    @Override
    public void onPause(){
        super.onPause();
        if (recordingTask != null){
            recordingTask.stop();
            recordingTask = null;
        }
    }

    public void show(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }
}
