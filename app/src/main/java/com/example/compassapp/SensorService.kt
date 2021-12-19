package com.example.compassapp

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlin.math.round
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private val accelerometerInput = FloatArray(3)
    private val magnetometerInput = FloatArray(3)
    private val rotMatrix = FloatArray(9)
    private val orientMatrix = FloatArray(3)
    private var background = false
    private val notificationActivityRequestCode = 0
    private val notificationId = 1
    private val notificationStopRequestCode = 2

    companion object {
        val angle_key = "angle"
        val direction_key = "direction"
        val background_key = "background"
        val notification_id_key = "notificationId"
        val onsensor_changed_action_key = "com.example.compassapp.ON_SENSOR_CHANGED"
        val notificationStop_action_key = "com.example.compassapp.NOTIFICATION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }

        val notification = createNotification(getString(R.string.not_found), 0.0)
        startForeground(notificationId, notification)
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            background = it.getBooleanExtra(background_key, false)
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        //checking sensor type and copying values
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerInput, 0, accelerometerInput.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerInput, 0, magnetometerInput.size)
        }

        updateOrientationAngles()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotMatrix, null, accelerometerInput, magnetometerInput)

        val orientation = SensorManager.getOrientation(rotMatrix, orientMatrix)
        val degrees = (Math.toDegrees(orientation.get(0).toDouble()) + 360.0) % 360.0
        val angle = round(degrees * 100) / 100
        val direction = getDirection(degrees)

        val intent = Intent()
        intent.putExtra(angle_key, angle)
        intent.putExtra(direction_key, direction)
        intent.action = onsensor_changed_action_key

        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        if (background){
            val notification = createNotification(direction, angle)
            startForeground(notificationId, notification)
        } else {
            stopForeground(true)
        }
    }

    private fun createNotification(direction: String, angle: Double): Notification {

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                application.packageName,
                "Notifications", NotificationManager.IMPORTANCE_DEFAULT
            )

            // Configure the notification channel.
            notificationChannel.enableLights(false)
            notificationChannel.setSound(null, null)
            notificationChannel.enableVibration(false)
            notificationChannel.vibrationPattern = longArrayOf(0L)
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(baseContext, application.packageName)
        // Open activity intent
        val contentIntent = PendingIntent.getActivity(
            this, notificationActivityRequestCode,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        // Stop notification intent
        val stopNotificationIntent = Intent(this, WifiP2pManager.ActionListener::class.java)
        stopNotificationIntent.action = notificationStop_action_key
        stopNotificationIntent.putExtra(notification_id_key, notificationId)
        val pendingStopNotificationIntent =
            PendingIntent.getBroadcast(this, notificationStopRequestCode, stopNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText("You're currently facing $direction at an angle of $angle°")
            .setWhen(System.currentTimeMillis())
            .setDefaults(0)
            .setVibrate(longArrayOf(0L))
            .setSound(null)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .addAction(R.mipmap.ic_launcher,
                getString(R.string.stop_notifications), pendingStopNotificationIntent)


        return notificationBuilder.build()
    }

    private fun getDirection(angle: Double): String {
        var direction = ""

        if (angle >= 350 || angle <= 10)
            direction = "N"
        if (angle < 350 && angle > 280)
            direction = "NW"
        if (angle <= 280 && angle > 260)
            direction = "W"
        if (angle <= 260 && angle > 190)
            direction = "SW"
        if (angle <= 190 && angle > 170)
            direction = "S"
        if (angle <= 170 && angle > 100)
            direction = "SE"
        if (angle <= 100 && angle > 80)
            direction = "E"
        if (angle <= 80 && angle > 10)
            direction = "NE"

        return direction
    }

    class ActionListener : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null && intent.action != null) {
                if (intent.action.equals(notificationStop_action_key)) {
                    context?.let {
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val locatyIntent = Intent(context, SensorService::class.java)
                        context.stopService(locatyIntent)
                        val notificationId = intent.getIntExtra(notification_id_key, -1)
                        if (notificationId != -1) {
                            notificationManager.cancel(notificationId)
                        }
                    }
                }
            }
        }
    }

}
