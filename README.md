# Sensor to OSC mobile app for iOS and Android

Android app to stream data from various Android and Polar BLE sensors to OSC

Tested with Polar H10 (ECG and Accelerometer streams) and Verity Sense.

Based on the [polar-ble-sdk](https://github.com/polarofficial/polar-ble-sdk)

## Available OSC streams

These streams are implemented in the app (not all might be supported by your device):

`/oscPrefix/hr` Heart rate stream

`/oscPrefix/ecg` ECG stream

`/oscPrefix/acc` Accelerometer stream

`/oscPrefix/gyro` Gyroscope stream

`/oscPrefix/mag` Magnetometer stream

`/oscPrefix/ppg` PPG stream

`/oscPrefix/ppi` PPI stream

The "/oscPrefix" prefix will be automatically set and can be changed manually.

## Screenshots

![Screenshot](/screenshot1.png)
