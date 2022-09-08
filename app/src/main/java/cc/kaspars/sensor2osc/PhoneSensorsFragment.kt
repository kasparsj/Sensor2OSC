package cc.kaspars.sensor2osc

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.os.Bundle
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

    private var accListener: SensorListener? = null
    private var gyroListener: SensorListener? = null
    private var magListener: SensorListener? = null
    private var quatListener: QuatListener? = null
    private var accSensor:Sensor? = null
    private var gyroSensor:Sensor? = null
    private var magSensor:Sensor? = null
    private var quatSensor:Sensor? = null

    private lateinit var sensorManager: SensorManager
    private lateinit var accButton: Button
    private lateinit var gyroButton: Button
    private lateinit var magButton: Button
    private lateinit var quatButton: Button

    class SensorListener(private var context:Fragment, private var address: String, private var numArgs:Int) : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            val samples = arrayListOf<Int>()
            for (i in 0..(numArgs-1)) {
                samples.add(event.values[i].toInt());
            }
            (context as PhoneSensorsFragment).sendMessage(address, samples)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //TODO: we'll see later about it
        }
    }

    class QuatListener(private var context:Fragment, private var address: String) : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            val quat = FloatArray(4);
            SensorManager.getQuaternionFromVector(quat, event.values)

            val samples = arrayListOf<Float>()
            samples.add(quat.get(1))
            samples.add(quat.get(2))
            samples.add(quat.get(3))
            samples.add(quat.get(0))

            (context as PhoneSensorsFragment).sendMessage(address, samples)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //TODO: we'll see later about it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getInt(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
        sensorManager = (activity as MainActivity).getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        quatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_phone_sensors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accButton = view.findViewById(R.id.acc_button)
        gyroButton = view.findViewById(R.id.gyr_button)
        magButton = view.findViewById(R.id.mag_button)
        quatButton = view.findViewById(R.id.quat_button)

        accButton.setOnClickListener {
            if (magSensor == null) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no accelerometer on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (accListener == null) {
                accListener = SensorListener(this, "/acc", 3)
                sensorManager.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_GAME)
                toggleButtonDown(accButton, R.string.stop_acc_stream)
            } else {
                sensorManager.unregisterListener(accListener)
                accListener = null
                toggleButtonUp(accButton, R.string.start_acc_stream)
            }
        }

        gyroButton.setOnClickListener {
            if (magSensor == null) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no gyroscope on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (gyroListener == null) {
                gyroListener = SensorListener(this, "/gyro", 3)
                sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
                toggleButtonDown(gyroButton, R.string.stop_gyro_stream)
            } else {
                sensorManager.unregisterListener(gyroListener)
                toggleButtonUp(gyroButton, R.string.start_gyro_stream)
            }
        }

        magButton.setOnClickListener {
            if (magSensor == null) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no magnetometer on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (magListener == null) {
                magListener = SensorListener(this, "/mag", 3)
                sensorManager.registerListener(magListener, magSensor, SensorManager.SENSOR_DELAY_GAME)
                toggleButtonDown(magButton, R.string.stop_mag_stream)
            } else {
                toggleButtonUp(magButton, R.string.start_mag_stream)
                sensorManager.unregisterListener(magListener)
            }
        }

        quatButton.setOnClickListener {
            if (quatSensor == null) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    "There is no rotation_vector on your device",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (quatListener == null) {
                quatListener = QuatListener(this, "/quat")
                sensorManager.registerListener(quatListener, quatSensor, SensorManager.SENSOR_DELAY_GAME)
                toggleButtonDown(quatButton, R.string.stop_quat_stream)
            } else {
                toggleButtonUp(quatButton, R.string.start_quat_stream)
                sensorManager.unregisterListener(quatListener)
            }
        }
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

    fun sendMessage(address:String, args: List<Any>? = null) {
        AsyncTask.execute {
            (activity as MainActivity).sendMessage(address, android.os.Build.MODEL, args = args)
        }
    }
}