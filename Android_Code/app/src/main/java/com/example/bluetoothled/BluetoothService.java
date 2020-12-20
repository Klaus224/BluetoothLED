package com.example.bluetoothled;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {

    /** UUID to uniquely identify the BluetoothService */
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Member fields
    public BluetoothDevice mDevice;
    public ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int mState;
    private Handler mHandler;

    // constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_CONNECTING = 1; // connecting to device
    public static final int STATE_CONNECTED = 2; // connected to device
    public static final int STATE_FAILED = 3; // connection to device failed
    public static final int STATE_LOST = 4; // connection to device lost

    // LOGGING
    private static final String TAG = "BluetoothService";

    // Constructor
    public BluetoothService(BluetoothDevice device, android.os.Handler handler){
        // assignments
        mDevice = device;
        mHandler = handler;

        // set initial connection state
        mState = STATE_NONE;
    }

    /** method to update the user interface**/
    private synchronized void updateUserInterface(){
        // get current connection state
        mState = getstate();
        // send data to ui thread
        mHandler.obtainMessage(mState).sendToTarget();
    }

    /** method to get the connection state **/
    public synchronized int getstate(){
        return mState;
    }

    /** method to start connection*/
    public synchronized void connect(){
        // cancel any threads attempting to connect
        if (mState == STATE_CONNECTING){
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // cancel any thread running a connection
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        // create and start thread to connect with given device
        connectThread = new ConnectThread(mDevice);
        connectThread.start();

        // update ui
        updateUserInterface();
    }

    /** method to manage connection */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){
        // cancel thread that opened connection
        if (connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        // cancel any thread running a connection
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        // Create and start thread to manage connection
        connectedThread = new ConnectedThread(socket, device);
        connectedThread.start();

        // update ui
        updateUserInterface();
    }

    /** method to write data to output stream*/
    public void write(byte[] out){
        // write out if connected
        if (mState == STATE_CONNECTED){
            connectedThread.write(out);
        }
        else{
            connectionLost();
        }

    }

    /** method to stop all threads **/
    public synchronized void stop() {

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    /** method to handle when connection is lost **/
    private void connectionLost(){
        // update ui
        updateUserInterface();

        mState = STATE_NONE;

    }

    /** Thread where bluetooth socket is created and connected to the provided device */
    private class ConnectThread extends Thread{
        // member fields
        private final BluetoothDevice mmdevice;
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device){
            // use temporary variable that will later be assigned to mmsocket
            // because mmSocket is final which means that it always points to the same object
            BluetoothSocket tmp = null;
            mmdevice = device;

            // LOGGING
            Log.d(TAG, "create ConnectThread for " + mDevice.getName());

            // Create BT socket to connect with the given device
            try {
                /**
                 * initializes a BT socket that allows the client to connect to a bluetooth device
                 * this will block until it succeeds or throws an exception
                 */
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Failed to create RFcomm socket");
            }
            // assign mmSocket to the socket just created
            mmSocket = tmp;
            // update connection state
            mState = STATE_CONNECTING;
        }

        /** connect the device. will block until it succeeds or throws and exception */
        public void run(){
            // LOGGING
            Log.d(TAG, "begin ConnectThread for " + mmdevice.getName());

            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Unable to connect");
                // update connection state
                mState = STATE_FAILED;
                updateUserInterface();
                // connection failed, close socket
                try {
                    mmSocket.close();

                } catch (IOException ex) {
                    Log.e(TAG, "Failed to close socket");
                }
                return;
            }

            // reset connect thread
            synchronized (BluetoothService.this){
                connectThread = null;
            }

            // connection has succeeded, start to manage the connection
            connected(mmSocket, mmdevice);
        }

        /** cancel connections in-progress and close socket */
        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket");
            }
        }
    }

    /** Thread where the connection is managed */
    private class ConnectedThread extends Thread{
        // member fields
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device){
            mmSocket = socket;
            mDevice = device;

            // LOGGING
            Log.d(TAG, "create ConnectedThread for:" + mDevice.getName());

            // initialize to null because they are final
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // get the input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Failed to open temp sockets");
            }

            // temp sockets successfully opened
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            // update connection state
            mState = STATE_CONNECTED;
        }

        public void run(){
            // LOGGING
            Log.d(TAG, "begin ConnectedThread");

            // buffer for reading data
            byte[] buffer = new byte[1024];
            int bytes;

            // listen to the input stream
            while (mState == STATE_CONNECTED){
                try {
                    bytes = mmInStream.read(buffer);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);

                    // update connection state
                    mState = STATE_LOST;
                    connectionLost();
                }
            }

        }

        /** method for sending data */
        public void write(byte [] bytes){

            try {
                mmOutStream.write(bytes);
                // LOGGING
                Log.d(TAG, "Write" + bytes.toString() + "to" + mDevice.getName());

            } catch (IOException e) {
                Log.e(TAG, "Failed to write");

                // update connection state
                mState = STATE_LOST;
                updateUserInterface();
            }
        }

        /** Call this from main activity to shutdown connection */
        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket");
            }
        }
    }

}
