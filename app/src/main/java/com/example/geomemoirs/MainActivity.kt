package com.example.geomemoirs

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.geomemoirs.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize sensor manager and light sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, DeviceAdminReceiver::class.java)


        // Start listening for light sensor changes
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }



        // Set custom action bar layout
        val actionBar = supportActionBar
        actionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        actionBar?.setCustomView(R.layout.action_bar_layout)


        // Making the Avatar open the Options Menu on click
        val avatarImageView = actionBar?.customView?.findViewById<ImageView>(R.id.avatarImageView)
        avatarImageView?.setOnClickListener {
            // Find the avatarImageView
            val avatarImageView = actionBar.customView?.findViewById<ImageView>(R.id.avatarImageView)

            // Create a PopupMenu anchored to the avatarImageView
            val popupMenu = android.widget.PopupMenu(this, avatarImageView)

            // Inflate the menu resource file
            popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)

            // Set a listener for menu item clicks
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_feedback -> {
                        Toast.makeText(baseContext, "Feedback Clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_support -> {
                        Toast.makeText(baseContext, "Support Clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_logout -> {
                        // Handle logout action
                        // Sign out the user and navigate back to the login screen
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            // Show the PopupMenu
            popupMenu.show()
        }


        // login or signup
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If not signed in, navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // If already signed in, proceed to setup navigation
            val navView: BottomNavigationView = binding.navView
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_home, R.id.navigation_dashboard
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister light sensor listener
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        // Register the light sensor listener
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the light sensor listener to avoid battery drain
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_LIGHT -> {
                    val lightValue = event.values[0]

                    // Adjust screen brightness based on light intensity ranges
                    when {
                        lightValue <= 50 -> adjustBrightness(20)
                        lightValue in 51.0..100.0 -> adjustBrightness(40)
                        lightValue in 101.0..150.0 -> adjustBrightness(60)
                        lightValue in 151.0..200.0 -> adjustBrightness(80)
                        else -> adjustBrightness(100)
                    }
                }
                Sensor.TYPE_PROXIMITY -> {
                    // Handle proximity sensor value change (for example, turn off screen if close)
                    if (event.values[0] == 0f) {
                        // Screen is close to an object
                        Log.i("Lock", "locking the device")
                        devicePolicyManager.lockNow()
                    }
                }
            }
        }
    }

    private fun adjustBrightness(brightnessLevel: Int) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightnessLevel / 100.0f
        window.attributes = layoutParams
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}
