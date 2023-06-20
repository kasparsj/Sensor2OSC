package cc.kaspars.sensor2osc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class SensorService : Service(), SensorEventListener {

    companion object {
        const val ACTION_SEND_MESSAGE = "sendOSCMessage"
    }

    private lateinit var sensorManager: SensorManager
    private var accSensor:Sensor? = null
    private var gyroSensor:Sensor? = null
    private var magSensor:Sensor? = null
    private var quatSensor:Sensor? = null
    private var accListener:Boolean = false;
    private var gyroListener:Boolean = false;
    private var magListener:Boolean = false;
    private var quatListener:Boolean = false;

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    override fun onCreate() {
        super.onCreate()

        // Init sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        quatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentTitle("Sensor Service")
            .setContentText("Listening to sensor data...")
            .setSmallIcon(R.drawable.ic_sensor_service)
            .build()

        startForeground(101, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String{
        val channelId = "my_service"
        val channelName = "My Background Service"
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onSensorChanged(event: SensorEvent) {
        when(event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val samples = arrayListOf<Float>()
                for (i in 0..2) {
                    samples.add(event.values[i]);
                }
                sendMessage("/acc", samples)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val samples = arrayListOf<Float>()
                for (i in 0..2) {
                    samples.add(event.values[i]);
                }
                sendMessage("/gyro", samples)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val samples = arrayListOf<Float>()
                for (i in 0..2) {
                    samples.add(event.values[i]);
                }
                sendMessage("/mag", samples)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val quat = FloatArray(4);
                SensorManager.getQuaternionFromVector(quat, event.values)

                val samples = arrayListOf<Float>()
                samples.add(quat.get(1))
                samples.add(quat.get(2))
                samples.add(quat.get(3))
                samples.add(quat.get(0))

                sendMessage("/quat", samples)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Here you handle the accuracy change
        Log.i("SensorService", "Sensor accuracy changed: $accuracy")
    }

    fun hasSensor(type:Int):Boolean {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> accSensor != null
            Sensor.TYPE_GYROSCOPE -> gyroSensor != null
            Sensor.TYPE_MAGNETIC_FIELD -> magSensor != null
            Sensor.TYPE_ROTATION_VECTOR -> quatSensor != null
            else -> false
        }
    }

    fun toggleSensorListener(type: Int):Boolean {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accListener = if (!accListener) {
                    sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME)
                } else {
                    sensorManager.unregisterListener(this, accSensor)
                    false
                }
                return accListener
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroListener = if (!gyroListener) {
                    sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
                } else {
                    sensorManager.unregisterListener(this, gyroSensor)
                    false
                }
                return gyroListener
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magListener = if (!magListener) {
                    sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_GAME)
                } else {
                    sensorManager.unregisterListener(this, magSensor)
                    false
                }
                return magListener
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                quatListener = if (!quatListener) {
                    sensorManager.registerListener(this, quatSensor, SensorManager.SENSOR_DELAY_GAME)
                } else {
                    sensorManager.unregisterListener(this, quatSensor)
                    false
                }
                return quatListener
            }
            else -> false
        }
    }

    fun sendMessage(address:String, args: ArrayList<Float>? = null) {
        val intent = Intent(ACTION_SEND_MESSAGE)
        intent.putExtra("address", address)
        if (args != null) {
            intent.putExtra("args", args.toFloatArray())
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}