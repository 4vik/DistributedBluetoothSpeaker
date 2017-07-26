package com.example.nicolas.bluetoothspeeker;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.FileDescriptor;
import java.util.concurrent.TimeUnit;


/**
 * Created by judith on 29.11.16.
 */

public class HostMusicPlayer {

    MediaPlayer mp;
    Context myContext;
    long originalStartTime;

    public HostMusicPlayer(Context context) {
       myContext = context;
    }

    // Wird NICHT im UI thread aufgerufen
    public void play(long startTime, FileDescriptor fileDescriptor, final String songName) {
        originalStartTime = startTime;
        if(mp != null){
            mp.reset();
            mp.release();
        }
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(fileDescriptor);
            mp.prepare();
        } catch (Exception ex){ex.printStackTrace();}
        while(System.currentTimeMillis() < startTime) {}
        mp.start();
        PlayAudio.instance.handler.post(new Runnable() {
            @Override
            public void run() {
                PlayAudio.instance.setPlaying(songName, mp);
            }
        });
        Log.i("HostMusicPlayer", "after STart");


        try{
            TimeUnit.SECONDS.sleep(3);
        }catch (Exception ex) {}
        Log.i("HostMusicPlayer", "beforePause");
        mp.pause();
        long newStartOffset = System.currentTimeMillis() - originalStartTime + 100;
        long newStartTime = originalStartTime + newStartOffset;
        mp.seekTo((int) newStartOffset);
        Log.i("HostMusicPlayer", "BeforeRestart");
        while (System.currentTimeMillis() < newStartTime) {}
        mp.start();
    }
}





