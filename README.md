# Sensor to OSC Android app

Android app to stream data from various Android and Polar BLE sensors to OSC

Mostly tested with Polar H10 (ECG and Accelerometer streams)

Based on [androidBleSdkTestApp](https://github.com/polarofficial/polar-ble-sdk/tree/master/examples/example-android/androidBleSdkTestApp) from [polar-ble-sdk](https://github.com/polarofficial/polar-ble-sdk)

## Available OSC streams

These streams are implemented in the app (not all might be supported by your device):

`/sensor/hr` Heart rate stream

`/sensor/ecg` ECG stream

`/sensor/acc` Accelerometer stream

`/sensor/gyro` Gyroscope stream

`/sensor/mag` Magnetometer stream

`/sensor/ppg` PPG stream

`/sensor/ppi` PPI stream

The "/sensor" prefix can be changed.

## Screenshots

![Screenshot](/screenshot1.png)
