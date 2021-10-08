/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.camera.camera2.pipe.testing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.compat.Camera2CameraMetadata
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import kotlinx.atomicfu.atomic
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

/**
 * Utility class for creating, configuring, and interacting with Robolectric's [CameraManager].
 */
public object RobolectricCameras {
    private val cameraIds = atomic(0)

    val application: Application
        get() {
            val app: Application = ApplicationProvider.getApplicationContext()
            val shadowApp: ShadowApplication = shadowOf(app)
            shadowApp.grantPermissions(Manifest.permission.CAMERA)
            return app
        }

    val cameraManager: CameraManager
        get() = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val initializedCameraIds = mutableSetOf<CameraId>()

    /**
     * This will create, configure, and add the specified CameraCharacteristics to the Robolectric
     * CameraManager, which allows the camera characteristics to be queried for tests and for Fake
     * CameraDevice objects to be created for tests.
     */
    @Suppress("MissingPermission")
    fun create(
        metadata: Map<CameraCharacteristics.Key<*>, Any> = emptyMap()
    ): CameraId {
        val shadowCameraManager = Shadow.extract<Any>(
            cameraManager
        ) as ShadowCameraManager

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)

        // Configure the camera characteristics
        for (entry in metadata) {
            shadowCharacteristics.set(entry.key, entry.value)
        }

        val cameraNumber = cameraIds.incrementAndGet()
        val cameraId = CameraId("FakeCamera-$cameraNumber")

        // Add the camera to the camera service
        shadowCameraManager.addCamera(cameraId.value, characteristics)
        initializedCameraIds.add(cameraId)

        return cameraId
    }

    operator fun get(fakeCameraId: CameraId): CameraCharacteristics {
        check(initializedCameraIds.contains(fakeCameraId)) {
            "CameraId ($fakeCameraId) MUST be created before being accessed!"
        }
        return cameraManager.getCameraCharacteristics(fakeCameraId.value)
    }

    @SuppressLint("MissingPermission")
    fun open(cameraId: CameraId): FakeCamera {
        check(initializedCameraIds.contains(cameraId))
        val characteristics = cameraManager.getCameraCharacteristics(cameraId.value)
        val metadata = Camera2CameraMetadata(
            cameraId,
            false,
            characteristics,
            FakeCameraMetadataProvider(),
            emptyMap()
        )

        @Suppress("SyntheticAccessor")
        val callback = CameraStateCallback(cameraId)
        cameraManager.openCamera(
            cameraId.value,
            callback,
            Handler()
        )

        // Wait until the camera is "opened" by robolectric.
        shadowOf(Looper.myLooper()).idle()
        val cameraDevice = callback.camera!!

        @Suppress("SyntheticAccessor")
        return FakeCamera(
            cameraId,
            characteristics,
            metadata,
            cameraDevice
        )
    }

    /** Remove all fake camera instances from Robolectric */
    fun clear() {
        val shadowCameraManager = Shadow.extract<Any>(
            cameraManager
        ) as ShadowCameraManager
        for (cameraId in initializedCameraIds) {
            try {
                shadowCameraManager.removeCamera(cameraId.value)
            } catch (e: Throwable) {
                // Ignored - This is a cleanup task.
            }
        }
        initializedCameraIds.clear()
    }

    /**
     * The [FakeCamera] instance wraps up several useful objects for use in tests.
     */
    data class FakeCamera(
        val cameraId: CameraId,
        val characteristics: CameraCharacteristics,
        val metadata: CameraMetadata,
        val cameraDevice: CameraDevice
    )

    private class CameraStateCallback(private val cameraId: CameraId) :
        CameraDevice.StateCallback() {
        var camera: CameraDevice? = null
        override fun onOpened(cameraDevice: CameraDevice) {
            check(cameraDevice.id == cameraId.value)
            this.camera = cameraDevice
        }

        override fun onDisconnected(camera: CameraDevice) {
            throw UnsupportedOperationException(
                "onDisconnected is not expected for Robolectric Camera"
            )
        }

        override fun onError(
            camera: CameraDevice,
            error: Int
        ) {
            throw UnsupportedOperationException("onError is not expected for Robolectric Camera")
        }
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RobolectricCamerasTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val mainLooper = shadowOf(Looper.getMainLooper())

    @Test
    fun fakeCamerasCanBeOpened() {
        val fakeCameraId = RobolectricCameras.create(
            mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK)
        )
        val fakeCamera = RobolectricCameras.open(fakeCameraId)

        Truth.assertThat(fakeCamera).isNotNull()
        Truth.assertThat(fakeCamera.cameraId).isEqualTo(fakeCameraId)
        Truth.assertThat(fakeCamera.cameraDevice).isNotNull()
        Truth.assertThat(fakeCamera.characteristics).isNotNull()
        Truth.assertThat(fakeCamera.characteristics[CameraCharacteristics.LENS_FACING]).isNotNull()
        Truth.assertThat(fakeCamera.metadata).isNotNull()
        Truth.assertThat(fakeCamera.metadata[CameraCharacteristics.LENS_FACING]).isNotNull()
    }

    @After
    fun teardown() {
        mainLooper.idle()
        RobolectricCameras.clear()
    }
}