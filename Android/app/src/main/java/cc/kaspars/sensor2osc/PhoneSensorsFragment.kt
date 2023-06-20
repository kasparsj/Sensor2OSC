package cc.kaspars.sensor2osc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PhoneSensorsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PhoneSensorsFragment : Fragment() {

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: Int, param2: String) =
            PhoneSensorsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        private const val TAG = "PhoneSensorsFragment"
    }

    private lateinit var accButton: Button
    private lateinit var gyroButton: Button
    private lateinit var magButton: Button
    private lateinit var quatButton: Button

    private var sensorService: SensorService? = null
    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SensorService.LocalBinder
            sensorService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        startSensorService();

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_phone_sensors, container, false)
    }

    private fun startSensorService() {
        val serviceIntent = Intent(context, SensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(serviceIntent)
        } else {
            context?.startService(serviceIntent)
        }
        context?.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorService()
    }

    private fun stopSensorService() {
        val serviceIntent = Intent(context, SensorService::class.java)
        context?.stopService(serviceIntent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accButton = view.findViewById(R.id.acc_button)
        gyroButton = view.findViewById(R.id.gyr_button)
        magButton = view.findViewById(R.id.mag_button)
        quatButton = view.findViewById(R.id.quat_button)

        accButton.setOnClickListener {
            if (hasSensor(Sensor.TYPE_ACCELEROMETER) == false) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no accelerometer on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (toggleSensorListener(Sensor.TYPE_ACCELEROMETER) == true) {
                toggleButtonDown(accButton, R.string.stop_acc_stream)
            }
            else {
                toggleButtonUp(accButton, R.string.start_acc_stream)
            }
        }

        gyroButton.setOnClickListener {
            if (hasSensor(Sensor.TYPE_GYROSCOPE) == false) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no gyroscope on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (toggleSensorListener(Sensor.TYPE_MAGNETIC_FIELD) == true) {
                toggleButtonDown(gyroButton, R.string.stop_gyro_stream)
            }
            else {
                toggleButtonUp(gyroButton, R.string.start_gyro_stream)
            }
        }

        magButton.setOnClickListener {
            if (hasSensor(Sensor.TYPE_MAGNETIC_FIELD) == false) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no magnetometer on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (toggleSensorListener(Sensor.TYPE_MAGNETIC_FIELD) == true) {
                toggleButtonDown(magButton, R.string.stop_mag_stream)
            }
            else {
                toggleButtonUp(magButton, R.string.start_mag_stream)
            }
        }

        quatButton.setOnClickListener {
            if (hasSensor(Sensor.TYPE_ROTATION_VECTOR) == false) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no rotation_vector on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (toggleSensorListener(Sensor.TYPE_ROTATION_VECTOR) == true) {
                toggleButtonDown(quatButton, R.string.stop_quat_stream)
            }
            else {
                toggleButtonUp(quatButton, R.string.start_quat_stream)
            }
        }
    }

    private fun toggleButtonDown(button: Button, @StringRes resourceId: Int) {
        toggleButton(button, true, getString(resourceId))
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

    private fun hasSensor(type:Int): Boolean? {
        if (isBound) {
            return sensorService?.hasSensor(type);
        }
        return null;
    }

    private fun toggleSensorListener(type:Int):Boolean? {
        if (isBound) {
            return sensorService?.toggleSensorListener(type)
        }
        return null;
    }
}