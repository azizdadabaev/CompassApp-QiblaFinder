package com.example.compassapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.compassapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter(SensorService.onsensor_changed_action_key)
        )

    }
    override fun onResume() {
        super.onResume()
        startForegroundServiceForCompassSensor(false)
    }

    private fun startForegroundServiceForCompassSensor(background: Boolean) {
        val compassSensorIntent = Intent(this, SensorService::class.java)
        compassSensorIntent.putExtra(SensorService.background_key, background)
        ContextCompat.startForegroundService(this, compassSensorIntent)
    }

    override fun onPause() {
        super.onPause()
        startForegroundServiceForCompassSensor(true)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val direction = intent.getStringExtra(SensorService.direction_key)
            val angle = intent.getDoubleExtra(SensorService.angle_key,0.0)
            val angleWithDirection = "$angle  $direction"

            binding.directionTextView.text = angleWithDirection
            binding.imgCompass.rotation = angle.toFloat() * -1
        }
    }










}