# Sensor to OSC mobile app (iOS and Android)

Android app to stream data from various Android and Polar BLE sensors to OSC

Mostly tested with Polar H10 (ECG and Accelerometer streams)

Based on [androidBleSdkTestApp](https://github.com/polarofficial/polar-ble-sdk/tree/master/examples/example-android/androidBleSdkTestApp) from [polar-ble-sdk](https://github.com/polarofficial/polar-ble-sdk)

## Available OSC streams

These streams are implemented in the app (not all might be supported by your device):

`/deviceId/hr` Heart rate stream

`/deviceId/ecg` ECG stream

`/deviceId/acc` Accelerometer stream

`/deviceId/gyro` Gyroscope stream

`/deviceId/mag` Magnetometer stream

`/deviceId/ppg` PPG stream

`/deviceId/ppi` PPI stream

The "/deviceId" prefix will be automatically set and can be changed manually.

## Screenshots

![Screenshot](/screenshot1.png)
