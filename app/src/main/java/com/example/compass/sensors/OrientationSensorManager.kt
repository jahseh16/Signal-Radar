package com.example.compass.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassSensorData(
  val azimuth: Float = 0f, // 0° .. 360° (Heading / Rumbo)
  val pitch: Float = 0f, // Degrees (-90° .. +90°, Inclinación frontal)
  val roll: Float = 0f, // Degrees (-180° .. +180°, Inclinación lateral)
  val pressureHpa: Float? = null, // Barometric pressure
  val barometricAltitudeMeters: Double? = null, // Barometric altitude in meters
  val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
  val hasBarometer: Boolean = false,
  val hasRotationVector: Boolean = false,
  val isCalibrated: Boolean = true,
)

class OrientationSensorManager(private val context: Context) : SensorEventListener {

  private val tag = "OrientationSensors"
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  private val _sensorData = MutableStateFlow(CompassSensorData())
  val sensorData: StateFlow<CompassSensorData> = _sensorData.asStateFlow()

  // Sensors
  private val rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val magneticSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
  private val pressureSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

  // Matrices for orientation calculation
  private val rotationMatrix = FloatArray(9)
  private val truncatedRotationVector = FloatArray(4)
  private val orientationAngles = FloatArray(3)
  private val accelReading = FloatArray(3)
  private val magnetReading = FloatArray(3)
  private var hasAccel = false
  private var hasMagnet = false

  // Low-pass filter smoothing values
  private var smoothAzimuth = 0f
  private var smoothPitch = 0f
  private var smoothRoll = 0f
  private var isFirstReading = true

  private var isListening = false

  init {
    _sensorData.value = _sensorData.value.copy(
      hasBarometer = pressureSensor != null,
      hasRotationVector = rotationVectorSensor != null
    )
  }

  fun start() {
    if (isListening || sensorManager == null) return
    isListening = true
    isFirstReading = true

    // Register rotation vector or fallback
    if (rotationVectorSensor != null) {
      sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    } else {
      accelerometerSensor?.let {
        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
      }
      magneticSensor?.let {
        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
      }
    }

    // Register barometer with high responsiveness if supported
    pressureSensor?.let {
      sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
    }
  }

  fun stop() {
    if (!isListening || sensorManager == null) return
    isListening = false
    sensorManager.unregisterListener(this)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return

    when (event.sensor.type) {
      Sensor.TYPE_ROTATION_VECTOR -> {
        handleRotationVector(event)
      }
      Sensor.TYPE_ACCELEROMETER -> {
        System.arraycopy(event.values, 0, accelReading, 0, accelReading.size)
        hasAccel = true
        if (rotationVectorSensor == null && hasMagnet) {
          computeOrientationFromAccelAndMagnet()
        }
      }
      Sensor.TYPE_MAGNETIC_FIELD -> {
        System.arraycopy(event.values, 0, magnetReading, 0, magnetReading.size)
        hasMagnet = true
        if (rotationVectorSensor == null && hasAccel) {
          computeOrientationFromAccelAndMagnet()
        }
      }
      Sensor.TYPE_PRESSURE -> {
        handlePressure(event)
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
      val isCalibrated = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
      _sensorData.value = _sensorData.value.copy(
        sensorAccuracy = accuracy,
        isCalibrated = isCalibrated
      )
    }
  }

  private fun handleRotationVector(event: SensorEvent) {
    try {
      if (event.values.size > 4) {
        System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, truncatedRotationVector)
      } else {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
      }

      SensorManager.getOrientation(rotationMatrix, orientationAngles)
      processOrientationAngles(orientationAngles)
    } catch (e: Exception) {
      Log.e(tag, "Error calculating rotation vector matrix", e)
    }
  }

  private fun computeOrientationFromAccelAndMagnet() {
    val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelReading, magnetReading)
    if (success) {
      SensorManager.getOrientation(rotationMatrix, orientationAngles)
      processOrientationAngles(orientationAngles)
    }
  }

  private fun processOrientationAngles(angles: FloatArray) {
    // Azimuth: angles[0] (-PI to PI) -> convert to degrees (0 to 360)
    var rawAzimuthDeg = Math.toDegrees(angles[0].toDouble()).toFloat()
    rawAzimuthDeg = (rawAzimuthDeg + 360f) % 360f

    // Pitch: angles[1] (-PI/2 to PI/2) -> degrees
    val rawPitchDeg = Math.toDegrees(angles[1].toDouble()).toFloat()

    // Roll: angles[2] (-PI to PI) -> degrees
    val rawRollDeg = Math.toDegrees(angles[2].toDouble()).toFloat()

    if (isFirstReading) {
      smoothAzimuth = rawAzimuthDeg
      smoothPitch = rawPitchDeg
      smoothRoll = rawRollDeg
      isFirstReading = false
    } else {
      // Shortest-path angular smoothing for azimuth
      val angleDiff = Math.toDegrees(
        atan2(
          sin(Math.toRadians((rawAzimuthDeg - smoothAzimuth).toDouble())),
          cos(Math.toRadians((rawAzimuthDeg - smoothAzimuth).toDouble()))
        )
      ).toFloat()
      val filterFactor = 0.18f
      smoothAzimuth = (smoothAzimuth + angleDiff * filterFactor + 360f) % 360f

      // Standard linear low-pass for pitch and roll
      smoothPitch += (rawPitchDeg - smoothPitch) * filterFactor
      smoothRoll += (rawRollDeg - smoothRoll) * filterFactor
    }

    _sensorData.value = _sensorData.value.copy(
      azimuth = smoothAzimuth,
      pitch = smoothPitch,
      roll = smoothRoll
    )
  }

  private fun handlePressure(event: SensorEvent) {
    val pressureHpa = event.values[0]
    val baroAltitude = SensorManager.getAltitude(
      SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
      pressureHpa
    ).toDouble()

    _sensorData.value = _sensorData.value.copy(
      pressureHpa = pressureHpa,
      barometricAltitudeMeters = baroAltitude
    )
  }
}
