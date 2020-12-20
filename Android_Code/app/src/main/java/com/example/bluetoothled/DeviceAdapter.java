package com.example.bluetoothled;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceAdapter extends
        RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    // member fields
    private ArrayList<BluetoothDevice> mDeviceList;

    /**
     * constructor for DeviceAdapter class
     * this is called in mainactivity to create the reycyclerview
     * this is the connection between the data and the view items (UI)
     */
    public DeviceAdapter(ArrayList<BluetoothDevice> devices){
        mDeviceList = devices;
    }

    /**
     * inflates (loads into memory) a view and returns a viewholder that contains the view
     * a viewholder describes a view item and meta data about its place within the recyclerview
     * this viewholders layout is described in the device_cardview.xml file
     * @param parent
     * @param viewType
     * @return DeviceViewHolder
     */
    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /**
         * inflate device cardview from xml layout to populate the holder
         * this is the container for each view (UI widget) in the recyclerview
         */
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_cardview, parent, false);
        DeviceViewHolder dvh = new DeviceViewHolder(view);
        return dvh;
    }

    /**
     * sets the content of a view item at a given position
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        // get the current item
        final BluetoothDevice current = mDeviceList.get(position);

        // set textviews
        holder.mNameTextView.setText(current.getName());
        holder.mAddressTextView.setText(current.getAddress());

        // set tag for cardview
        holder.mCardView.setTag(current);
    }

    /**
     * gets the number of data items
     * @return item count
     */
    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }

    /**
     * class or object to store and recycle views as they are scrolled off screen
     * this is what is created by the OnCreateViewHolder method
     *
     */
    public static class DeviceViewHolder extends RecyclerView.ViewHolder{
        // member fields
        private TextView mNameTextView;
        private TextView mAddressTextView;
        private CardView mCardView;

        // constructor for DeviceViewHolder class
        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);

            // create java objects from xml layouts
            mNameTextView = itemView.findViewById(R.id.deviceName);
            mAddressTextView = itemView.findViewById(R.id.deviceAddress);
            // inflate the layout associated with the adapter
            mCardView = itemView.findViewById(R.id.device_row);

            // handle click
            mCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // get the clicked bluetooth device
                    BluetoothDevice current = (BluetoothDevice) mCardView.getTag();

                    // check the bond state of the device and create bond (pair) the device isn't
                    if (current.getBondState() == BluetoothDevice.BOND_NONE){
                        current.createBond();
                    }
                    // intent for device activity
                    Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                    intent.putExtra("device", current);

                    // why do i have to do this?
                    view.getContext().startActivity(intent);
                }
            });

        }
    }

}
