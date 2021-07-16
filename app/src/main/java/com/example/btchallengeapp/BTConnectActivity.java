package com.example.btchallengeapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Objects;
import java.util.UUID;

/**
 * This class will show the peripheral which the user has selected from the scanned peripheral list on the previous screen.
 * There will be an option to connect to the bluetooth peripheral. Once user connects to the peripheral, this will
 * read the body weight composition scale parameters and show them in a list view.
 */
public class BTConnectActivity extends AppCompatActivity {

    private Device selectedDevice;
    private static final UUID SCALE_DATA = UUID.fromString("00002a9c-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID SERVICE_BODY_COMPOSITION = UUID.fromString("0000181b-0000-1000-8000-00805f9b34fb");

    private Button connectBTDevice;
    private Button disconnectDevice;
    private BluetoothGatt bluetoothGatt;

    private ArrayAdapter<String> listAdapter;
    private BTBondStateChangeReceiver bondStateChangeReceiver;
    private final String LOG_TAG = BTConnectActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_connect);

        ListView listView = findViewById(R.id.devices);
        TextView deviceName = findViewById(R.id.selectedDevice);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);

        Intent intent = getIntent();
        selectedDevice = intent.getParcelableExtra(DeviceScanActivity.SELECTED_DEVICE_KEY);
        deviceName.setText("Selected Device : " + selectedDevice.getDeviceName());

        connectBTDevice = findViewById(R.id.connect);
        connectBTDevice.setOnClickListener(v -> connectBTDevice());

        disconnectDevice = findViewById(R.id.disconnect);
        disconnectDevice.setOnClickListener(v -> disconnect());
        disconnectDevice.setVisibility(View.GONE);
    }

    private void connectBTDevice() {
        connectBTDevice.setVisibility(View.GONE);
        disconnectDevice.setVisibility(View.VISIBLE);
        //registering to listen to any bond state change event
        registerForBondStateChange(getApplicationContext());
        //connecting to the device
        bluetoothGatt = selectedDevice.getBtDevice().connectGatt(getApplicationContext(), true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(LOG_TAG, "Successfully connected to and now trying to discover services");
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(LOG_TAG, "Successfully disconnected");
                        gatt.close();
                    }
                } else {
                    Log.e(LOG_TAG, "Error encountered and disconnecting");
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(LOG_TAG, "Service discovered");
                BluetoothGattService service = gatt.getService(SERVICE_BODY_COMPOSITION);
                if (service != null) {
                    setIndicationForCharacteristic(service, gatt, SCALE_DATA);
                } else {
                    Log.e(LOG_TAG, "Service not available, unknown device");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.d(LOG_TAG, "onCharacteristicChanged() : characteristic UUID " + characteristic.getUuid());
                if (SCALE_DATA.equals(characteristic.getUuid())) {
                    byte[] data = characteristic.getValue();
                    //As the payload data expected from weight scale is 13 bytes, we check the byte array length accordingly
                    if (data != null && data.length==13) {
                        Log.d(LOG_TAG, "Byte array data obtained with length " + data.length);
                        parseBytesFromWeightScale(data);
                    } else {
                        Log.e(LOG_TAG, "Invalid Data");
                    }
                } else {
                    Log.e(LOG_TAG, "Unknown characteristic");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                Log.d(LOG_TAG, "On characteristic read called");
            }

        });
    }

    /**
     * Register for bond state change events
     *
     * @param context context for registering BroadcastReceiver
     */
    public void registerForBondStateChange(Context context) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bondStateChangeReceiver = new BTBondStateChangeReceiver();
        context.registerReceiver(bondStateChangeReceiver, filter);
    }


    /**
     * Broadcast receiver listening to bluetooth bond state change actions
     */
    private class BTBondStateChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Bundle bundle = intent.getExtras();
                if (action != null && action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    Log.d(LOG_TAG, "ACTION_BOND_STATE_CHANGED");
                    onPairingStateChanged(Objects.requireNonNull(bundle), intent);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception in onReceive of BTBondStateChangeReceiver " + e);
            }
        }
    }

    /**
     * Handling of pairing/bonding state changes action
     * Note - For now, we have listened to the events, appropriate actions may be added under various bond actions as needed
     * BOND_NONE --> BOND_BONDING: Pairing started
     * BOND_BONDING --> BOND_NONE : Pairing error, as pairing was not completed to BOND_BONDED status
     * BOND_BONDING --> BOND_BONDED : Pairing completed
     *
     * @param bundle broadcast bundle
     **/
    private void onPairingStateChanged(Bundle bundle, Intent intent) {
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        int newBondState = bundle.getInt(BluetoothDevice.EXTRA_BOND_STATE);
        int previousBondState = bundle.getInt(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE);
        Log.d(LOG_TAG, "Previous bond state: " + previousBondState + " ---> New bond state: " + newBondState);
        String deviceInfoLog = bluetoothDevice.getName() + "| MACId: " + bluetoothDevice.getAddress();
        switch (newBondState) {
            case BluetoothDevice.BOND_NONE:
                /* Possible pairing failed case as Pairing state changed from BOND_BONDING to BOND_NONE */
                if (previousBondState == BluetoothDevice.BOND_BONDING) {
                    Log.e(LOG_TAG, "********** Possible pairing failed as pairing state changed from BOND_BONDING to BOND_NONE **********");
                    //@TODO Appropriate error handling can be added as per application requirement
                }
                break;

            case BluetoothDevice.BOND_BONDING:
                /* Pairing started for device, start a timeout handler */
                Log.d(LOG_TAG, "********** Pairing Initiated with Device :" + deviceInfoLog + " **********");
                break;

            case BluetoothDevice.BOND_BONDED:
                Log.d(LOG_TAG, "********** Device paired successfully :" + deviceInfoLog + " **********");
                //@TODO Appropriate success flow can be added as per application requirement
                break;
            default:
                break;
        }
    }

    /**
     * Method to set characteristics for service.
     */
    private void setCharacteristicForService(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt, UUID uuid, byte[] value) {
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    CHAR_CLIENT_CONFIG);
            if (descriptor != null) {
                descriptor.setValue(value);
                gatt.writeDescriptor(descriptor);
            } else {
                Log.d(LOG_TAG, "descriptor is null: " + uuid.toString());
            }
        } else {
            Log.d(LOG_TAG, String.format("unable to find characteristic with uuid: %s", uuid.toString()));
        }
    }

    /**
     * Sets indication for characteristic.
     *
     * @param bluetoothGattService the bluetooth gatt service
     * @param gatt                 the gatt
     * @param uuid                 the uuid
     */
    public void setIndicationForCharacteristic(BluetoothGattService bluetoothGattService, BluetoothGatt gatt, UUID uuid) {
        BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(uuid);
        Log.d(LOG_TAG, String.format("setIndicationForCharacteristic() Characteristic: %s Service: %s", uuid.toString(), bluetoothGattService.getUuid().toString()));
        setCharacteristicForService(characteristic, gatt, uuid, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
    }

    protected boolean isBitSet(byte value, int bit) {
        return (value & (1 << bit)) != 0;
    }

    /**
     * Utility method to parse bytes from the byte array supplied. In this particular example,
     * we extract the information received for the body weight scale BT device and parse the weight, date, and time information
     * from the bytes received from the device.
     *
     * @param data
     */
    private void parseBytesFromWeightScale(byte[] data) {
        try {
            final byte ctrlByte0 = data[0];
            final byte ctrlByte1 = data[1];

            final boolean isWeightRemoved = isBitSet(ctrlByte1, 7);
            final boolean isDateInvalid = isBitSet(ctrlByte1, 6);
            final boolean isStabilized = isBitSet(ctrlByte1, 5);
            final boolean isLBSUnit = isBitSet(ctrlByte0, 0);
            final boolean isCattyUnit = isBitSet(ctrlByte1, 6);
            final boolean isImpedance = isBitSet(ctrlByte1, 1);

            if (isStabilized && !isWeightRemoved && !isDateInvalid) {

                final int year = ((data[3] & 0xFF) << 8) | (data[2] & 0xFF);
                final int month = data[4];
                final int day = data[5];
                final int hours = data[6];
                final int min = data[7];
                final int sec = data[8];

                float weight;
                float impedance = 0.0f;

                if (isLBSUnit || isCattyUnit) {
                    weight = (float) (((data[12] & 0xFF) << 8) | (data[11] & 0xFF)) / 100.0f;
                } else {
                    weight = (float) (((data[12] & 0xFF) << 8) | (data[11] & 0xFF)) / 200.0f;
                }

                if (isImpedance) {
                    impedance = ((data[10] & 0xFF) << 8) | (data[9] & 0xFF);
                    Log.d(LOG_TAG, "Impedance is " + impedance);
                }

                Log.d(LOG_TAG, "Weight is : " + weight);
                runOnUiThread(() -> {
                    String displayData = "Date- " + day + "/" + month + "/" + year + " Time- " + hours + ":" + min + " (HH:mm)" + " Weight- " +
                            weight + (isLBSUnit ? " Pounds" : " Kgs");
                    listAdapter.add(displayData);
                    listAdapter.notifyDataSetChanged();
                });
            }
        } catch (ParseException e) {
            Log.e(LOG_TAG, "PARSING EXCEPTION " + e);
        }
    }

    /**
     * Disconnect the established connection
     */
    private void disconnect() {
        connectBTDevice.setVisibility(View.VISIBLE);
        disconnectDevice.setVisibility(View.GONE);
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
    }

}