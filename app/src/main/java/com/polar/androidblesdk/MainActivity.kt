package com.polar.androidblesdk

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPortOut
import java.net.InetAddress
import java.net.SocketException


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val ZERO_IP = "0.0.0.0"
        private const val DEFAULT_OSC_PORT = 57120
        private const val DEFAULT_OSC_PREFIX = "/polar"
    }

    var adapterViewPager: FragmentPagerAdapter? = null
    private var oscIp: String = ZERO_IP
    private var oscPort: Int = DEFAULT_OSC_PORT
    private var oscPrefix: String = DEFAULT_OSC_PREFIX
    private lateinit var setOscIpButton: Button
    private lateinit var setOscPortButton: Button
    private lateinit var setOscPrefixButton: Button

    private var sender: OSCPortOut? = null

    class MyPagerAdapter(fragmentManager: FragmentManager?) :
        FragmentPagerAdapter(fragmentManager!!) {
        // Returns total number of pages
        override fun getCount(): Int {
            return NUM_ITEMS
        }

        // Returns the fragment to display for that page
        override fun getItem(position: Int): Fragment {
            return when (position) {
                1 -> PolarSensorsFragment.newInstance(1, "Polar Sensors")
                else -> PhoneSensorsFragment.newInstance(0, "Phone Sensors")
            }
        }

        // Returns the page title for the top indicator
        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                1 -> "Polar Sensors"
                else -> "Phone Sensors"
            }
        }

        companion object {
            private const val NUM_ITEMS = 2
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val vpPager = findViewById<View>(R.id.viewpager) as ViewPager
        adapterViewPager = MyPagerAdapter(supportFragmentManager)
        vpPager.adapter = adapterViewPager

        setOscIpButton = findViewById(com.polar.androidblesdk.R.id.set_osc_ip_button)
        setOscPortButton = findViewById(com.polar.androidblesdk.R.id.set_osc_port_button)
        setOscPrefixButton = findViewById(com.polar.androidblesdk.R.id.set_osc_prefix_button)

        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        oscIp = sharedPref.getString(getString(com.polar.androidblesdk.R.string.saved_osc_ip_key), oscIp).toString()
        setOscIpButton.text = getString(com.polar.androidblesdk.R.string.set_osc_ip, oscIp)
        setOscIpButton.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Enter IP address")

            val input = EditText(this)
            input.setHint(ZERO_IP)
            if (oscIp != ZERO_IP) {
                input.setText(oscIp)
            }
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                oscIp = input.text.toString()
                setOscIpButton.text = getString(com.polar.androidblesdk.R.string.set_osc_ip, oscIp)
                initSender(oscIp, oscPort)
            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

            builder.show()
        }

        oscPort = sharedPref.getInt(getString(com.polar.androidblesdk.R.string.saved_osc_port_key), oscPort)
        setOscPortButton.text = getString(com.polar.androidblesdk.R.string.set_osc_port, Integer.toString(oscPort))
        setOscPortButton.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Enter port number")

            val input = EditText(this)
            input.hint = DEFAULT_OSC_PORT.toString()
            input.setText(oscPort.toString())
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                oscPort = Integer.parseInt(input.text.toString())
                setOscPortButton.text = getString(com.polar.androidblesdk.R.string.set_osc_port, Integer.toString(oscPort))
                initSender(oscIp, oscPort)
            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

            builder.show()
        }

        if (oscIp != ZERO_IP && oscPort > 0) {
            initSender(oscIp, oscPort)
        }

        oscPrefix = sharedPref.getString(getString(com.polar.androidblesdk.R.string.saved_osc_prefix_key), oscPrefix).toString()
        setOscPrefixButton.text = getString(com.polar.androidblesdk.R.string.set_osc_prefix, oscPrefix)
        setOscPrefixButton.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Enter OSC prefix")

            val input = EditText(this)
            input.setHint(DEFAULT_OSC_PREFIX)
            input.setText(oscPrefix)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                oscPrefix = input.text.toString()
                setOscPrefixButton.text = getString(com.polar.androidblesdk.R.string.set_osc_prefix, oscPrefix)
            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

            builder.show()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        sender?.close()
    }

    fun showSnackbar(message: String) {
        val contextView = findViewById<View>(com.polar.androidblesdk.R.id.buttons_container)
        Snackbar.make(contextView, message, Snackbar.LENGTH_LONG)
            .show()
    }

    fun disableAllButtons() {
        setOscPrefixButton.isEnabled = false
    }

    fun enableAllButtons() {
        setOscPrefixButton.isEnabled = true
    }

    private fun initSender(ip: String, port: Int) {
        val isIpAddressValid = Patterns.IP_ADDRESS.matcher(ip).matches()
        return if (isIpAddressValid) {
            try {
                //connect()
                sender = OSCPortOut(InetAddress.getByName(ip), port)
                val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
                with (sharedPref.edit()) {
                    putString(getString(R.string.saved_osc_ip_key), ip)
                    putInt(getString(R.string.saved_osc_port_key), port)
                    apply()
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
        } else {
            //Logger.e("Invalid IP address", this)
        }
    }

    fun sendMessage(address: String, deviceId:String, args: List<Any>? = null) {
        Log.d(TAG, "sendMessage - address: $address - args: $args")
//        Completable
//            .fromCallable {
                val args1 = args?.toMutableList()
                args1?.add(0, deviceId)
                sendOSCMessage(oscPrefix + address, args1)
//            }
//            .subscribeOn(Schedulers.io())
//            .subscribe()
//            .addTo(compositeDisposable)
    }

    private fun sendOSCMessage(address: String, args: List<Any>?) {
        val msg = OSCMessage(address, args)
        try {
            sender?.send(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}