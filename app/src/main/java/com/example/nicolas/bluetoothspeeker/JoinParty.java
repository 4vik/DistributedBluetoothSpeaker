package com.example.nicolas.bluetoothspeeker;

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
public class JoinParty extends AppCompatActivity {

    protected static UUID uuid = UUID.fromString("928a6e0c-b2b9-42d0-8599-80e33ea61786");

    private TextView textView;
    private ListView listview;

    public static final String SYNCHRONIZE =  "SYN";
    public static final String PLAY_SONG = "PLS";
    public static final String SEND_FILE = "SNF";
    public static final String END_FILE = "EDF";
    public static final String WRITE = "WRT";


    private BroadcastReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothSocket bluetoothSocket; //IF CONNECTION_ESTABLISHED, then there is the socket
    private ConnectedThreadDJ connectedThreadDJ; //Thread for handling the connection (read & write)
    private ConnectThread connectThread;

    private List<BluetoothDevice> available_device_list;
    private List<String> available_device_names;
    private ArrayAdapter available_device_adapter;

    boolean CONNECTION_ESTABLISHED = false;

    private File current_song;

    private Map<String, Uri> uriMap = new HashMap<String, Uri>();
    private Map<String, InMemoryMediaDataSource> inMemoryMap = new HashMap<String, InMemoryMediaDataSource>();

