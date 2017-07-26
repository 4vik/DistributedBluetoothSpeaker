package com.example.nicolas.bluetoothspeeker;

import android.Manifest;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;


public class PlayAudio extends Fragment {
    public static PlayAudio instance;

    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;

    private ListView listMusic = null;

    private Timer timer = new Timer();

    private MediaCursorAdapter mediaAdapter = null; //MediaCursorAdapter
    private TextView selectedFile = null;
    private TextView currentTime = null;
    private TextView duration = null;
    private SeekBar seekbar = null;
    //private MediaPlayer player = null; // old stuff, now use realPlayer and assume it can be null

    private MediaPlayer realPlayer = null;

    private ImageButton playButton = null;
    private ImageButton prevButton = null;
    private ImageButton nextButton = null;

    private boolean isStarted = true;
    private String currentFile = "";
    private boolean isMovingSeekBar = false;

    public final Handler handler = new Handler();

    private final Runnable updatePositionRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        instance = this;

        View view = inflater.inflate(R.layout.activity_play_audio, container, false);

        listMusic = (ListView) view.findViewById(R.id.list);
        listMusic.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                currentFile = (String) view.getTag();
                Log.i("PlayAudio", "start playing: " + currentFile);
                String[] parts = currentFile.split("/");
                String songName = parts[parts.length-1];
                setPreparing(songName);
                PartyCreation.instance.serverToClientThread.sendFileToAllandPlay(currentFile, songName);
                // send file
                //startPlay(currentFile);
            }
        });

        selectedFile = (TextView) view.findViewById(R.id.selectedfile);
        currentTime = (TextView) view.findViewById(R.id.currentTime);
        duration = (TextView) view.findViewById(R.id.duration);
        seekbar = (SeekBar) view.findViewById(R.id.seekbar);
        playButton = (ImageButton) view.findViewById(R.id.play);
        prevButton = (ImageButton) view.findViewById(R.id.prev);
        nextButton = (ImageButton) view.findViewById(R.id.next);

        //player = new MediaPlayer();
        //player.setOnCompletionListener(onCompletion);
        //player.setOnErrorListener(onError);
        seekbar.setOnSeekBarChangeListener(seekBarChanged);

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        //String selection = null;
        Cursor cursor = super.getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null , selection, null, null);

        if ( null != cursor) {
            cursor.moveToFirst();

            mediaAdapter = new MediaCursorAdapter(super.getActivity(), R.layout.list_item_music_player, cursor);

            listMusic.setAdapter(mediaAdapter);
            //setListAdapter(mediaAdapter);

            playButton.setOnClickListener(onButtonClick);
            nextButton.setOnClickListener(onButtonClick);
            prevButton.setOnClickListener(onButtonClick);


        }
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler.removeCallbacks(updatePositionRunnable);
        //player.stop();
        //player.reset();
        //player.release();

        timer.cancel();
        timer.purge();

        //player = null;
    }

    // This method is called once the song starts being sent to clients
    public void setPreparing(String songName) {
        selectedFile.setText(songName + " (pre-buffering)");
    }

    public void setPlaying(String songName, final MediaPlayer realPlayer) {
        Log.i("Selected: ", songName);
        this.realPlayer = realPlayer;

        selectedFile.setText(songName);
        seekbar.setProgress(0);


        duration.setText(convertMStoMMSS(realPlayer.getDuration()));
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (realPlayer != null && realPlayer.isPlaying()) {
                    currentTime.post(new Runnable() {
                        @Override
                        public void run() {
                            if (realPlayer != null && realPlayer.isPlaying())
                                currentTime.setText(convertMStoMMSS(realPlayer.getCurrentPosition()));

                        }
                    });
                }
            }
        }, 0, 1000);


        seekbar.setMax(realPlayer.getDuration());
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        updatePosition();

        isStarted = true;

    }

    private String convertMStoMMSS(int milliseconds) {

        int seconds = milliseconds / 1000;

        long s = seconds % 60;
        long m = (seconds / 60) % 60;

        return String.format("%02d:%02d", m,s);
    }

    private void stopPlay() {

        //player.stop();
        //player.reset();
        playButton.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositionRunnable);
        seekbar.setProgress(0);

        isStarted = false;
    }

    private void updatePosition() {

        handler.removeCallbacks(updatePositionRunnable);
        if(realPlayer != null)
            seekbar.setProgress(realPlayer.getCurrentPosition());

        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);


    }

    private class MediaCursorAdapter extends SimpleCursorAdapter {

        public MediaCursorAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c,
                    new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE, MediaStore.Audio.AudioColumns.DURATION},
                    new int[]{R.id.displayname, R.id.title, R.id.duration});

        }

        @Override
        public void bindView(View view, Context context, Cursor cursor){

            TextView title = (TextView) view.findViewById(R.id.title);

            TextView name = (TextView) view.findViewById(R.id.displayname);

            //TextView durationSong = (TextView) view.findViewById(R.id.durationSong);

            name.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));

            /*long durationInMs = Long.parseLong(cursor.getString(
                    cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));

            double durationInMin = ((double)  durationInMs / 1000.0) / 60.0;

            durationInMin = new BigDecimal(Double.toString(durationInMin)).setScale(2, BigDecimal.ROUND_UP).doubleValue();

            duration.setText("" + durationInMin);*/

            view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.list_item_music_player, parent, false);

            bindView(v, context, cursor);

            return v;
        }




    }


    private View.OnClickListener onButtonClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.play: {
                    if(realPlayer != null && realPlayer.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        // TODO: Implement me
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        // TODO: Implement me
                        /*
                        if (isStarted) {
                            player.start();
                            playButton.setImageResource(android.R.drawable.ic_media_pause);
                            updatePosition();
                        } else {
                            startPlay(currentFile);
                        } */
                    }
                    break;
                }
                case R.id.next: {
                    // TODO implement me
                    //int seekto = player.getCurrentPosition() + STEP_VALUE;
                    //if(seekto > player.getDuration())
                    //    seekto = player.getDuration();


                    //player.pause();
                    //player.seekTo(seekto);
                    //player.start();
                    break;

                }
                case R.id.prev: {
                    // TODO implement me
                    /*
                    int seekto = player.getCurrentPosition() - STEP_VALUE;

                    if(seekto < 0)
                        seekto = 0;

                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                    */
                    break;
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp){
            stopPlay();
        }
    };

    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar){
            isMovingSeekBar = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isMovingSeekBar = true;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar,int progress, boolean fromUser) {
            if(isMovingSeekBar) {
                //player.seekTo(progress);

                Log.i("OnSeekBarChangeListener", "onProgressChanged");
            }
        }
    };






}

