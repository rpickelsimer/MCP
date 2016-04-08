package edu.rasm.pickel.mcp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**  add a handler that toasts on connection success
 * handler here, set handler, put a handler in connect thread that ssuccess connect
 * basically, do it like the bluetooth aussie***/


// device only pairs if running beofre app opened. need to do more discovery. Also, does not handle
    //config change first activity
public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener{

    ArrayAdapter<String> listAdapter;
    ListView listView;

    // to discover devices, broadcast receiver
    IntentFilter filter;
    BroadcastReceiver receiver;

    //Mine
    Button btnNext;
    TextView tvDeviceLabel, tvDeviceSelected;

    BTManager manager;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BTManager.SUCCESS_CONNECT:
                    btnNext.setEnabled(true);
                    tvDeviceLabel.setText(R.string.connected_device);
                    tvDeviceSelected.setText(manager.getDeviceName());
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // global BlueTooth manager object
        manager = (BTManager)getApplicationContext();
        manager.setmHandler(mHandler);
        manager.setBtAdapter();

        // UI elements
        btnNext = (Button)findViewById(R.id.button);
        tvDeviceLabel = (TextView)findViewById(R.id.tvDeviceLabel);
        tvDeviceSelected = (TextView)findViewById(R.id.tvDeviceSelected);

        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
        listView.setAdapter((listAdapter));

        // broadcast receiver
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add to list of found devices
                    manager.addDevice(device);
                    String s = "";
                    //check against paired devices
                    for(int a = 0; a < manager.getPairedDevices().size(); a++) {
                        if(device.getName().equals(manager.getPairedDevices().get(a))) {
                            // append string
                            s = "(Paired)";
                            break;
                        }
                    }

                    // Add the name and address to an array adapter to show in a ListView
                    listAdapter.add(device.getName() + " " + s + " " + "\n" + device.getAddress());
                }
                else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    //if Bluetooth is turned off
                    if (manager.getBtAdapter().getState() == manager.getBtAdapter().STATE_OFF) {
                        Intent i = manager.getEnabledAdapterIntent();
                        startActivityForResult(i, 1);  //check that it returns true (enabled)
                    }
                }
                else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        Toast.makeText(getApplicationContext(), "Device is paired.", Toast.LENGTH_SHORT).show();
                        manager.setPairedDevices();
                        manager.startDiscovery();
                    } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                        Toast.makeText(getApplicationContext(), "Device is unpaired.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);

        // check if adapter is null
        if (manager.getBtAdapter() == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth detected.", Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            if (!manager.isAdapterEnabled()) {
                Intent intent = new Intent(manager.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 1);  //check that it returns true (enabled)
            }
        }
        manager.setPairedDevices();
        manager.startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();

        //unregisterReceiver(receiver);

    }

    @Override
    public void onResume() {
        super.onResume();


        // this is pretty weak - I'm trying to make sure it wasn't paused before a connection got going
        //if (manager.getmConnectThread() == null) {
         //   Intent intent = new Intent(manager.ACTION_REQUEST_ENABLE);
         //   startActivityForResult(intent, 1);  //check that it returns true (enabled)
        //}

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


        if(manager.getBtAdapter().isDiscovering()) {
            manager.getBtAdapter().cancelDiscovery();
        }
        if(listAdapter.getItem(i).contains("Paired")) {
            tvDeviceSelected.setText("connecting...");
            manager.setMmDevice(manager.getDevices().get(i));
            manager.startConnection();
        }
        else {
            //Toast.makeText(getApplicationContext(), "Device is not paired.", Toast.LENGTH_SHORT).show();
            manager.pairDevice(manager.getDevices().get(i));
        }
    }

    /** onClick for continue as paired or unpaired device */
    public void onNextActivityClick(View v) {

        Intent intent = new Intent(this, ControlsActivity.class);
        startActivityForResult(intent, 2);
    }

    /** onClick() for unpairing a device */
    public void onUnpairClick(View v) {

        manager.unpairDevice(manager.getMmDevice());
    }

    /** onClick() for unpairing a device */
    public void onStartDiscoveryClick(View v) {

        manager.setPairedDevices();
        manager.startDiscovery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
