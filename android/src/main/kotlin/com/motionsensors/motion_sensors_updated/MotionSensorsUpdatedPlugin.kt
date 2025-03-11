package com.motionsensors.motion_sensors_updated

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** MotionSensorsUpdatedPlugin */
class MotionSensorsUpdatedPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var sensorManager: SensorManager
    private lateinit var methodChannel: MethodChannel
    private lateinit var context: Context
    private lateinit var screenOrientationHandler: ScreenOrientationHandler

    private val sensors = mutableMapOf<String, SensorHandler>()

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        methodChannel = MethodChannel(binding.binaryMessenger, "motion_sensors/method")
        methodChannel.setMethodCallHandler(this)

        setupEventChannels(binding.binaryMessenger)
    }

    private fun setupEventChannels(messenger: BinaryMessenger) {
        val sensorTypes = mapOf(
            "accelerometer" to Sensor.TYPE_ACCELEROMETER,
            "gyroscope" to Sensor.TYPE_GYROSCOPE,
            "magnetometer" to Sensor.TYPE_MAGNETIC_FIELD,
            "user_accelerometer" to Sensor.TYPE_LINEAR_ACCELERATION,
            "orientation" to Sensor.TYPE_GAME_ROTATION_VECTOR,
            "absolute_orientation" to Sensor.TYPE_ROTATION_VECTOR,
            "screen_orientation" to Sensor.TYPE_ROTATION_VECTOR  // Using rotation vector for consistency
        )

        sensorTypes.forEach { (name, type) ->
            val channelName = "motion_sensors/$name"
            val eventChannel = EventChannel(messenger, channelName)
            val sensor = sensorManager.getDefaultSensor(type)
            val handler = if (name == "screen_orientation") {
                ScreenOrientationHandler(context, sensorManager).also { screenOrientationHandler = it }
            } else {
                SensorHandler(sensorManager, sensor)
            }
            eventChannel.setStreamHandler(handler)
            sensors[name] = handler
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        sensors.values.forEach { it.teardown() }
        sensors.clear()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "isSensorAvailable" -> {
                val sensorType = call.argument<String>("sensorType")
                result.success(sensors[sensorType]?.isSensorAvailable())
            }
            "setSensorUpdateInterval" -> {
                val sensorType = call.argument<String>("sensorType")
                val interval = call.argument<Int>("interval") ?: SensorManager.SENSOR_DELAY_NORMAL
                sensors[sensorType]?.setUpdateInterval(interval)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

class SensorHandler(private val sensorManager: SensorManager, private val sensor: Sensor?) : EventChannel.StreamHandler, SensorEventListener {
    private var eventSink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        sensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onCancel(arguments: Any?) {
        sensorManager.unregisterListener(this)
        eventSink = null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.values?.let {
            eventSink?.success(it.toList())
        }
    }

    fun isSensorAvailable(): Boolean = sensor != null

    fun setUpdateInterval(interval: Int) {
        if (sensor != null) {
            sensorManager.unregisterListener(this)
            sensorManager.registerListener(this, sensor, interval)
        }
    }

    fun teardown() {
        sensorManager.unregisterListener(this)
    }
}

class ScreenOrientationHandler(private val context: Context, private val sensorManager: SensorManager) : EventChannel.StreamHandler, SensorEventListener {
    private var eventSink: EventChannel.EventSink? = null
    private var lastRotation: Double = -1.0

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onCancel(arguments: Any?) {
        sensorManager.unregisterListener(this)
        eventSink = null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val rotation = getScreenOrientation()
        if (rotation != lastRotation) {
            eventSink?.success(rotation)
            lastRotation = rotation
        }
    }

    private fun getScreenOrientation(): Double {
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_0 -> 0.0
            Surface.ROTATION_90 -> 90.0
            Surface.ROTATION_180 -> 180.0
            Surface.ROTATION_270 -> 270.0
            else -> 0.0
        }
    }
}