    private long offset;
    private ClientMusicPlayer clientMusicPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_party);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(discoverableIntent);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        clientMusicPlayer = new ClientMusicPlayer(this);
        listview = (ListView) findViewById(R.id.joinAvailableDevices);


        available_device_list = new ArrayList<BluetoothDevice>();
        available_device_names = new ArrayList<String>();
        available_device_adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, available_device_names);
        listview.setAdapter(available_device_adapter);
        listview.bringToFront();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //Log.i("Broadcast Receiver", "got Called: " + intent.getAction());

                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    Log.i("BroadcastReceiver", "in the if");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device.getName() == null) return;
                    if(available_device_list.contains(device)){
                        //Log.i("BroadcastReceiver", "Already listed device detected");
                    } else {
                        Log.i("BroadcastReceiver", "New Device: " +device.getName());
                        available_device_list.add(device);
                        available_device_names.add(device.getName() + "\nAvailable");
                        available_device_adapter.notifyDataSetChanged();
                        Log.i("JoinParty", "device_names: " + available_device_names);
                        //Log.i("brjasödlkfj", available_device_adapter.getCount()+"");
                        //Log.i("BroadcastReceiver", device.getName() + available_device_adapter.getCount() + available_device_names.size() + listview.getCount());

                        //available_device_adapter.add(device.getName() + "\nAvailable");
                        //available_device_adapter.notifyDataSetChanged();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int pos, long what){
                Log.i("onItemClickListener", "Pos: " + Integer.toString(pos));
                BluetoothDevice device;

                device = available_device_list.get(pos);
                connectThread = new ConnectThread(device);
                connectThread.start();

            }
        });

        mBluetoothAdapter.startDiscovery();

        // Settings Button
        final Button settings2_btn = (Button) findViewById(R.id.btn_settings);
        settings2_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String passVariable = new String();
                passVariable = "EMPTY";

                if(connectThread != null) {
                    passVariable = connectThread.getConnecteDevice().getAddress();
                }

                Intent settings_intent = new Intent(v.getContext(), Settings.class);
                settings_intent.putExtra("DeviceAdress", passVariable);
                startActivity(settings_intent);
            }
        });

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

    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;
        //private int mNumber;
        //private UUID muuid;

        public ConnectThread(BluetoothDevice device){
            //mNumber = number;
            BluetoothSocket tmp = null;
            mDevice = device;
            //String macAddress = android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), "bluetooth_address");
            //final UUID muuid = UUID.fromString("cd95f9ed-7aba-48c2-a53b-" +  macAddress.replace(":", ""));
            //muuid = UUID.fromString(uuid_prefix + mDevice.getAddress().replace(":", "").substring(0, 10)+mNumber);
            Log.i("ConnectThread", uuid.toString());
            try{
                tmp = mDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException ex) {ex.printStackTrace();}

            mSocket = tmp;
        }


        public void run(){
            mBluetoothAdapter.cancelDiscovery();
            try {

                Log.i("ConnectThread", "Using uuid: " + uuid.toString());
                mSocket.connect();
                CONNECTION_ESTABLISHED = true;

                    /*for (int i = 0; i < 7; i++) {
                        if (i != mNumber)
                            connectThreads[i].cancel();
                    } */
                Log.i("ConnectThread", "Connected");

                //Now we have the connection, close all other Threads trying to connect:


            } catch (Exception ex) {
                try {
                    ex.printStackTrace();
                    //Log.i("connecting", "Trying new number" + ++number);
                    mSocket.close();
                    //number = (number++)%7;
                    //muuid = UUID.fromString(uuid_prefix + mDevice.getAddress().replace(":", "").substring(0, 10)+number);
                    //mSocket = mDevice.createRfcommSocketToServiceRecord(muuid);

                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }

            bluetoothSocket = mSocket;
            JoinParty.this.runOnUiThread(new Runnable() {
                public void run() {
                    //Change appereance and Lists when connected
                    Toast.makeText(JoinParty.this, "Connected", Toast.LENGTH_LONG).show();
                    int index = available_device_list.indexOf(mDevice);
                    available_device_names.remove(index);
                    available_device_names.add(mDevice.getName() +"\nConnected");
                    available_device_adapter.notifyDataSetChanged();
                }
            });

            //Handle the connection
            connectedThreadDJ = new ConnectedThreadDJ(bluetoothSocket);
            connectedThreadDJ.start();
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException ex ){ex.printStackTrace();}
        }


        public BluetoothDevice getConnecteDevice() {
            return mDevice;
        }
    }

    //This thread manages Connections
    private class ConnectedThreadDJ extends Thread {
        private final BluetoothSocket mSocket;
        private InputStream mInputStream;
        private OutputStream mOutputStream;


        //OutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null; //streams for saving mp3
        FileOutputStream f = null;

        private boolean is_reading_song = false;
        private boolean is_playing = false;
        int current_song_size;
        int bitRate;
        int sampleRate;
        //byte[] current_song = new byte[20000000];
        int current_song_position;
        int cp = 0;
        int current_system_time;
        private InMemoryMediaDataSource dataSource;


        public ConnectedThreadDJ(BluetoothSocket socket) {
            mSocket = socket;

            try{
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
            } catch (IOException ex) {ex.printStackTrace();}

        }

        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;
            DataInputStream dataInputStream = new DataInputStream(mInputStream);

            try {
                handleInputNew(dataInputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void write(byte[] bytes){
            try{
                mOutputStream.write(bytes);
            } catch (IOException ex) {ex.printStackTrace();}
        }

        public void cancel() {
            try{
                mSocket.close();
            } catch (IOException ex) {ex.printStackTrace();}
        }


        private void handleInputNew(DataInputStream in) throws Exception{
            final String TAG = "handleInputNew";
            while(true) {
                //I know it's deprecated, but it works and it's handy...
                String line = in.readLine();
                String[] parts = line.split(";");
                String type = parts[0];

                switch (type) {
                    case SYNCHRONIZE:
                        Log.d(TAG, "messageType: synchronise");
                        long globalTime = Long.parseLong(parts[1]);
                        Log.i(TAG, "Global Time: " + globalTime);
                        determineOffset(globalTime);
                        //Log.d(TAG, "set offset to: " + offset);
                        break;

                    case PLAY_SONG:
                        Log.d(TAG, "messageType: playSong");
                        long globalStartTime = Long.parseLong(parts[1]);
                        String songName = parts[2];
                        //String fileName = (String) jsonObject.get("FileName");
                        bufferedOutputStream.flush();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            clientMusicPlayer.play(globalToLocal(globalStartTime), inMemoryMap.get(songName));
                        } else {
                            //this assumes that the whole song is already loaded
                            clientMusicPlayer.play(globalToLocal(globalStartTime), uriMap.get(songName));
                        }
                        break;

                    case SEND_FILE:
                        current_song_size = Integer.parseInt(parts[1]);
                        String fileName = parts[2];
                        Log.d(TAG, "messageType: sendFile, size: " + current_song_position + ", " + fileName);

                        current_song_position = 0;
                        is_playing = false;
                        is_reading_song = true;
                        File sharedMemory = Environment.getExternalStorageDirectory();
                        File dir = new File(sharedMemory.getAbsolutePath() + "/DJ_cache");
                        if(!dir.mkdir()) {
                            Log.d(TAG, "could not mkdir? " + dir.toString());
                        }
                        current_song = new File(dir, fileName);
                        f = new FileOutputStream(current_song);
                        uriMap.put(fileName, Uri.fromFile(current_song));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            dataSource = new InMemoryMediaDataSource(current_song_size);
                            inMemoryMap.put(fileName, dataSource);
                        }
                        bufferedOutputStream = new BufferedOutputStream(f, BUFFER_SIZE);
                        break;

                    case WRITE:
                        //Log.d(TAG, "messageType: write");
                        assert (is_reading_song);
                        int packetSize = Integer.parseInt(parts[1]);
                        byte[] songBuffer = new byte[packetSize];
                        in.readFully(songBuffer);
                        bufferedOutputStream.write(songBuffer);
                        if (dataSource != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                dataSource.write(songBuffer);
                            } else {
                                assert (false);
                            }
                        }
                        bufferedOutputStream.flush();
                        break;

                    case END_FILE:
                        Log.d(TAG, "messageType: endFile");
                        is_reading_song = false;
                        bufferedOutputStream.close();
                        break;
                }


            }

        }

    }

    private void determineOffset(long globalTime){
        offset = globalTime - System.currentTimeMillis();
    }

    private long localToGlobal(long localTime) {
        return localTime + offset;
    }

    private long globalToLocal(long globalTime) {
        return globalTime - offset;
    }

    protected void onDestroy(){
        super.onDestroy();
        setContentView(R.layout.activity_join_party);
        Log.i("onDestroy", "got Called");
        unregisterReceiver(mReceiver);
        try {
            connectThread.cancel();
            connectedThreadDJ.cancel();
            bluetoothSocket.close();
        } catch (Exception ex) {ex.printStackTrace();}
    }

}
