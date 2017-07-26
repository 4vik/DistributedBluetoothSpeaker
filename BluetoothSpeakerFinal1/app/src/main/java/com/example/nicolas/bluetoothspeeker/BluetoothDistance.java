package com.example.nicolas.bluetoothspeeker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


/***
 * HOW TO USE THIS CLASS
 *
 * 1. Call create method
 *      BluetoothDistance.create(getApplicationContext(), connectedDevice, true);
 *
 * 3. Put the two devices (client and DJ) at 1 meter distance and calibrate the measurer - e.g. calibrate using the Calibrate() method
 *      bluetoothDistance.Calibrate();
 *
 * 4. Wait until the calibration is finished - e.g. check with CheckIfCalibrated()
 *      bluetoothDistance.CheckIfCalibrated()
 *
 * 5. Once the calibration is finished, call StartDistanceMeasurement() to start the measurement, the method return true if measurement was successfully started - e.g.
 *      bluetoothDistance.StartDistanceMeasurement()
 *
 * 6. Get proximity zone (0,1,2 or 3) with getLastKnownDistanceZone(). Call this method periodically to always get the newest value - e.g.
 *      bluetoothDistance.getLastKnownDistanceZone()
 */


public class BluetoothDistance {
    private static Context context;

    private static BluetoothAdapter adapter;
    private static String DJDeviceAdress;

    private static UIUpdater mUIUpdater;

    private static int timeBetweenDiscoveries = 2000;

    private static boolean RSSIMeasurement = false;
    private static boolean IsCalibrating = false;
    private static boolean HasCalibrated = false;
    private static boolean isClient = false;

    // Used for RSSI distance measurement (n between 2 and 4, standard is 2)
    private static int txPower = 0;
    private static int n = 2;
    private static double distance = -1.0;

    public static boolean wasCreated = false;

    // Constructor
    public static void create(Context _context, String _DJDeviceAdress, boolean _isClient) {
        wasCreated = true;

        /// / Set values
        context = _context;
        DJDeviceAdress = _DJDeviceAdress;
        isClient = _isClient;

        // Get default bluetooth adapter
        adapter = BluetoothAdapter.getDefaultAdapter();

        // Register receiver
        context.registerReceiver(signalStrengthReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        // Initialize updater (method is called periodically)
        mUIUpdater = new UIUpdater(new Runnable() {
            @Override
            public void run() {
                if(CheckBTStatus() && DJDeviceAdress != null && DJDeviceAdress != "" && isClient && RSSIMeasurement) {
                    if(adapter.isDiscovering())
                        adapter.cancelDiscovery();

                    adapter.startDiscovery();
                }
            }
        }, timeBetweenDiscoveries);
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private static final BroadcastReceiver signalStrengthReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if(RSSIMeasurement || IsCalibrating) {
                String action = intent.getAction();

                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if(device.getAddress().equals(DJDeviceAdress)) {
                        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                        if(RSSIMeasurement && HasCalibrated) {
                            distance = Math.pow(10, (txPower - rssi) / 10 * n);
                        }

                        // Calibrate - txPower = RSSI at 1 meter distance
                        if(IsCalibrating) {
                            distance = 1.0;
                            txPower = rssi;
                            HasCalibrated = true;
                            IsCalibrating = false;
                        }
                    }
                }
            }
        }
    };

    public void Destroy() {
        mUIUpdater.stopUpdates();
        adapter.cancelDiscovery();

        context.unregisterReceiver(signalStrengthReceiver);

        wasCreated = false;
    }

    public void SetDJDevice(String deviceAdress) {
        DJDeviceAdress = deviceAdress;
        HasCalibrated = false;
    }

    // Returns true if calibrate was successfully started, false else
    public static boolean Calibrate() {
        if(DJDeviceAdress != null && DJDeviceAdress != "" && CheckBTStatus()) {
            adapter.startDiscovery();
            IsCalibrating = true;
            return true;
        }
        else {
            return false;
        }
    }

    public static boolean CheckIfCalibrated() {
        return HasCalibrated;
    }

    public static boolean CheckIfCurrentlyCalibrating() { return IsCalibrating; }

    // Returns true if distance measurement successfully started, false else
    public static boolean StartDistanceMeasurement() {
        if(HasCalibrated && DJDeviceAdress != null && DJDeviceAdress != "" && CheckBTStatus() && !RSSIMeasurement) {
            mUIUpdater.startUpdates();
            RSSIMeasurement = true;
            return true;
        }
        else {
            return false;
        }
    }

    public static void StopDistanceMeasurement() {
        mUIUpdater.stopUpdates();
        RSSIMeasurement = false;
        adapter.cancelDiscovery();
    }

    public static boolean IsMeasuring() {
        return RSSIMeasurement;
    }

    /***
     * @return = The proximity zone of the device to the DJ (Zone 1 = Less than 1 meter, Zone 2 = between 1 and 10 meters, Zone 3 = more than 10 meters, Zone 0 = Unknown)
     */
    public static int getLastKnownDistanceZone() {
        if(distance < 1)
            return 1;
        else if (distance >= 1 && distance < 10)
            return 2;
        else if (distance >= 10)
            return 3;
        else
            return 0;
    }

    /***
     * @param ms = Milliseconds between updates
     */
    public static void SetUpdateFrequency(int ms) {
        timeBetweenDiscoveries = ms;
    }

    private static boolean CheckBTStatus() {
        if(adapter.isEnabled())
            return true;
        else {
            return false;
        }
    }
}
