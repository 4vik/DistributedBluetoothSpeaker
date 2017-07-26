package com.example.nicolas.bluetoothspeeker;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class AvailableDevice extends Fragment {
    private ListView availableDeviceListView;
    private List<BluetoothDevice> available_device_list;
    private List<String> available_device_names;
    private ArrayAdapter available_device_adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        available_device_list = new ArrayList<>();
        available_device_names = new ArrayList<>();
        View view = inflater.inflate(R.layout.activity_available_device, container, false);
        availableDeviceListView = (ListView) view.findViewById(R.id.availableDevices);
        available_device_adapter = new ArrayAdapter<String>(view.getContext(), R.layout.list_item, available_device_names);
        availableDeviceListView.setAdapter(available_device_adapter);

        return view;

    }

    public void addDevice(BluetoothDevice device){
        Log.i("AvailableFragment", "addDevice");
        available_device_list.add(device);
        available_device_names.add(device.getName());
        available_device_adapter.notifyDataSetChanged();

    }

    public void removeDevice(BluetoothDevice device){
        available_device_list.remove(device);
        available_device_names.remove(device.getName());
        available_device_adapter.notifyDataSetChanged();
    }

    public boolean containsDevice(BluetoothDevice device) {
        return available_device_list.contains(device);
    }


}