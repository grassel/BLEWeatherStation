package com.nokia.bleweatherstation;

/**
 * Created by guido on 13/11/14.
 */
public class AdvertisementReader {

    AdvertisementReader(byte[] beaconPayload) {
        this.beaconPayload = beaconPayload;
    }

    public int readUnsignedInteger(int offset) {
        return ((offset>=0) && (offset < this.beaconPayload.length)) ?
                ((256 * (this.beaconPayload[offset] & 0xFF)) + (this.beaconPayload[offset+1] & 0xFF)) : -1;
    }

    float unsignedIntegerToFloat(int value, float offset, float presition) {
       return (value / presition) + offset;
    }

    protected byte[] beaconPayload;
}
