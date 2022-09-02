# polarAndroidOscApp
Android app to stream data from Polar BLE devices to OSC

Based on [androidBleSdkTestApp](https://github.com/polarofficial/polar-ble-sdk/tree/master/examples/example-android/androidBleSdkTestApp) from [polar-ble-sdk](https://github.com/polarofficial/polar-ble-sdk)

## Available OSC streams

Theese streams are implemented in the app (not all might be supported by your device):

`/polar/ecg` ECG stream

`/polar/acc` Accelerometer stream

`/polar/gyro` Gyroscope stream

`/polar/mag` Magnetometer stream

`/polar/ppg` PPG stream

`/polar/ppi` PPI stream

Tested with Polar H10 (only supports ECG and Accelerometer streams)
