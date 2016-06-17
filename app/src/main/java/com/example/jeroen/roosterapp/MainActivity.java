package com.example.jeroen.roosterapp;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends Activity {

    private static final int sampleRate = 8000;
    private AudioRecord audio;
    private int bufferSize;
    private double lastLevel = 0;
    private Thread thread;
    private Thread SendResults;
    private static final int SAMPLE_DELAY = 75;
    private int movements = 0;
    private int minutes = 0;
    private int nrem1 = 0;
    private int nrem2 = 0;
    private int nrem3 = 0;
    private int nrem4 = 0;
    private int rem = 0;
    private int LastPhase = 0;
    private int CurrentPhase = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            bufferSize = AudioRecord
                    .getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }
    }

    protected void onResume() {
        super.onResume();
        audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        audio.startRecording();
        thread = new Thread(new Runnable() {
            public void run() {
                while(thread != null && !thread.isInterrupted()){
                    //Let's make the thread sleep for a the approximate sampling time
                    try{Thread.sleep(SAMPLE_DELAY);}catch(InterruptedException ie){ie.printStackTrace();}
                    readAudioBuffer();//After this call we can get the last value assigned to the lastLevel variable

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if(lastLevel > 0){
                                movements++;
                            }
                        }
                    });
                }
            }
        });
        thread.start();

        SendResults = new Thread(new Runnable() {
            @Override
            public void run() {

            }

            public void CheckPhase(int phase) {
                if (phase != LastPhase) {
                    if (LastPhase == 1) {
                        nrem1 = 0;
                    } else if (LastPhase == 2) {
                        nrem2 = 0;
                    } else if (LastPhase == 3) {
                        nrem3 = 0;
                    } else if (LastPhase == 4) {
                        nrem4 = 0;
                    } else if (LastPhase == 5) {
                        rem = 0;
                    }
                }
            }
                public void send(){
                    while(thread != null && !thread.isInterrupted()){
                        try{Thread.sleep(60000);}catch(InterruptedException ie){ie.printStackTrace();}
                        if (movements > 20){
                            nrem1++;
                            CheckPhase(1);
                            LastPhase = 1;
                        }
                        else if (movements > 15){
                            nrem2++;
                            CheckPhase(2);
                            LastPhase = 2;
                        }
                        else if (movements > 10){
                            nrem3++;
                            CheckPhase(3);
                            LastPhase = 3;

                        }
                        else if (movements > 5){
                            nrem4++;
                            CheckPhase(4);
                            LastPhase = 4;
                        }
                        else if (movements >= 0){
                            rem++;
                            CheckPhase(5);
                            LastPhase = 5;
                        }

                        movements = 0;
                        minutes++;

                    }
                }

                });

                SendResults.start();
            }

            /**
             * Functionality that gets the sound level out of the sample
             */
            private void readAudioBuffer() {

                try {
                    short[] buffer = new short[bufferSize];

                    int bufferReadResult = 1;

                    if (audio != null) {

                        // Sense the voice...
                        bufferReadResult = audio.read(buffer, 0, bufferSize);
                        double sumLevel = 0;
                        for (int i = 0; i < bufferReadResult; i++) {
                            sumLevel += buffer[i];
                        }
                        lastLevel = Math.abs((sumLevel / bufferReadResult));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onPause() {
                super.onPause();
                thread.interrupt();
                thread = null;
                SendResults.interrupt();
                SendResults = null;
                try {
                    if (audio != null) {
                        audio.stop();
                        audio.release();
                        audio = null;
                    }
                } catch (Exception e) {e.printStackTrace();}
            }
        }
