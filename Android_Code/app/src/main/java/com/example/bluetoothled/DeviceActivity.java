package com.example.bluetoothled;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.larswerkman.holocolorpicker.ColorPicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DeviceActivity extends AppCompatActivity {

    // member fields
    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mDevice;
    private TextView mSelectedTextView;
    private Button  mConnectBtn;
    private Button  mOnBtn;
    private Button  mOffBtn;
    private TextView mStatusTextView;
    private ProgressBar mPBar;
    private ColorPicker mPicker;
    public BluetoothService bluetoothService;

    // logging
    private final String TAG = "Device_Activity";

    // message to send
    private final String ON = "1";
    private final String OFF = "0";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_activity);

        // create java objects in memory from xml layouts
        mSelectedTextView = findViewById(R.id.selected_device);
        mConnectBtn = findViewById(R.id.connect_btn);
        mStatusTextView = findViewById(R.id.device_status);
        mOnBtn = findViewById(R.id.on_btn);
        mOffBtn = findViewById(R.id.off_btn);
        mPBar = findViewById(R.id.progress_circular);
        mPicker = findViewById(R.id.picker);

        // get default bluetooth adapter
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        // cancel discovery
        mBTAdapter.cancelDiscovery();

        // get device from main activity
        mDevice = getIntent().getParcelableExtra("device");

        // set device name in textview
        mSelectedTextView.setText(mDevice.getName());

        // set initial device status
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
            // set the status if the device is paired
            mStatusTextView.setText("Paired");
        }
        else {
            mStatusTextView.setText("Not Paired");
        }

        /** Handler to get information from the BluetoothService**/
        @SuppressLint("HandlerLeak")
        Handler mHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.d(TAG, "handleMessage: " + msg.what);
                switch ((int) msg.what) {
                    case BluetoothService.STATE_CONNECTING:
                        mStatusTextView.setText("Connecting...");
                        // make the progress bar visible
                        mPBar.setVisibility(View.VISIBLE);
                        mPicker.setVisibility(View.INVISIBLE);
                        break;
                    case BluetoothService.STATE_CONNECTED:
                        mStatusTextView.setText("Connected");
                        // hide progress bar
                        mPBar.setVisibility(View.INVISIBLE);
                        mPicker.setVisibility(View.VISIBLE);
                        break;
                    case BluetoothService.STATE_FAILED:
                        mStatusTextView.setText("Connection Failed");
                        // hide progress bar
                        mPBar.setVisibility(View.INVISIBLE);
                        mPicker.setVisibility(View.INVISIBLE);
                        break;
                    case BluetoothService.STATE_LOST:
                        mStatusTextView.setText("Connection Lost");
                        mPicker.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        };

        // get bluetooth service
        bluetoothService = new BluetoothService(mDevice, mHandler);

        // onclick of Connect button
        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // initiate connection
                bluetoothService.connect();
            }
        });

        // on click of on button
        mOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // write to output socket to turn on LED
                byte [] send = ON.getBytes();
                bluetoothService.write(send);
            }
        });

        // on click of off button
        mOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // write to output socket to turn off LED
                byte [] send = OFF.getBytes();
                bluetoothService.write(send);
            }
        });

        // listen for changes to colour picker
        mPicker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {

                // get rgb value from color
                short red = (short) Color.red(color);
                short blue = (short) Color.blue(color);
                short green = (short) Color.green(color);

                // convert data to byte arrays
                byte [] send = ByteBuffer.allocate(16)
                        .putChar('>')   // end character
                        .putInt(red).putInt(blue).putInt(green) // rgb values
                        .putChar('<') // start character
                        .array();

                // LOGGING
                Log.d(TAG, "Array: "+ red + blue + green);

                // write color to output socket
                bluetoothService.write(send);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop bluetoothservice
        bluetoothService.stop();
    }


}
