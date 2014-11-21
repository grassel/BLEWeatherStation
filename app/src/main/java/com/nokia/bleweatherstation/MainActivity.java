package com.nokia.bleweatherstation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.grassel.bleweatherstation.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.temperatureTextView = (TextView) findViewById(R.id.tempTextField);
        this.preasureTextView = (TextView) findViewById(R.id.preasureTextView);
        this.humidityTextView = (TextView) findViewById(R.id.humidityTextView);

                /*
         * Bluetooth in Android 4.3 is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        startScan();
    }

    @Override
    protected void onPause() {
        stopScan();
        super.onPause();
    }


    private void startScan() {
        this.bluetoothLeScanner = this.mBluetoothAdapter.getBluetoothLeScanner();
        ScanFilter filter = new ScanFilter.Builder().build();
        List<ScanFilter> filterList = new ArrayList<ScanFilter>(1);
        filterList.add(filter);
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        this.bluetoothLeScanner.startScan(filterList, settings, new ScanCallback() {
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (int i=0; i<results.size(); i++) {
                    ScanResult result = results.get(i);
                    MainActivity.this.onLeScan(result.getDevice(), result.getRssi(), result.getScanRecord());
                }
            }
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                MainActivity.this.onLeScan(result.getDevice(), result.getRssi(), result.getScanRecord());
            }

            @Override
            public void  onScanFailed (int errorCode) {
                Log.e(TAG, "BLE scan FAILED with code " + errorCode);
            }
        });
        setProgressBarIndeterminateVisibility(true);

       // mHandler.postDelayed(mStopRunnable, 10 * 1000); // scan for 20 sec
        Log.i(TAG, "scan has started");
    }



    private void stopScan() {
        if (bluetoothLeScanner == null) {
            Log.w(TAG, "BLE stopScan: bluetoothLeScanner is NULL");
            return;
        }

        this.bluetoothLeScanner.stopScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                MainActivity.this.onLeScan(result.getDevice(), result.getRssi(), result.getScanRecord());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "BLE scan FAILED with code " + errorCode);
            }
        });
        setProgressBarIndeterminateVisibility(false);
        Log.i(TAG, "scan has stopped");
    }

    public void onLeScan(BluetoothDevice device, int rssi, ScanRecord sc) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);

        byte[] scanRecord = sc.getBytes();

        String scanRecordHex = "";
        for (int i=0; i<scanRecord.length; i++) {
            scanRecordHex += String.format("%Xo ", scanRecord[i]);
        }
        Log.i(TAG, "scanrecord=" + scanRecordHex);
        parseScanResult(scanRecord);
     }

    private static int UPDATE_TEXT_FIELDS_WITH_SENSOR_VALUES = 4711;

    private void parseScanResult(byte[] scanRecord) {
        WeatherStationAdvertisementReader reader = new WeatherStationAdvertisementReader(scanRecord);
        if (reader.checkServiceUuid()) {
            this.temperatureSensorValue = reader.readTemp();
            this.preasureSensorValue = reader.readPreasure();
            this.humiditySensorValue = reader.readHumidity();
        }
        // send an update message from this background thread to the UI Thread
        Message updateMessageMessage =
                mHandler.obtainMessage(UPDATE_TEXT_FIELDS_WITH_SENSOR_VALUES, this);
        updateMessageMessage.sendToTarget();
    }


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_TEXT_FIELDS_WITH_SENSOR_VALUES) {
                Log.i(TAG, "Message handler: updating textFields with new sensor readings in UI Thread");
                MainActivity.this.temperatureTextView.setText(String.format("%2.2f", MainActivity.this.temperatureSensorValue));
                MainActivity.this.preasureTextView.setText(String.format("%4.1f", MainActivity.this.preasureSensorValue));
                MainActivity.this.humidityTextView.setText(String.format("%2.2f", MainActivity.this.humiditySensorValue));
            } else {
                Log.i(TAG, "Message handler: ERROR UNHANDLED MESSAGE");
            }
        }
    };




    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

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

        switch (item.getItemId()) {
            case R.id.action_scan:
                startScan();
                return true;

            //noinspection SimplifiableIfStatement
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private TextView temperatureTextView, humidityTextView, preasureTextView;
    private float temperatureSensorValue, preasureSensorValue, humiditySensorValue;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private static final String TAG = "BLEWeatherStation.MainActivity";
    private static final int DATA_TYPE_SERVICE_DATA = 0x16;
    private static final byte [] SERVICE_UUID = {(byte) 0xD8, (byte) 0xFF};
}
