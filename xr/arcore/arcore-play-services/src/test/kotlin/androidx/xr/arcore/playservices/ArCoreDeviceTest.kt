/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.playservices

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.TrackingState as RuntimeTrackingState
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Session as ArCore1xSession
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSensor

@RunWith(AndroidJUnit4::class)
class ArCoreDeviceTest {
    private lateinit var underTest: ArCoreDevice
    private lateinit var mockSession: ArCore1xSession
    private lateinit var mockCamera: Camera
    private lateinit var mockFrame: Frame

    @Before
    fun setUp() {
        underTest = ArCoreDevice()
        mockSession = mock<ArCore1xSession>()
        mockCamera = mock<Camera>()
        mockFrame = mock<Frame>()
    }

    @Test
    fun update_updatesDevicePose() {
        val expectedPose = Pose(Vector3(1f, 1f, 1f), Quaternion(1f, 1f, 1f, 1f))
        whenever(mockSession.update()).thenReturn(mockFrame)
        whenever(mockFrame.camera).thenReturn(mockCamera)
        whenever(mockCamera.pose).thenReturn(expectedPose.toARCorePose())
        check(underTest.devicePose != expectedPose)

        val frame = mockSession.update()
        val context = ApplicationProvider.getApplicationContext<Context>()
        underTest.configureTracking(DeviceTrackingMode.SPATIAL, context)
        underTest.update(frame)

        assertThat(underTest.devicePose).isEqualTo(expectedPose)
    }

    @Test
    @Suppress("DEPRECATION")
    fun update_withInertialTrackingMode_usesSensorPoseAndSetsTracking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val shadowSensorManager = shadowOf(sensorManager)
        shadowSensorManager.addSensor(
            org.robolectric.shadows.ShadowSensor.newInstance(
                android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR
            )
        )

        underTest.configureTracking(DeviceTrackingMode.INERTIAL, context)
        underTest.resume()

        val eventValues = floatArrayOf(0f, 0f, 0f, 1f)
        val sensorEvent = org.robolectric.shadows.ShadowSensorManager.createSensorEvent(4)
        System.arraycopy(eventValues, 0, sensorEvent.values, 0, 4)
        sensorEvent.sensor =
            sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR)
        shadowSensorManager.sendSensorEventToListeners(sensorEvent)

        val tempArray = FloatArray(4)
        android.hardware.SensorManager.getQuaternionFromVector(tempArray, eventValues)
        val sensorRotation =
            Quaternion(x = tempArray[1], y = tempArray[2], z = tempArray[3], w = tempArray[0])
        val expectedRotation = Quaternion.fromEulerAngles(90f, 0f, 0f) * sensorRotation
        val expectedTranslation =
            (expectedRotation *
                Vector3(0f, ArCoreDevice.VERTICAL_OFFSET, ArCoreDevice.HORIZONTAL_OFFSET)) -
                Vector3(0f, ArCoreDevice.VERTICAL_OFFSET, 0f)
        val expectedPose = Pose(translation = expectedTranslation, rotation = expectedRotation)

        underTest.update(mockFrame)

        assertThat(underTest.devicePose).isEqualTo(expectedPose)
        assertThat(underTest.trackingState).isEqualTo(RuntimeTrackingState.TRACKING)
    }

    @Test
    fun update_withInertialModeAndSensorMissing_returnsIdentityAndStopped() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        underTest.configureTracking(DeviceTrackingMode.INERTIAL, context)
        underTest.resume()

        underTest.update(mockFrame)

        assertThat(underTest.devicePose).isEqualTo(Pose())
        assertThat(underTest.trackingState).isEqualTo(RuntimeTrackingState.STOPPED)
    }

    @Test
    @Suppress("DEPRECATION")
    fun update_withInertialModeAndNotResumed_returnsPaused() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val shadowSensorManager = shadowOf(sensorManager)
        shadowSensorManager.addSensor(
            org.robolectric.shadows.ShadowSensor.newInstance(
                android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR
            )
        )

        underTest.configureTracking(DeviceTrackingMode.INERTIAL, context)
        underTest.resume()

        val eventValues = floatArrayOf(0f, 0f, 0f, 1f)
        val sensorEvent = org.robolectric.shadows.ShadowSensorManager.createSensorEvent(4)
        System.arraycopy(eventValues, 0, sensorEvent.values, 0, 4)
        sensorEvent.sensor =
            sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR)
        shadowSensorManager.sendSensorEventToListeners(sensorEvent)

        underTest.pause()

        val tempArray = FloatArray(4)
        android.hardware.SensorManager.getQuaternionFromVector(tempArray, eventValues)
        val sensorRotation =
            Quaternion(x = tempArray[1], y = tempArray[2], z = tempArray[3], w = tempArray[0])
        val expectedRotation = Quaternion.fromEulerAngles(90f, 0f, 0f) * sensorRotation
        val expectedPose = Pose(rotation = expectedRotation)

        underTest.update(mockFrame)

        assertThat(underTest.devicePose).isEqualTo(expectedPose)
        assertThat(underTest.trackingState).isEqualTo(RuntimeTrackingState.PAUSED)
    }

    @Test
    fun resume_inertialMode_registersSensorListener() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val shadowSensorManager = shadowOf(sensorManager)
        shadowSensorManager.addSensor(
            org.robolectric.shadows.ShadowSensor.newInstance(
                android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR
            )
        )

        underTest.configureTracking(DeviceTrackingMode.INERTIAL, context)
        underTest.resume()

        assertThat(shadowSensorManager.listeners).hasSize(1)
    }

    @Test
    fun pause_unregistersSensorListener() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val shadowSensorManager = shadowOf(sensorManager)
        shadowSensorManager.addSensor(
            org.robolectric.shadows.ShadowSensor.newInstance(
                android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR
            )
        )

        underTest.configureTracking(DeviceTrackingMode.INERTIAL, context)
        underTest.resume()
        underTest.pause()

        assertThat(shadowSensorManager.listeners).isEmpty()
    }
}
