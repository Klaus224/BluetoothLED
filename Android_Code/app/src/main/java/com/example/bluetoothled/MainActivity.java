package com.example.bluetoothled;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // logging
    private static String TAG = "BT_App";

    // member fields
    private Button mLoadButton;
    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mDevice;
    private ArrayList<BluetoothDevice> mDeviceList;
    private RecyclerView mRecyclerview;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    // Return intent extras
    private static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create Java objects from xml layouts
        mLoadButton = findViewById(R.id.load_devices);

        /**
         * wire up recyclerview
         */
        // create java object from xml layout
        mRecyclerview = findViewById(R.id.recyclerView_devices);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerview.setLayoutManager(mLayoutManager);

        /**
         *  register broadcast receivers
         */
        registerBroadcastReceivers();

        /**
         *  get bluetooth adapter
         */
        mBTAdapter = null;
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        /**
            enable bluetooth
         */
        enableBT();

        /**
            on click of load button
         */
        mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get devices
                mDeviceList = getDevices();

                // finish wiring up recyclerview
                mAdapter = new DeviceAdapter(mDeviceList);
                mRecyclerview.setAdapter(mAdapter);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(enableBTReceiver);
        unregisterReceiver(mDiscoverDeviceReceiver);
    }

    /**
     *  method to enable bluetooth
     */
    private void enableBT(){

        // does device have a bluetooth adapter
        if (mBTAdapter == null){
            showToast("Device does not support Bluetooth");
        }
        // bluetooth is not enabled
        if (!mBTAdapter.isEnabled()){
            // prompt user to enable bluetooth
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // user accepted request to turn on bluetooth
        if (requestCode == Activity.RESULT_OK){
            showToast("Bluetooth enabled");
        }
        else{
            showToast("Bluetooth must be enabled");
        }
    }

    /**
     *  method to register all broadcast receivers
     */
    private void registerBroadcastReceivers(){

        /**
            register for broadcasts when bluetooth is turned on and off
         */
        IntentFilter enableBTIntentFilter = new IntentFilter();
        enableBTIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(enableBTReceiver, enableBTIntentFilter);

        /**
            register for broadcasts when a device is discovered
            - executed by getDevices method
         */
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mDiscoverDeviceReceiver, discoverDevicesIntent);


    }

    /**
     *  broadcast receiver for bluetooth adapter state
     */
    private final BroadcastReceiver enableBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // get the state
            int bluetoothStateExtra = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            // handle changes to bluetooth state
            switch (bluetoothStateExtra){
                // bluetooth is on
                case BluetoothAdapter.STATE_ON:
                    showToast("Bluetooth enabled");
                // bluetooth is off
                case BluetoothAdapter.STATE_OFF:
                    showToast("Bluetooth disabled");

                    // prompt user to turn bluetooth back on
                    Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);

            }
        }
    };

    /**
     * broadcast receiver for discovering devices
     */
    private final BroadcastReceiver mDiscoverDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // get action from intent
            String action = intent.getAction();
            // discovery has found a device.
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                // get the bluetooth device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add device to device list
                mDeviceList.add(device);
                // update the recyclerview adapter
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * method to get paired and unpaired devices
     */
    private ArrayList<BluetoothDevice> getDevices(){
        ArrayList<BluetoothDevice> mDevicelist = new ArrayList<>();
        // get paired devices and store in devicelist
        mDevicelist.addAll(mBTAdapter.getBondedDevices());

        // cancel and then start discovery
        mBTAdapter.cancelDiscovery();
        mBTAdapter.startDiscovery(); // get devices during mDeviceBroadcastReceiver
        return mDevicelist;
    }

    /**
     * method for showing toast messages
     */
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}