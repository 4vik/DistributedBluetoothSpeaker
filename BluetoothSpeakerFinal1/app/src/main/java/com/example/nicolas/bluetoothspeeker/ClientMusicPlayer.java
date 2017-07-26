package com.example.nicolas.bluetoothspeeker;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;


/**
 * Created by judith on 30.11.16.
 */

//TODO: Make own thread
public class ClientMusicPlayer {

    private static final String TAG = "ClientMusicPlayer";
    MediaPlayer mp;
    Context myContext;
    long originalStartTime;
    Uri currentSongUri;
    InMemoryMediaDataSource currentSongSource;
    boolean isUri;
    //SongCache songCache;

    public ClientMusicPlayer(Context context) {
        myContext = context;
    }


    //maybe runInUIThread ..? (like HostMusicPlayer play())
    public void play(long startTime, Uri uri) {
        isUri = true;
        currentSongUri = uri;
        originalStartTime = startTime;
        if(mp != null){
            mp.reset();
            mp.release();
        }
        mp = new MediaPlayer();
        mp.setOnCompletionListener(onCompletion);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(myContext, uri);

            mp.prepare();
        } catch (Exception ex){ex.printStackTrace();}

        Log.d(TAG, "playMusic()" + uri);

        //busy waiting in order for correct time for playing the music
        while (System.currentTimeMillis() < startTime) {
        }
        mp.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void play(long startTime, InMemoryMediaDataSource source) {
        isUri = false;
        currentSongSource = source;
        originalStartTime = startTime;
        if(mp != null){
            mp.reset();
            mp.release();
        }
        mp = new MediaPlayer();
        mp.setOnCompletionListener(onCompletion);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(source);

            mp.prepare();
        } catch (Exception ex){ex.printStackTrace();}

        Log.d(TAG, "playMusic()" + source);

        //busy waiting in order for correct time for playing the music
        while (System.currentTimeMillis() < startTime) {
        }
        mp.start();
    }

    private void onCompletionUri() throws Exception{
        mp.reset();
        mp.setDataSource(myContext, currentSongUri);
        mp.prepare();
        long newStartOffset = System.currentTimeMillis() - originalStartTime + 100;
        long newStartTime = originalStartTime + newStartOffset;
        mp.seekTo((int) newStartOffset);

        while (System.currentTimeMillis() < newStartTime) {}
        mp.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void onCompletionSource() throws Exception {
        mp.reset();
        mp.setDataSource(currentSongSource);
        mp.prepare();
        long newStartOffset = System.currentTimeMillis() - originalStartTime + 100;
        long newStartTime = originalStartTime + newStartOffset;
        mp.seekTo((int) newStartOffset);

        while (System.currentTimeMillis() < newStartTime) {}
        mp.start();
    }

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp){
            try {
                if (isUri) {
                    onCompletionUri();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        onCompletionSource();
                }
            } catch (Exception ex){ex.printStackTrace();}
        }
    };


}
