package com.example.lapatopa.firetemp;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final int REQUEST_ENABLE_BT = 1;
    private Button temp;
    private Button bt;
    private Button lights;
    private TextView text;

    private FTDriver mSerial;
    private Spinner spin;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };






    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    Handler bHandler = new Handler();
    Handler mHandler = new Handler();

    // [FTDriver] Permission String
    private static final String ACTION_USB_PERMISSION =
            "jp.ksksue.tutorial.USB_PERMISSION";

    private byte[] bytes = new byte[4096];
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;
    //private UsbManager mUsbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set buttons
        bt = (Button) findViewById(R.id.BTButton);
        bt.setOnClickListener(this);
        temp = (Button) findViewById(R.id.TempButton);
        temp.setOnClickListener(this);
        temp.setEnabled(false);
        lights = (Button) findViewById(R.id.lightsButton);
        lights.setOnClickListener(this);

        //set bluetooth selector
        spin = (Spinner) findViewById(R.id.spinner);
        //spin.setOnItemClickListener();
        spin.setEnabled(false);

        // set the text output (for the temps)
        text = (TextView) findViewById(R.id.TextView2);
        text.setLines(15);
        //text.setText("No Button Pressed");

        // Obtain the bluetooth manager
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // Initializes Bluetooth adapter.
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // set the FTD comms with the thermometer
        // [FTDriver] Create Instance
        mSerial = new FTDriver((UsbManager)getSystemService(Context.USB_SERVICE));
        // [FTDriver] setPermissionIntent() before begin()
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        mSerial.setPermissionIntent(permissionIntent);
        mSerial.begin(FTDriver.BAUD9600);
        //UsbSerialDriver driver = UsbSerialProber.acquire(manager);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // [FTDriver] Close USB Serial
        mSerial.end();
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


    @Override
    public void onClick(View v) {
    if (v.getId() == R.id.lightsButton){
        //text.append("This LightsButton be working yal\n");
        if(mSerial.begin(FTDriver.BAUD9600)) {
            temp.setEnabled(true);
            text.append("Temp Sensor connected\n");
        }
        else {
            text.append("Temp Sensor cannot connect\n");
        }

    }
    if (v.getId() == R.id.TempButton){
        new Thread(mLoop).start();

        }
    if (v.getId() == R.id.BTButton){
        text.setText("This BTButton be working yal\n");
        //TODO this button must be used to discover some bluetooth devices
        // this is for the spinner action stuff
        // Create an ArrayAdapter using the string array and a default spinner layout
        List<String> list = new ArrayList<String>();
        list.add("list 1");
        list.add("list 2");
        list.add("list 3");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        // Specify the layout to use when the list of choices appears
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spin.setAdapter(dataAdapter);
        spin.setEnabled(true);
        //scanLeDevice(true);
        }
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            for (;;) {// this is the main loop for transferring

                // ////////////////////////////////////////////////////////
                // Read and Display to Terminal
                // ////////////////////////////////////////////////////////
                    mHandler.post(new Runnable() {
                        int len;
                        byte[] rbuf = new byte[4096];
                        public void run() {
                            len = mSerial.read(rbuf);
                            rbuf[len] = 0;
                            StringBuilder mText = new StringBuilder();
                            for(int i=0; i<len; i++) {
                                mText.append((char) rbuf[i]);
                            }
                            text.setText(mText + "\n");
                        }
                    });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @SuppressLint("NewApi")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            bHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.cancelDiscovery();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            //mBluetoothAdapter.getBluetoothLeScanner();
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
