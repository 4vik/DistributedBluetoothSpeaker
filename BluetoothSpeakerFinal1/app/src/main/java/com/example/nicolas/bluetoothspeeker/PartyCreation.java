package com.example.nicolas.bluetoothspeeker;


import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyCreation extends FragmentActivity {

    public static PartyCreation instance = null;
    protected static UUID uuid = UUID.fromString("928a6e0c-b2b9-42d0-8599-80e33ea61786");

    private ViewPager viewPager;
    private Tabsadapter sectionsPagerAdapter;
    private AvailableDevice availableDevicesFragment;
    private ConnectedDevices connectedDevicesFragment;
    private MusicPlayer musicPlayerFragment;

    private static final String TAG = "PartyCreation";
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mReceiver;
    private List<BluetoothSocket> bluetoothSocketList;
    private int connectionCounter = 0;

    private List<ConnectedThreadDJ> connectedThreadList = new ArrayList<ConnectedThreadDJ>();
    private AcceptThread mAcceptThread;
    public static final int BUFFER_SIZE = 2048;
    public ServerToClientThread serverToClientThread;
    private HostMusicPlayer hostMusicPlayer;

    private int mState;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_CHANGE = 4;

    public static final String SYNCHRONIZE =  "SYN";
    public static final String PLAY_SONG = "PLS";
    public static final String SEND_FILE = "SNF";
    public static final String END_FILE = "EDF";
    public static final String WRITE = "WRT";


    private Map<String, FileDescriptor> hashMap = new HashMap<String, FileDescriptor>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_creation);

        ActivityCompat.requestPermissions(PartyCreation.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        startActivity(discoverableIntent);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // is useless? who should see that?
        // mState = STATE_LISTEN;
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        mState = STATE_CONNECTING;


        serverToClientThread = new ServerToClientThread();
        serverToClientThread.start();
        hostMusicPlayer = new HostMusicPlayer(this);

        bluetoothSocketList = new ArrayList<>();


        // Settings Button
        // Leave a party
        final Button settings_btn = (Button) findViewById(R.id.leave);
        settings_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent settings_intent = new Intent(v.getContext(), LeaveParty.class);
                startActivity(settings_intent);
            }
        });



    }

    private void initializeReceiver(){
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i("Broadcast Receiver", "got Called: " + intent.getAction());

                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    //  Log.i("BroadcastReceiver", "in the if");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device.getName() == null) return;
                    if(availableDevicesFragment.containsDevice(device)){
                          Log.i("BroadcastReceiver", "Already listed device detected");
                    } else {
                        Log.i("BroadcastReceiver", "New device detected: " + device.getName());
                        availableDevicesFragment.addDevice(device);
                    }
                    Log.i("broadcastReceiver", "im finished");
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        // Now that handler is registered, start discovery

        mBluetoothAdapter.startDiscovery();

        Log.i("initializeReceiver", "done");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted and now can proceed
                    //a sample method called
                    // Initialisation
                    viewPager = (ViewPager) findViewById(R.id.tabspager);
                    sectionsPagerAdapter = new Tabsadapter(getSupportFragmentManager());
                    TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
                    tabLayout.setupWithViewPager(viewPager);
                    viewPager.setAdapter(sectionsPagerAdapter);

                    /** the ViewPager requires a minimum of 1 as OffscreenPageLimit */
                    int limit = (sectionsPagerAdapter.getCount() > 1 ? sectionsPagerAdapter.getCount() - 1 : 1);
                    viewPager.setOffscreenPageLimit(limit);

                    TextView tabOne = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
                    tabOne.setText("Available\r\ndevices");
                    tabLayout.getTabAt(0).setCustomView(tabOne);

                    TextView tabTwo = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
                    tabTwo.setText("Connected\r\ndevices");
                    tabLayout.getTabAt(1).setCustomView(tabTwo);

                    TextView tabThree = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
                    tabThree.setText("Music\r\nPlayer");
                    tabLayout.getTabAt(2).setCustomView(tabThree);

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(PartyCreation.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // add other cases for more permissions
        }
    }


    //This is the server role Thread for the DJ
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;
        private BluetoothSocket tmpbs;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("MusicPlayer-DJ", uuid);

            }catch (IOException ex) {ex.printStackTrace();}
            mServerSocket = tmp;
        }

        public void run(){
            Log.i("AcceptThread", "In the thread, run()");

            while (true) {
                //synchronized (myLock) {
                try {
                    Log.i("whileloop", "Before accept()");
                    tmpbs = mServerSocket.accept();
                    Log.i("whileloop", "After accept()");
                    if (tmpbs == null) {
                        Log.i("AcceptThread", "Socket null");
                    } else {
                        Log.i("AcceptThread", "Connection established");
                        mState = STATE_CONNECTED;

                        //Manage lists with changing device from available to connected
                        final BluetoothDevice bld = tmpbs.getRemoteDevice();
                        String name = bld.getName();


                        Log.i("AcceptThread", "managedLists");
                        final String connInfo = "Newest connected Device: " + name + "\nNumber of connected Devices: " + (1 + connectionCounter);
                        PartyCreation.this.runOnUiThread(new Runnable() {
                            public void run() {
                                connectedDevicesFragment.addDevice(bld);
                                availableDevicesFragment.removeDevice(bld);
                                //connectionInfo.setText(connInfo);
                                Log.i("AcceptThread", "Im on UI Thread");
                            }
                        });
                        Log.i("AcceptThread", "Connection Info updated");
                        //connectionInfo.setText(connInfo);
                        bluetoothSocketList.add(tmpbs);
                        //connectedThreadDJ[connectionCounter] = new ConnectedThreadDJ(bluetoothSockets[connectionCounter]);
                        connectedThreadList.add(new ConnectedThreadDJ((bluetoothSocketList.get(connectionCounter))));
                        connectedThreadList.get(connectionCounter).start();
                        //connectedThreadDJ[connectionCounter].start();
                        connectionCounter++;
                        Log.i("AcceptThread", "Started new connectedThread");


                        //Wait for following connections
                        //cancel();
                        //mAcceptThread = new AcceptThread(connectionCounter);
                        //mAcceptThread.run();

                        //cancel();
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    //break;
                }
            }
        }


        //}

        public void cancel(){
            try {
                mServerSocket.close();
            } catch (IOException ex) {ex.printStackTrace();}
        }
    }

    //This thread manages Connections
    private class ConnectedThreadDJ extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public ConnectedThreadDJ(BluetoothSocket socket) {
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException ex) {ex.printStackTrace();}

            mInputStream = tmpIn;
            mOutputStream = tmpOut;
            Log.i("ConnectedThread", "Im created");
        }

        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;
            serverToClientThread.synchronise();

            while(true) {
                try{
                    Log.i("ConnectedThread", "Waiting for Input");
                    bytes = mInputStream.read(buffer);

                    //reads all input and shows it in a toast
                    final String hello = new String(buffer);
                    PartyCreation.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(PartyCreation.this, hello, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (IOException ex) {ex.printStackTrace(); break;}
            }
        }

        public void write(byte[] bytes){
            try{
                mOutputStream.write(bytes);
            } catch (IOException ex) {ex.printStackTrace();}
        }

        public void write(byte[] bytes, int size) {
            try {
                mOutputStream.write(bytes, 0, size);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try{
                mSocket.close();
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException ex) {ex.printStackTrace();}
        }
    }


    public class ServerToClientThread extends Thread {

        private boolean is_playing;
        private Handler handler;

        public ServerToClientThread() {}

        public void run() {
            Log.i("ServerToClientThread", "Waiting for stuff to do");
            is_playing = false;
            Looper.prepare();
            handler = new Handler();
            Looper.loop();
        }

        public void sendFileToAllandPlay(final String absolutePath, final String songName){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String TAG = "sendFileToAllandPlay";
                    Log.i(TAG, "sendFileToAll enters UI Thread");
                    FileDescriptor fileDescriptor;

                    InputStream inputStream;
                    try {
                        // Create FD for host it to hashmap, used as source for the Host player
                        FileInputStream myInputStream = new FileInputStream(absolutePath);
                        fileDescriptor = myInputStream.getFD();
                        hashMap.put(songName, fileDescriptor);

                        // open inputstream to read from and send to clients
                        inputStream = new FileInputStream(absolutePath);
                    } catch (FileNotFoundException ex) {
                        Log.i("sendFileToAll", "File not found!");
                        ex.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    //String TAG = "sendFile";
                    byte[] myBuffer = new byte[BUFFER_SIZE];
                    int len;
                    int current_progress = 0;

                    try {

                        String fileType = SEND_FILE + ";" + inputStream.available()+";" + songName + "\n";
                        for(PartyCreation.ConnectedThreadDJ ctd : connectedThreadList) {
                            ctd.write(fileType.getBytes());
                        }
                        Log.i(TAG, "Start Sending, got " + connectedThreadList.size() + " clients");
                        while((len = inputStream.read(myBuffer)) != -1) {
                            //Log.i(TAG, "While loop, len = " + len);
                            current_progress += len;
                            if(!is_playing && current_progress > 300000){
                                Log.i(TAG, "send a playAll");
                                playAll(System.currentTimeMillis() + 1000, songName);
                                is_playing = true;
                            }
                            for(PartyCreation.ConnectedThreadDJ ctd : connectedThreadList) {
                                String header = WRITE + ";" + len + "\n";
                                ctd.write(header.getBytes());
                                ctd.write(myBuffer, len);
                            }
                        }
                        String fileEnd = END_FILE + "\n";
                        for(PartyCreation.ConnectedThreadDJ ctd : connectedThreadList)
                            ctd.write(fileEnd.getBytes());
                        Log.i(TAG, "Stop Sending");
                    } catch (Exception ex){ex.printStackTrace();}


                }
            });

            return;
        }


        public void synchronise(){
            String TAG = "synchronise";
            Log.i(TAG, "start");
            byte[] dataArray;

            Long globalTime;

            for(PartyCreation.ConnectedThreadDJ ctd : connectedThreadList){
                //byte[] bufferArray = new byte[BUFFER_SIZE];
                globalTime = System.currentTimeMillis();
                String data = SYNCHRONIZE + ";" + globalTime.toString() + "\n";
                //dataArray = data.getBytes();
                //System.arraycopy(dataArray, 0, bufferArray, 0, dataArray.length);
                //ctd.write(bufferArray);
                ctd.write(data.getBytes());
            }

        }

        public void playAll(final long startTime, final String songName) {
            Log.i("play", "at start");
            String data = PLAY_SONG + ";" + startTime + ";" + songName + "\n";

            for(PartyCreation.ConnectedThreadDJ ctd : connectedThreadList) {
                ctd.write(data.getBytes());
            }
            hostMusicPlayer.play(startTime, hashMap.get(songName), songName);
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        setContentView(R.layout.activity_party_creation);
        Log.i("onDestroy", "got Called");
        unregisterReceiver(mReceiver);
        for(ConnectedThreadDJ ctd : connectedThreadList) {
            ctd.cancel();
            //mAcceptThread.cancel();
        }
        mAcceptThread.cancel();
    }

    public class Tabsadapter  extends FragmentPagerAdapter {

        private int TOTAL_TABS = 3;

        public Tabsadapter(FragmentManager fm) {
            super(fm);
            // TODO Auto-generated constructor stub
        }

        @Override
        public Fragment getItem(int index) {
            // TODO Auto-generated method stub
            switch (index) {
                case 0:
                    return new AvailableDevice();

                case 1:
                    return new ConnectedDevices();

                case 2:
                    return new MusicPlayer();

            }

            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    Log.i("instantiateItem", "0");
                    availableDevicesFragment = (AvailableDevice) createdFragment;
                    initializeReceiver();
                    break;
                case 1:
                    Log.i("instantiateItem", "1");
                    connectedDevicesFragment = (ConnectedDevices) createdFragment;
                    break;
                case 2:
                    Log.i("instantiateItem", "2");
                    musicPlayerFragment = (MusicPlayer) createdFragment;
                    break;
            }

            return createdFragment;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return TOTAL_TABS;
        }


    }

}