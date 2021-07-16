# MIScale_BT_Integration

Brief about the application : We have developed an Android application which connects to a Xiaomi MI Body composition Scale (BLE peripheral) and shows the weight/date/time parameters on the Android phone. 

Execution steps:
1. Application asks for Bluetooth and location permissions
2. After permission grant, user can scan the BLE devices
3. User sees the list of scanned devices and user can choose a particular device to connect to (in this case Xiaomi Body Composition Scale)
4. Once connection is made, the “weight, date and time” parameters are requested to be notified to android app and we show the details on the app as received. For this device, explicit pairing was not needed although in our code we have made provision to listen to bonding events
5. We can disconnect the connection once the work is done
