package com.example.btchallengeapp;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Encapsulated the bluetooth device and some other parameters into a custom device class which can
 * be extended further to have some more properties if needed
 */
public class Device implements Parcelable {

    private String deviceName;
    private String deviceMacAddress;
    private BluetoothDevice btDevice;

    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    public void setBtDevice(BluetoothDevice btDevice) {
        this.btDevice = btDevice;
    }

    public static Creator<Device> getCREATOR() {
        return CREATOR;
    }

    protected Device(Parcel in) {
        deviceName = in.readString();
        deviceMacAddress = in.readString();
        btDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    public Device(){}

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceMacAddress() {
        return deviceMacAddress;
    }

    public void setDeviceMacAddress(String deviceMacAddress) {
        this.deviceMacAddress = deviceMacAddress;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(deviceName);
        parcel.writeString(deviceMacAddress);
        parcel.writeParcelable(btDevice, 1);

    }
}
