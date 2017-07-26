package com.example.nicolas.bluetoothspeeker;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class ConnectedDevices extends Fragment {

    private ListView connectedDeviceListView;
    private List<BluetoothDevice> connected_device_list;
    private List<String> connected_device_names;
    private ArrayAdapter connected_device_adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        connected_device_list = new ArrayList<>();
        connected_device_names = new ArrayList<>();
        View view = inflater.inflate(R.layout.activity_connected_devices, container, false);
        connectedDeviceListView = (ListView) view.findViewById(R.id.connectedDevices);
        connected_device_adapter = new ArrayAdapter<String>(view.getContext(), R.layout.list_item, connected_device_names);
        connectedDeviceListView.setAdapter(connected_device_adapter);

        return view;
    }

    public void addDevice(BluetoothDevice device){
        connected_device_list.add(device);
        connected_device_names.add(device.getName());
        connected_device_adapter.notifyDataSetChanged();

    }

    public void removeDevice(BluetoothDevice device){
        connected_device_list.remove(device);
        connected_device_names.remove(device.getName());
        connected_device_adapter.notifyDataSetChanged();
    }

    public boolean containsDevice(BluetoothDevice device) {
        return connected_device_list.contains(device);
    }
}