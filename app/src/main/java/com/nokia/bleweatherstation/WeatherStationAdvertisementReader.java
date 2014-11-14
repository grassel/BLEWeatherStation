package com.nokia.bleweatherstation;

import android.util.Log;

/**
 * Created by guido on 13/11/14.
 */
public class WeatherStationAdvertisementReader extends AdvertisementReader {

    public WeatherStationAdvertisementReader(byte[] beaconPayload) {
            super(beaconPayload);
    }

    boolean checkServiceUuid() {
        if ((beaconPayload == null) || (beaconPayload.length < serviceUuidOffset+1)) {
            return false;
        }
        Log.i(TAG, String.format("serviceUuid: %Xo, %Xo", beaconPayload[serviceUuidOffset],
                beaconPayload[serviceUuidOffset+1]));
        boolean result = (serviceUuid[0] ==  beaconPayload[serviceUuidOffset]) &&
                (serviceUuid[1] == beaconPayload[serviceUuidOffset+1]);
        return result;
    }

    float readTemp() {
        Log.i(TAG, String.format("readTemp: %Xo, %Xo  -> int: %d", beaconPayload[payloadOffsetTemp],
                beaconPayload[payloadOffsetTemp+1],
                this.readUnsignedInteger(payloadOffsetTemp)));
        return this.unsignedIntegerToFloat(this.readUnsignedInteger(payloadOffsetTemp),
                convertionOffsetTemp, convertionPresitionTemp);
    }

    float readPreasure() {
        Log.i(TAG, String.format("readPreasure: %Xo, %Xo  -> int: %d", beaconPayload[payloadOffsetPreasure],
                beaconPayload[payloadOffsetPreasure + 1],
                this.readUnsignedInteger(payloadOffsetPreasure)));
        return this.unsignedIntegerToFloat(this.readUnsignedInteger(payloadOffsetPreasure),
                convertionOffsetPreasure, convertionPresitionPreasure);
    }

    float readHumidity() {
        Log.i(TAG, String.format("readHumidity: %Xo, %Xo -> int: %d", beaconPayload[payloadOffsetHumidity],
                beaconPayload[payloadOffsetHumidity + 1],
                this.readUnsignedInteger(payloadOffsetHumidity)));
        return this.unsignedIntegerToFloat(this.readUnsignedInteger(payloadOffsetHumidity),
                convertionOffsetHumidity, convertionPresitionHumidity);
    }

    private static int payloadOffset = 6;

    private static int serviceUuidOffset = payloadOffset + 0;
    private static final byte[] serviceUuid = {(byte) 0xd8, (byte) 0xff}; //

    private static int payloadOffsetTemp = payloadOffset + 4;
    private static float   convertionPresitionTemp = 10.0f;
    private static float   convertionOffsetTemp = -100.0f;

    private static int payloadOffsetPreasure = payloadOffset + 6;
    private static  float   convertionPresitionPreasure = 10.0f;
    private static float   convertionOffsetPreasure = 0.0f;

    private static int payloadOffsetHumidity = payloadOffset + 8;
    private static float   convertionPresitionHumidity = 10.0f;
    private static  float   convertionOffsetHumidity = 0.0f;

    private static final String TAG = "WeatherStationAdvertisementReader";

}
