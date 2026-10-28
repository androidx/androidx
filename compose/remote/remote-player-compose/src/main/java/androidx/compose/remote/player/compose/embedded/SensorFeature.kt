/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.emptyIntSet
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

/**
 * Feeds device sensors into the sensor system variables (`ID_ACCELERATION_X` .. `ID_LIGHT`) that a
 * document listens to.
 *
 * Sensors are a host capability rather than operations of a profile. Like the View player's
 * `SensorSupport`, this registers only the sensors whose variables the document references, so
 * documents without sensor variables register nothing.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object SensorFeature : RcPlayerFeature(Key("Sensor")) {
    override val opcodes: IntSet = emptyIntSet()

    @Composable
    override fun Install(scope: RcFeatureDocumentScope) {
        val context = LocalContext.current
        DisposableEffect(scope, context) {
            val registration = SensorRegistration.register(context, scope.remoteContext)
            onDispose { registration?.unregister() }
        }
    }
}

/** A listener registered for the device sensors whose variables a document reads. */
private class SensorRegistration(
    private val sensorManager: SensorManager,
    private val remoteContext: RemoteContext,
    private val sensors: List<Pair<Sensor, DeviceSensor>>,
) : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        // Called at sensor rate, so avoid iterator and lambda allocations.
        val deviceSensor = sensors.fastFirstOrNull { it.first == event.sensor }?.second ?: return
        deviceSensor.ids.forEachIndexed { index, id ->
            // A store write: the graph reads these ids from the snapshot-backed store, so
            // dependent draws re-run.
            remoteContext.loadFloat(id, event.values[index])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    companion object {
        /** Registers for the sensors [remoteContext]'s document reads, or returns null if none. */
        fun register(context: Context, remoteContext: RemoteContext): SensorRegistration? {
            val state = remoteContext.mRemoteComposeState
            val needed = DeviceSensors.fastFilter { sensor ->
                sensor.ids.any { state.hasListener(it) }
            }
            if (needed.isEmpty()) return null
            val sensorManager =
                context.applicationContext.getSystemService(SensorManager::class.java)
                    ?: return null
            val sensors = ArrayList<Pair<Sensor, DeviceSensor>>(needed.size)
            needed.fastForEach { deviceSensor ->
                sensorManager.getDefaultSensor(deviceSensor.type)?.let {
                    sensors.add(it to deviceSensor)
                }
            }
            if (sensors.isEmpty()) return null
            val registration = SensorRegistration(sensorManager, remoteContext, sensors)
            sensors.fastForEach { (sensor, _) ->
                sensorManager.registerListener(
                    registration,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
            }
            return registration
        }
    }
}

/** A device sensor and the system variables its readings feed, in `SensorEvent.values` order. */
private class DeviceSensor(val type: Int, val ids: IntArray)

private val DeviceSensors =
    listOf(
        DeviceSensor(
            Sensor.TYPE_ACCELEROMETER,
            intArrayOf(
                RemoteContext.ID_ACCELERATION_X,
                RemoteContext.ID_ACCELERATION_Y,
                RemoteContext.ID_ACCELERATION_Z,
            ),
        ),
        DeviceSensor(
            Sensor.TYPE_GYROSCOPE,
            intArrayOf(
                RemoteContext.ID_GYRO_ROT_X,
                RemoteContext.ID_GYRO_ROT_Y,
                RemoteContext.ID_GYRO_ROT_Z,
            ),
        ),
        DeviceSensor(
            Sensor.TYPE_MAGNETIC_FIELD,
            intArrayOf(
                RemoteContext.ID_MAGNETIC_X,
                RemoteContext.ID_MAGNETIC_Y,
                RemoteContext.ID_MAGNETIC_Z,
            ),
        ),
        DeviceSensor(Sensor.TYPE_LIGHT, intArrayOf(RemoteContext.ID_LIGHT)),
    )
