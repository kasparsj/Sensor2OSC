package cc.kaspars.sensor2osc

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PolarSensorsFragment : Fragment() {
    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: Int, param2: String) =
            PolarSensorsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        private const val TAG = "PolarSensorsFragment"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_POLAR_ID = "7E37D222"
    }

    private var polarId = DEFAULT_POLAR_ID

    private val api: PolarBleApi by lazy {
        // Notice PolarBleApi.ALL_FEATURES are enabled
        PolarBleApiDefaultImpl.defaultImplementation(requireActivity().applicationContext, PolarBleApi.ALL_FEATURES)
    }
    private var scanDisposable: Disposable? = null
    private var autoConnectDisposable: Disposable? = null
    private var ecgDisposable: Disposable? = null
    private var accDisposable: Disposable? = null
    private var gyrDisposable: Disposable? = null
    private var magDisposable: Disposable? = null
    private var ppgDisposable: Disposable? = null
    private var ppiDisposable: Disposable? = null
    private var sdkModeEnableDisposable: Disposable? = null
    private var recordingStartStopDisposable: Disposable? = null
    private var recordingStatusReadDisposable: Disposable? = null
    private var listExercisesDisposable: Disposable? = null
    private var fetchExerciseDisposable: Disposable? = null
    private var removeExerciseDisposable: Disposable? = null

    private var sdkModeEnabledStatus = false
    private var deviceConnected = false
    private var bluetoothEnabled = false
    private var exerciseEntries: MutableList<PolarExerciseEntry> = mutableListOf()

    private lateinit var connectButton: Button
    private lateinit var autoConnectButton: Button
    private lateinit var scanButton: Button
    private lateinit var ecgButton: Button
    private lateinit var accButton: Button
    private lateinit var gyrButton: Button
    private lateinit var magButton: Button
    private lateinit var ppgButton: Button
    private lateinit var ppiButton: Button
    private lateinit var listExercisesButton: Button
    private lateinit var fetchExerciseButton: Button
    private lateinit var removeExerciseButton: Button
    private lateinit var startH10RecordingButton: Button
    private lateinit var stopH10RecordingButton: Button
    private lateinit var readH10RecordingStatusButton: Button
    private lateinit var setTimeButton: Button
    private lateinit var toggleSdkModeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Rename parameter arguments, choose names that match
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_polar_sensors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
        connectButton = view.findViewById(R.id.connect_button)
        autoConnectButton = view.findViewById(R.id.auto_connect_button)
        scanButton = view.findViewById(R.id.scan_button)
        ecgButton = view.findViewById(R.id.ecg_button)
        accButton = view.findViewById(R.id.acc_button)
        gyrButton = view.findViewById(R.id.gyr_button)
        magButton = view.findViewById(R.id.mag_button)
        ppgButton = view.findViewById(R.id.ohr_ppg_button)
        ppiButton = view.findViewById(R.id.ohr_ppi_button)
        listExercisesButton = view.findViewById(R.id.list_exercises)
        fetchExerciseButton = view.findViewById(R.id.read_exercise)
        removeExerciseButton = view.findViewById(R.id.remove_exercise)
        startH10RecordingButton = view.findViewById(R.id.start_h10_recording)
        stopH10RecordingButton = view.findViewById(R.id.stop_h10_recording)
        readH10RecordingStatusButton = view.findViewById(R.id.h10_recording_status)
        setTimeButton = view.findViewById(R.id.set_time)
        toggleSdkModeButton = view.findViewById(R.id.toggle_SDK_mode)

        api.setPolarFilter(false)
        api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId)
                setPolarId(polarDeviceInfo.deviceId)
                deviceConnected = true
                val buttonText = getString(R.string.disconnect_from_device, polarId)
                toggleButtonDown(connectButton, buttonText)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId)
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, polarId)
                toggleButtonUp(connectButton, buttonText)
                toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
            }

            override fun streamingFeaturesReady(
                identifier: String, features: Set<PolarBleApi.DeviceStreamingFeature>
            ) {
                for (feature in features) {
                    Log.d(TAG, "Streaming feature $feature is ready")
                }
            }

            override fun hrFeatureReady(identifier: String) {
                Log.d(TAG, "HR READY: $identifier")
                // hr notifications are about to start
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                //Log.d(TAG, "HR value: ${data.hr} rrsMs: ${data.rrsMs} rr: ${data.rrs} contact: ${data.contactStatus} , ${data.contactStatusSupported}")
                val samples = arrayListOf<Int>()
                samples.add(data.hr)
                samples.addAll(data.rrsMs)
                samples.addAll(data.rrs)
                samples.addAll(listOf(if (data.contactStatus) 1 else 0, if (data.contactStatusSupported) 1 else 0))
                sendMessage("/hr", samples)
            }

            override fun polarFtpFeatureReady(s: String) {
                Log.d(TAG, "FTP ready")
            }
        })

        val sharedPref = (activity as MainActivity).getPreferences(Context.MODE_PRIVATE) ?: return
        polarId = sharedPref.getString(getString(R.string.saved_polar_id_key), polarId).toString()
        connectButton.text = getString(R.string.connect_to_device, polarId)
        connectButton.setOnClickListener {
            val builder: AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder((activity as MainActivity))
            builder.setTitle("Enter Polar ID")

            val input = EditText(activity)
            input.setHint(DEFAULT_POLAR_ID)
            input.setText(polarId)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                setPolarId(input.text.toString())
                connectButton.text = getString(R.string.connect_to_device, polarId)
                try {
                    if (deviceConnected) {
                        api.disconnectFromDevice(polarId)
                    } else {
                        api.connectToDevice(polarId)
                    }
                } catch (polarInvalidArgument: PolarInvalidArgument) {
                    val attempt = if (deviceConnected) {
                        "disconnect"
                    } else {
                        "connect"
                    }
                    Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
                }
            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

            builder.show()
        }

        autoConnectButton.setOnClickListener {
            if (autoConnectDisposable != null) {
                autoConnectDisposable?.dispose()
            }
            autoConnectDisposable = api.autoConnectToDevice(-50, "180D", null)
                .subscribe(
                    { Log.d(TAG, "auto connect search complete") },
                    { throwable: Throwable -> Log.e(TAG, "" + throwable.toString()) }
                )
        }

        scanButton.setOnClickListener {
            val isDisposed = scanDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(scanButton, R.string.scanning_devices)
                scanDisposable = api.searchForDevice()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarDeviceInfo: PolarDeviceInfo ->
                            Log.d(TAG, "polar device found id: " + polarDeviceInfo.deviceId + " address: " + polarDeviceInfo.address + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name + " isConnectable: " + polarDeviceInfo.isConnectable)
                        },
                        { error: Throwable ->
                            toggleButtonUp(scanButton, "Scan devices")
                            Log.e(TAG, "Device scan failed. Reason $error")
                        },
                        { Log.d(TAG, "complete") }
                    )
            } else {
                toggleButtonUp(scanButton, "Scan devices")
                scanDisposable?.dispose()
            }
        }

        ecgButton.setOnClickListener {
            val isDisposed = ecgDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(ecgButton, R.string.stop_ecg_stream)
                ecgDisposable = requestStreamSettings(polarId, PolarBleApi.DeviceStreamingFeature.ECG)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startEcgStreaming(polarId, settings)
                    }
                    .subscribe(
                        { polarEcgData: PolarEcgData ->
                            sendMessage("/ecg", polarEcgData.samples)
                            //for (microVolts in polarEcgData.samples) {
                            //    Log.d(TAG, "    yV: $microVolts")
                            //}
                        },
                        { error: Throwable ->
                            toggleButtonUp(ecgButton, R.string.start_ecg_stream)
                            Log.e(TAG, "ECG stream failed. Reason $error")
                        },
                        { Log.d(TAG, "ECG stream complete") }
                    )
            } else {
                toggleButtonUp(ecgButton, R.string.start_ecg_stream)
                // NOTE stops streaming if it is "running"
                ecgDisposable?.dispose()
            }
        }

        accButton.setOnClickListener {
            val isDisposed = accDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(accButton, R.string.stop_acc_stream)
                accDisposable = requestStreamSettings(polarId, PolarBleApi.DeviceStreamingFeature.ACC)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startAccStreaming(polarId, settings)
                    }
                    //.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            val samples = arrayListOf<Int>()
                            for (data in polarAccelerometerData.samples) {
                                Log.d(TAG, "ACC    x: ${data.x} y:  ${data.y} z: ${data.z}")
                                samples.addAll(listOf(data.x, data.y, data.z))
                            }
                            sendMessage("/acc", samples)
                        },
                        { error: Throwable ->
                            toggleButtonUp(accButton, R.string.start_acc_stream)
                            Log.e(TAG, "ACC stream failed. Reason $error")
                        },
                        {
                            showToast("ACC stream complete")
                            Log.d(TAG, "ACC stream complete")
                        }
                    )
            } else {
                toggleButtonUp(accButton, R.string.start_acc_stream)
                // NOTE dispose will stop streaming if it is "running"
                accDisposable?.dispose()
            }
        }

        gyrButton.setOnClickListener {
            val isDisposed = gyrDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(gyrButton, R.string.stop_gyro_stream)
                gyrDisposable =
                    requestStreamSettings(polarId, PolarBleApi.DeviceStreamingFeature.GYRO)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startGyroStreaming(polarId, settings)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { polarGyroData: PolarGyroData ->
                                sendMessage("/gyro", polarGyroData.samples)
                                for (data in polarGyroData.samples) {
                                    Log.d(TAG, "GYR    x: ${data.x} y:  ${data.y} z: ${data.z}")
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(gyrButton, R.string.start_gyro_stream)
                                Log.e(TAG, "GYR stream failed. Reason $error")
                            },
                            { Log.d(TAG, "GYR stream complete") }
                        )
            } else {
                toggleButtonUp(gyrButton, R.string.start_gyro_stream)
                // NOTE dispose will stop streaming if it is "running"
                gyrDisposable?.dispose()
            }
        }

        magButton.setOnClickListener {
            val isDisposed = magDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(magButton, R.string.stop_mag_stream)
                magDisposable =
                    requestStreamSettings(polarId, PolarBleApi.DeviceStreamingFeature.MAGNETOMETER)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startMagnetometerStreaming(polarId, settings)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { polarMagData: PolarMagnetometerData ->
                                sendMessage("/mag", polarMagData.samples)
                                for (data in polarMagData.samples) {
                                    Log.d(TAG, "MAG    x: ${data.x} y:  ${data.y} z: ${data.z}")
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(magButton, R.string.start_mag_stream)
                                Log.e(TAG, "MAGNETOMETER stream failed. Reason $error")
                            },
                            { Log.d(TAG, "MAGNETOMETER stream complete") }
                        )
            } else {
                toggleButtonUp(magButton, R.string.start_mag_stream)
                // NOTE dispose will stop streaming if it is "running"
                magDisposable!!.dispose()
            }
        }

        ppgButton.setOnClickListener {
            val isDisposed = ppgDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(ppgButton, R.string.stop_ppg_stream)
                ppgDisposable =
                    requestStreamSettings(polarId, PolarBleApi.DeviceStreamingFeature.PPG)
                        .flatMap { settings: PolarSensorSetting ->
                            api.startOhrStreaming(polarId, settings)
                        }
                        .subscribe(
                            { polarOhrPPGData: PolarOhrData ->
                                if (polarOhrPPGData.type == PolarOhrData.OHR_DATA_TYPE.PPG3_AMBIENT1) {
                                    sendMessage("/ppg", polarOhrPPGData.samples)
                                    for (data in polarOhrPPGData.samples) {
                                        Log.d(TAG, "PPG    ppg0: ${data.channelSamples[0]} ppg1: ${data.channelSamples[1]} ppg2: ${data.channelSamples[2]} ambient: ${data.channelSamples[3]}")
                                    }
                                }
                            },
                            { error: Throwable ->
                                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                                Log.e(TAG, "PPG stream failed. Reason $error")
                            },
                            { Log.d(TAG, "PPG stream complete") }
                        )
            } else {
                toggleButtonUp(ppgButton, R.string.start_ppg_stream)
                // NOTE dispose will stop streaming if it is "running"
                ppgDisposable?.dispose()
            }
        }

        ppiButton.setOnClickListener {
            val isDisposed = ppiDisposable?.isDisposed ?: true
            if (isDisposed) {
                toggleButtonDown(ppiButton, R.string.stop_ppi_stream)
                ppiDisposable = api.startOhrPPIStreaming(polarId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { ppiData: PolarOhrPPIData ->
                            sendMessage("/ppi", ppiData.samples)
                            for (sample in ppiData.samples) {
                                Log.d(TAG, "PPI    ppi: ${sample.ppi} blocker: ${sample.blockerBit} errorEstimate: ${sample.errorEstimate}")
                            }
                        },
                        { error: Throwable ->
                            toggleButtonUp(ppiButton, R.string.start_ppi_stream)
                            Log.e(TAG, "PPI stream failed. Reason $error")
                        },
                        { Log.d(TAG, "PPI stream complete") }
                    )
            } else {
                toggleButtonUp(ppiButton, R.string.start_ppi_stream)
                // NOTE dispose will stop streaming if it is "running"
                ppiDisposable?.dispose()
            }
        }

        listExercisesButton.setOnClickListener {
            val isDisposed = listExercisesDisposable?.isDisposed ?: true
            if (isDisposed) {
                exerciseEntries.clear()
                listExercisesDisposable = api.listExercises(polarId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { polarExerciseEntry: PolarExerciseEntry ->
                            Log.d(TAG, "next: ${polarExerciseEntry.date} path: ${polarExerciseEntry.path} id: ${polarExerciseEntry.identifier}")
                            exerciseEntries.add(polarExerciseEntry)
                        },
                        { error: Throwable ->
                            val errorDescription = "Failed to list exercises. Reason: $error"
                            Log.w(TAG, errorDescription)
                            (activity as MainActivity?)!!.showSnackbar(errorDescription)
                        },
                        {
                            val completedOk = "Exercise listing completed. Listed ${exerciseEntries.count()} exercises on device $polarId."
                            Log.d(TAG, completedOk)
                            (activity as MainActivity?)!!.showSnackbar(completedOk)
                        }
                    )
            } else {
                Log.d(TAG, "Listing of exercise entries is in progress at the moment.")
            }
        }

        fetchExerciseButton.setOnClickListener {
            val isDisposed = fetchExerciseDisposable?.isDisposed ?: true
            if (isDisposed) {
                if (exerciseEntries.isNotEmpty()) {
                    // just for the example purpose read the entry which is first on the exerciseEntries list
                    fetchExerciseDisposable = api.fetchExercise(polarId, exerciseEntries.first())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { polarExerciseData: PolarExerciseData ->
                                Log.d(TAG, "Exercise data count: ${polarExerciseData.hrSamples.size} samples: ${polarExerciseData.hrSamples}")
                                var onComplete = "Exercise has ${polarExerciseData.hrSamples.size} hr samples.\n\n"
                                if (polarExerciseData.hrSamples.size >= 3)
                                    onComplete += "HR data {${polarExerciseData.hrSamples[0]}, ${polarExerciseData.hrSamples[1]}, ${polarExerciseData.hrSamples[2]} ...}"
                                showDialog("Exercise data read", onComplete)
                            },
                            { error: Throwable ->
                                val errorDescription = "Failed to read exercise. Reason: $error"
                                Log.e(TAG, errorDescription)
                                (activity as MainActivity?)!!.showSnackbar(errorDescription)
                            }
                        )
                } else {
                    val helpTitle = "Reading exercise is not possible"
                    val helpMessage = "Either device has no exercise entries or you haven't list them yet. Please, create an exercise or use the \"LIST EXERCISES\" " +
                            "button to list exercises on device."
                    showDialog(helpTitle, helpMessage)
                }
            } else {
                Log.d(TAG, "Reading of exercise is in progress at the moment.")
            }
        }

        removeExerciseButton.setOnClickListener {
            val isDisposed = removeExerciseDisposable?.isDisposed ?: true
            if (isDisposed) {
                if (exerciseEntries.isNotEmpty()) {
                    // just for the example purpose remove the entry which is first on the exerciseEntries list
                    val entry = exerciseEntries.first()
                    removeExerciseDisposable = api.removeExercise(polarId, entry)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {
                                exerciseEntries.remove(entry)
                                val exerciseRemovedOk = "Exercise with id:${entry.identifier} successfully removed"
                                Log.d(TAG, exerciseRemovedOk)
                                (activity as MainActivity?)!!.showSnackbar(exerciseRemovedOk)
                            },
                            { error: Throwable ->
                                val exerciseRemoveFailed = "Exercise with id:${entry.identifier} remove failed: $error"
                                Log.w(TAG, exerciseRemoveFailed)
                                (activity as MainActivity?)!!.showSnackbar(exerciseRemoveFailed)
                            }
                        )
                } else {
                    val helpTitle = "Removing exercise is not possible"
                    val helpMessage = "Either device has no exercise entries or you haven't list them yet. Please, create an exercise or use the \"LIST EXERCISES\" button to list exercises on device"
                    showDialog(helpTitle, helpMessage)
                }
            } else {
                Log.d(TAG, "Removing of exercise is in progress at the moment.")
            }
        }

        startH10RecordingButton.setOnClickListener {
            val isDisposed = recordingStartStopDisposable?.isDisposed ?: true
            if (isDisposed) {
                val recordIdentifier = "TEST_APP_ID"
                recordingStartStopDisposable = api.startRecording(polarId, recordIdentifier, PolarBleApi.RecordingInterval.INTERVAL_1S, PolarBleApi.SampleType.HR)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            val recordingStartOk = "Recording started with id $recordIdentifier"
                            Log.d(TAG, recordingStartOk)
                            (activity as MainActivity?)!!.showSnackbar(recordingStartOk)
                        },
                        { error: Throwable ->
                            val title = "Recording start failed with id $recordIdentifier"
                            val message = "Possible reasons are, the recording is already started on the device or there is exercise recorded on H10. " +
                                    "H10 can have one recording in the memory at the time.\n\n" +
                                    "Detailed Reason: $error"
                            Log.e(TAG, "Recording start failed with id $recordIdentifier. Reason: $error")
                            showDialog(title, message)
                        }
                    )
            } else {
                Log.d(TAG, "Recording start or stop request is already in progress at the moment.")
            }
        }

        stopH10RecordingButton.setOnClickListener {
            val isDisposed = recordingStartStopDisposable?.isDisposed ?: true
            if (isDisposed) {
                recordingStartStopDisposable = api.stopRecording(polarId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            val recordingStopOk = "Recording stopped"
                            Log.d(TAG, recordingStopOk)
                            (activity as MainActivity?)!!.showSnackbar(recordingStopOk)
                        },
                        { error: Throwable ->
                            val recordingStopError = "Recording stop failed. Reason: $error"
                            Log.e(TAG, recordingStopError)
                            (activity as MainActivity?)!!.showSnackbar(recordingStopError)
                        }
                    )
            } else {
                Log.d(TAG, "Recording start or stop request is already in progress at the moment.")
            }
        }

        readH10RecordingStatusButton.setOnClickListener {
            val isDisposed = recordingStatusReadDisposable?.isDisposed ?: true
            if (isDisposed) {
                recordingStatusReadDisposable = api.requestRecordingStatus(polarId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { pair: Pair<Boolean, String> ->

                            val recordingOn = pair.first
                            val recordingId = pair.second

                            val recordingStatus = if (!recordingOn && recordingId.isEmpty()) {
                                "H10 Recording is OFF"
                            } else if (!recordingOn && recordingId.isNotEmpty()) {
                                "H10 Recording is OFF.\n\n" +
                                        "Exercise id $recordingId is currently found on H10 memory"
                            } else if (recordingOn && recordingId.isNotEmpty()) {
                                "H10 Recording is ON.\n\n" +
                                        "Exercise id $recordingId recording ongoing"
                            } else if (recordingOn && recordingId.isEmpty()) {
                                // This state is undefined. If recording is currently ongoing the H10 must return id of the recording
                                "H10 Recording state UNDEFINED"
                            } else {
                                // This state is unreachable and should never happen
                                "H10 recording state ERROR"
                            }
                            Log.d(TAG, recordingStatus)
                            showDialog("Recording status", recordingStatus)
                        },
                        { error: Throwable ->
                            val recordingStatusReadError = "Recording status read failed. Reason: $error"
                            Log.e(TAG, recordingStatusReadError)
                            (activity as MainActivity?)!!.showSnackbar(recordingStatusReadError)
                        }
                    )
            } else {
                Log.d(TAG, "Recording status request is already in progress at the moment.")
            }
        }

        setTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            api.setLocalTime(polarId, calendar)
                .subscribe(
                    { Log.d(TAG, "time ${calendar.time} set to device") },
                    { error: Throwable -> Log.d(TAG, "set time failed: $error") }
                )
        }

        toggleSdkModeButton.setOnClickListener {
            toggleSdkModeButton.isEnabled = false
            if (!sdkModeEnabledStatus) {
                sdkModeEnableDisposable = api.enableSDKMode(polarId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d(TAG, "SDK mode enabled")
                            // at this point dispose all existing streams. SDK mode enable command
                            // stops all the streams but client is not informed. This is workaround
                            // for the bug.
                            disposeAllStreams()
                            toggleSdkModeButton.isEnabled = true
                            sdkModeEnabledStatus = true
                            toggleButtonDown(toggleSdkModeButton, R.string.disable_sdk_mode)
                        },
                        { error ->
                            toggleSdkModeButton.isEnabled = true
                            val errorString = "SDK mode enable failed: $error"
                            showToast(errorString)
                            Log.e(TAG, errorString)
                        }
                    )
            } else {
                sdkModeEnableDisposable = api.disableSDKMode(polarId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d(TAG, "SDK mode disabled")
                            toggleSdkModeButton.isEnabled = true
                            sdkModeEnabledStatus = false
                            toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
                        },
                        { error ->
                            toggleSdkModeButton.isEnabled = true
                            val errorString = "SDK mode disable failed: $error"
                            showToast(errorString)
                            Log.e(TAG, errorString)
                        }
                    )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
                } else {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
                }
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    disableAllButtons()
                    Log.w(TAG, "No sufficient permissions")
                    showToast("No sufficient permissions")
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
            enableAllButtons()
        }
    }

    override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    private fun toggleButtonDown(button: Button, text: String? = null) {
        toggleButton(button, true, text)
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
    }

    private fun toggleButtonUp(button: Button, text: String? = null) {
        toggleButton(button, false, text)
    }

    private fun toggleButtonUp(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, false, getString(resourceId))
    }

    private fun toggleButton(button: Button, isDown: Boolean, text: String? = null) {
        if (text != null) button.text = text

        var buttonDrawable = button.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        if (isDown) {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryDarkColor))
        } else {
            DrawableCompat.setTint(buttonDrawable, resources.getColor(R.color.primaryColor))
        }
        button.background = buttonDrawable
    }

    private fun requestStreamSettings(
        identifier: String,
        feature: PolarBleApi.DeviceStreamingFeature
    ): Flowable<PolarSensorSetting> {

        val availableSettings = api.requestStreamSettings(identifier, feature)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn { error: Throwable ->
                val errorString = "Settings are not available for feature $feature. REASON: $error"
                Log.w(TAG, errorString)
                showToast(errorString)
                PolarSensorSetting(emptyMap())
            }
        val allSettings = api.requestFullStreamSettings(identifier, feature)
            .onErrorReturn { error: Throwable ->
                Log.w(
                    TAG,
                    "Full stream settings are not available for feature $feature. REASON: $error"
                )
                PolarSensorSetting(emptyMap())
            }
        return Single.zip(availableSettings, allSettings) { available: PolarSensorSetting, all: PolarSensorSetting ->
            if (available.settings.isEmpty()) {
                throw Throwable("Settings are not available")
            } else {
                Log.d(TAG, "Feature " + feature + " available settings " + available.settings)
                Log.d(TAG, "Feature " + feature + " all settings " + all.settings)
                return@zip android.util.Pair(available, all)
            }
        }
            .observeOn(AndroidSchedulers.mainThread())
            .toFlowable()
            .flatMap(
                Function { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
                    DialogUtility.showAllSettingsDialog(
                        (activity as MainActivity),
                        sensorSettings.first.settings,
                        sensorSettings.second.settings
                    ).toFlowable()
                } as io.reactivex.rxjava3.functions.Function<android.util.Pair<PolarSensorSetting, PolarSensorSetting>, Flowable<PolarSensorSetting>>
            )
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(requireActivity().applicationContext, message, Toast.LENGTH_LONG)
        toast.show()

    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder((activity as MainActivity))
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Respond to positive button press
            }
            .show()
    }

    private fun disableAllButtons() {
        connectButton.isEnabled = false
        autoConnectButton.isEnabled = false
        scanButton.isEnabled = false
        ecgButton.isEnabled = false
        accButton.isEnabled = false
        gyrButton.isEnabled = false
        magButton.isEnabled = false
        ppgButton.isEnabled = false
        ppiButton.isEnabled = false
        listExercisesButton.isEnabled = false
        fetchExerciseButton.isEnabled = false
        removeExerciseButton.isEnabled = false
        startH10RecordingButton.isEnabled = false
        stopH10RecordingButton.isEnabled = false
        readH10RecordingStatusButton.isEnabled = false
        setTimeButton.isEnabled = false
        toggleSdkModeButton.isEnabled = false
    }

    private fun enableAllButtons() {
        connectButton.isEnabled = true
        autoConnectButton.isEnabled = true
        scanButton.isEnabled = true
        ecgButton.isEnabled = true
        accButton.isEnabled = true
        gyrButton.isEnabled = true
        magButton.isEnabled = true
        ppgButton.isEnabled = true
        ppiButton.isEnabled = true
        listExercisesButton.isEnabled = true
        fetchExerciseButton.isEnabled = true
        removeExerciseButton.isEnabled = true
        startH10RecordingButton.isEnabled = true
        stopH10RecordingButton.isEnabled = true
        readH10RecordingStatusButton.isEnabled = true
        setTimeButton.isEnabled = true
        toggleSdkModeButton.isEnabled = true
    }

    private fun disposeAllStreams() {
        ecgDisposable?.dispose()
        accDisposable?.dispose()
        gyrDisposable?.dispose()
        magDisposable?.dispose()
        ppgDisposable?.dispose()
        ppgDisposable?.dispose()
    }

    fun setPolarId(value: String) {
        polarId = value
        val sharedPref = getPreferences()
        with (sharedPref.edit()) {
            putString(getString(R.string.saved_polar_id_key), polarId)
        }
    }

    private fun getPreferences(): SharedPreferences {
        return (activity as MainActivity)!!.getPreferences(Context.MODE_PRIVATE)
    }

    fun sendMessage(address: String, args: List<Any>? = null) {
        AsyncTask.execute {
            (activity as MainActivity?)!!.sendMessage(address, polarId, args)
        }
    }
}