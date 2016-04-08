package edu.rasm.pickel.mcp;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by ryan on 5/18/15.
 */


public class BTManager extends Application {

    public static final int SUCCESS_CONNECT = 0;
    public static final int MESSAGE_READ = 1;
    public static final String ACTION_REQUEST_ENABLE = BluetoothAdapter.ACTION_REQUEST_ENABLE;

    //start of our application we want to check bluetooth is available and then is enabled
    //check phone for Bluetooth
    BluetoothAdapter btAdapter;
    ArrayList<String> pairedDevices = new ArrayList<String>();
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
    //BluetoothDevice selectedDevice;

    String txString, rxString;
    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    Handler mHandler;

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public void write(String s) {
        //byte[] msgBuffer = s.getBytes();
        mConnectedThread.write(s.getBytes());
    }

    public boolean isAdapterEnabled() {
        return btAdapter.isEnabled();
    }

    public synchronized void startConnection() {
        mConnectThread = new ConnectThread(mmDevice);
        mConnectThread.start();
    }

    public void startConnection(BluetoothDevice device) {
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    public void startDataTransfer() {
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }
    public Intent getEnabledAdapterIntent() {

        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

    }

    public void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDiscovery() {
        // cancel discovery if already ongoing
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    public void setPairedDevices() {
        // get detected devices
        Set<BluetoothDevice> deviceArray = btAdapter.getBondedDevices();
        // check to make sure it doesn't return null
        if (deviceArray.size() > 0) {
            //loop through and add devices
            for(BluetoothDevice device:deviceArray) {
                //listAdapter.add(device.getName() + "\n" + device.getAddress()); //after tut 5
                pairedDevices.add(device.getName());
            }
        }
    }

    public void addDevice(BluetoothDevice device) {
        devices.add(device);
    }

    private class ConnectThread extends Thread {

        //private BluetoothSocket mmSocket;
        //private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;


            if(Build.VERSION.SDK_INT >= 10){
                try {
                    final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                    tmp = (BluetoothSocket) m.invoke(mmDevice, MY_UUID);
                } catch (Exception e) {
                    Log.e("MyDebug", "Could not create Insecure RFComm Connection", e);
                }
            }
            else {


                // Get a BluetoothSocket to connect with the given BluetoothDevice
                try {
                    // MY_UUID is the app's UUID string, also used by the server code
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                }

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();

            //Mine
        startDataTransfer();
        }

        //private void manageConnectedSocket(BluetoothSocket mmSocket2) {
        //}


        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    buffer = new byte[1024];// clears buffer instead of appending every time.
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                    //mHandler.obtainMessage(MESSAGE_READ, bytes).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void setBtAdapter() {
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothAdapter getBtAdapter() {
        return btAdapter;
    }

    public ArrayList<BluetoothDevice> getDevices() {
        return devices;
    }

    public String getTxString() {
        return txString;
    }

    public String getRxString() {
        return rxString;
    }

    public ConnectedThread getmConnectedThread() {
        return mConnectedThread;
    }

    public ConnectThread getmConnectThread() {
        return mConnectThread;
    }

    public ArrayList<String> getPairedDevices() {
        return pairedDevices;
    }

    public void setMmDevice(BluetoothDevice selectedDevice) {
        mmDevice = selectedDevice;
    }

    public BluetoothDevice getMmDevice() { return mmDevice; }

    public String getDeviceName() {
        return mmDevice.getName();
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }
}
