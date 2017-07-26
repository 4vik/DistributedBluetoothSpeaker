package com.example.nicolas.bluetoothspeeker;

import android.media.AudioManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaDataSource;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.nicolas.bluetoothspeeker.PartyCreation.BUFFER_SIZE;


//TODO Pic visible only after entering
public class Settings extends AppCompatActivity {

    String deviceAdress;

    boolean activated = false;
    boolean calibrated = false;
    boolean isCalibrating = false;

    Toast mToast;

    UIUpdater mUIUpdater;

    AudioManager audioManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        deviceAdress = "EMPTY";

        // Get device adress from passed variable
        Bundle extras = getIntent().getExtras();

        if(extras != null) {
            String value = extras.getString("DeviceAdress");
            deviceAdress = value;
        }

        Log.d("Test", deviceAdress);


        // Activate Button
        final Button activate_btn = (Button) findViewById(R.id.btn_activate);
        activate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(deviceAdress != null && !deviceAdress.equals("EMPTY")) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivity(discoverableIntent);

                BluetoothDistance.create(getApplicationContext(), deviceAdress, true);
                activated = true;

                mToast.makeText(getBaseContext(), "Activated!", Toast.LENGTH_LONG).show();
            }
                else {
                mToast.makeText(getBaseContext(), "Error! Not connected to DJ!", Toast.LENGTH_SHORT).show();
            }
            }
        });

        // Calibrate Button
        final Button calibrate_btn = (Button) findViewById(R.id.btn_calibrate);
        calibrate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAdress != null && !deviceAdress.equals("EMPTY") && activated) {
                    BluetoothDistance.Calibrate();
                    mToast.makeText(getBaseContext(), "Started calibration!", Toast.LENGTH_SHORT).show();
                    isCalibrating = true;
                }
                else {
                    mToast.makeText(getBaseContext(), "Error! Not yet activated!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Start Button
        final Button start_btn = (Button) findViewById(R.id.btn_start);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAdress != null && !deviceAdress.equals("EMPTY") && activated && calibrated) {
                    BluetoothDistance.StartDistanceMeasurement();
                    mToast.makeText(getBaseContext(), "Started distance measurement!", Toast.LENGTH_SHORT).show();
                }
                else {
                    mToast.makeText(getBaseContext(), "Error! Not yet calibrated!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Stop Button
        final Button stop_btn = (Button) findViewById(R.id.btn_stop);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BluetoothDistance.wasCreated && BluetoothDistance.IsMeasuring()) {
                    BluetoothDistance.StopDistanceMeasurement();
                }
                else {
                    mToast.makeText(getBaseContext(), "Error! Controller is not running!", Toast.LENGTH_SHORT).show();
                }
            }
        });



        // INITIALIZE UPDATER (METHOD RUN IS CALLED PERIODICALLY)
        mUIUpdater = new UIUpdater(new Runnable() {
            @Override
            public void run() {
                if(isCalibrating) {
                    if(BluetoothDistance.wasCreated && BluetoothDistance.CheckIfCalibrated()) {
                        mToast.makeText(getBaseContext(), "Finished Calibration", Toast.LENGTH_SHORT).show();
                        isCalibrating = false;
                        calibrated = true;
                    }
                }

                if(BluetoothDistance.wasCreated && BluetoothDistance.IsMeasuring()) {
                    int proximityZone = BluetoothDistance.getLastKnownDistanceZone();

                    audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    double unit = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 3;
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)unit*proximityZone, AudioManager.FLAG_SHOW_UI);
                }
            }
        }, 1000);

        mUIUpdater.startUpdates();

    }
}
