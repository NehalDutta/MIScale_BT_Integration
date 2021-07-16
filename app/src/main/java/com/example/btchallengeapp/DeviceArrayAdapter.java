package com.example.btchallengeapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * This is the adapter for the devices scanned. This will be used by the list view to show the list of devices
 */
public class DeviceArrayAdapter extends ArrayAdapter<Device> {

    public DeviceArrayAdapter(Context context, ArrayList<Device> devices) {
        super(context, 0, devices);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Device device = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.scanned_device, parent, false);
        }
        // Lookup view for data population
        TextView deviceName = convertView.findViewById(R.id.deviceName);
        TextView deviceMacAddress = convertView.findViewById(R.id.deviceMacAddress);
        // Populate the data into the view using the data object
        deviceName.setText(device.getDeviceName());
        deviceMacAddress.setText(device.getDeviceMacAddress());
        // Return the completed view to render on screen
        return convertView;
    }


}
