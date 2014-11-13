package org.grassel.bleweatherstation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Arrays;


public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {

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

        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

       // mHandler.postDelayed(mStopRunnable, 10 * 1000); // scan for 20 sec
        Log.i(TAG, "scan has started");
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
        Log.i(TAG, "scan has stopped");
    }

    /* BluetoothAdapter.LeScanCallback */

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);

        String scanRecordHex = "";
        for (int i=0; i<scanRecord.length; i++) {
            scanRecordHex += String.format("%Xo ", scanRecord[i]);
        }
        Log.i(TAG, "scanrecord=" + scanRecordHex);
        parseScaneResult(scanRecord);
     }

    private static int UPDATE_TEXT_FIELDS_WITH_SENSOR_VALUES = 4711;

    private void parseScaneResult(byte[] scanRecord) {
        WeatherStationAdvertisementReader reader = new WeatherStationAdvertisementReader(scanRecord);
        this.temperatureSensorValue = reader.readTemp();
        this.preasureSensorValue = reader.readPreasure();
        this.humiditySensorValue = reader.readHumidity();

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


    // return the payload, verify the first three bytes identify our service, then
    // chop them off, return the rest.
    private byte[] parseServiceDataFromBytes(byte[] scanRecord) {
        int currentPos = 0;
        try {
            while (currentPos < scanRecord.length) {
                int fieldLength = scanRecord[currentPos++] & 0xff;
                if (fieldLength == 0) {
                    break;
                }
                int fieldType = scanRecord[currentPos++] & 0xff;
                if (fieldType == DATA_TYPE_SERVICE_DATA) {
                    // The first two bytes of the service data are service data UUID.
                    if (scanRecord[currentPos++] == WEATHER_SERVICE_16_BIT_UUID_BYTES[0]
                            && scanRecord[currentPos++] == WEATHER_SERVICE_16_BIT_UUID_BYTES[1]) {
                        // length includes the length of the field type and ID
                        byte[] bytes = new byte[fieldLength - 3];
                        System.arraycopy(scanRecord, currentPos, bytes, 0, fieldLength - 3);
                        return bytes;
                    }
                }
                // length includes the length of the field type
                currentPos += fieldLength - 1;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
        }
        return null;
    }

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

    private static final String TAG = "BLEWeatherStation.MainActivity";
    private static final int DATA_TYPE_SERVICE_DATA = 0x16;
    private static final byte[] WEATHER_SERVICE_16_BIT_UUID_BYTES = {(byte) 0xd8, (byte) 0xff}; //

}
