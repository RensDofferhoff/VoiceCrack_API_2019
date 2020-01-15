package com.example.voicecrackclient;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

class RecordingTask {
    private AsyncRecordingTask recordingTask;
    private MediaRecorder recorder;
    private String outputPath;

    public RecordingTask() {

    }

    public void record(String path) throws RuntimeException{
        outputPath = path;
        recordingTask = new AsyncRecordingTask();
        recordingTask.execute();
    }

    public void stop(){
        recorder.stop();
        recorder.reset();
        recordingTask.cancel(true);
    }

    private class AsyncRecordingTask extends AsyncTask<String, String, String> {
        final int outputFormat = MediaRecorder.OutputFormat.MPEG_4;
        final int audioEncoder = MediaRecorder.AudioEncoder.AAC;
        final int bitDepth = 16;
        final int sampleRate = 44100;
        final int bitRate = sampleRate * bitDepth;

        @Override
        protected String doInBackground(String... strings) throws RuntimeException {
            if (recorder == null) {
                recorder = new MediaRecorder();
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(outputFormat);
            recorder.setAudioEncoder(audioEncoder);
            recorder.setAudioChannels(1);
            recorder.setOutputFile(outputPath);
            recorder.setAudioEncodingBitRate(bitRate);
            recorder.setAudioSamplingRate(sampleRate);

            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.d("ERROR", "Failed preparing the MediaRecorder");
                e.printStackTrace();
            }

            recorder.start();

            return "Done";
        }

        @Override
        protected void onProgressUpdate(String... strings) {

        }

        @Override
        protected void onPostExecute(String result) {

        }
    }
}