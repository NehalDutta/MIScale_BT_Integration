package com.example.btchallengeapp;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.appcompat.app.AppCompatActivity;

/**
 * This class will have the option to scan nearby BLE peripherals and show them in a list
 * User can select a device and go to the next screen to perform next set of operations
 */
public class DeviceScanActivity extends AppCompatActivity {

    private ArrayList<Device> devices = new ArrayList<>();
    private ListView listView;
    private BluetoothLeScanner btScanner;
    private Button startScanningButton;
    private Button stopScanningButton;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public final static String SELECTED_DEVICE_KEY = "selected_device_key";
    private final String LOG_TAG = DeviceScanActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configureBLE();

        startScanningButton = findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(v -> startScanning());

        stopScanningButton = findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(v -> stopScanning());
        stopScanningButton.setVisibility(View.INVISIBLE);

        //here we show the scanned nearby bluetooth devices in a clickable list view
        listView = findViewById(R.id.listView);

        final DeviceArrayAdapter adapter = new DeviceArrayAdapter(this, devices);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //Stop the scanning when user has selected a device. Take the user to the next screen with the chosen device details
                stopScanning();
                Device value = adapter.getItem(position);
                Toast.makeText(getApplicationContext(), value.getDeviceName() + " selected", Toast.LENGTH_SHORT).show();
                Intent myIntent = new Intent(DeviceScanActivity.this, BTConnectActivity.class);
                myIntent.putExtra(SELECTED_DEVICE_KEY, value);
                DeviceScanActivity.this.startActivity(myIntent);
            }
        });
    }

    /**
     * This method would check and enable bluetooth and location permissions needed for
     * bluetooth operations
     */
    private void configureBLE() {
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION));
            builder.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover devices when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {
                    });
                    builder.show();
                }
            }
        }
    }

    /**
     * This method will start scanning for nearby bluetooth devices
     */
    public void startScanning() {
        Log.d(LOG_TAG, "Start scanning for peripherals");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> btScanner.startScan(leScanCallback));
    }

    /**
     * This method will be used to stop the scanning of devices
     */
    public void stopScanning() {
        Log.d(LOG_TAG, "stopping scan");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(() -> btScanner.stopScan(leScanCallback));
    }

    // Function to remove duplicates from an ArrayList
    public ArrayList<Device> removeDuplicates(ArrayList<Device> list) {
        HashSet<Object> seen = new HashSet<>();
        devices.removeIf(e -> !seen.add(e.getDeviceName()));
        return list;
    }

    /**
     * Scancallback for Bluetooth LE scan. Scan results will be reported using these callbacks.
     */
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(LOG_TAG, "On scan result");
            //get the scanned device details and add to the scan list
            BluetoothDevice scannedDevice = result.getDevice();
            Device device = new Device();
            if (scannedDevice.getName() != null && scannedDevice.getAddress() != null) {
                device.setDeviceName(scannedDevice.getName());
            } else {
                device.setDeviceName("Unnamed Device");
            }
            device.setDeviceMacAddress(scannedDevice.getAddress());
            device.setBtDevice(scannedDevice);
            devices.add(device);
            devices = removeDuplicates(devices);
            Log.d(LOG_TAG, "Device :: " + device.getDeviceName() + " Added to the list");
            ((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(LOG_TAG, "SCAN FAILED " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

}