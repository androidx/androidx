/*
 * Copyright 2020 The Android Open Source Project
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
import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamType
import androidx.camera.camera2.pipe.impl.CameraGraphModules
import androidx.camera.camera2.pipe.impl.CameraMetadataImpl
import androidx.camera.camera2.pipe.impl.CameraPipeModules
import androidx.camera.camera2.pipe.wrapper.AndroidCameraDevice
import androidx.camera.camera2.pipe.wrapper.CameraDeviceWrapper
import androidx.test.core.app.ApplicationProvider
import dagger.Module
import dagger.Provides
import kotlinx.atomicfu.atomic
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import java.lang.UnsupportedOperationException
import javax.inject.Singleton

/**
 * Utility class for creating, configuring, and interacting with FakeCamera objects via Robolectric
 */
object FakeCameras {
    private val cameraIds = atomic(0)

    val application: Application
        get() {
            val app: Application = ApplicationProvider.getApplicationContext()
            val shadowApp: ShadowApplication = shadowOf(app)
            shadowApp.grantPermissions(Manifest.permission.CAMERA)
            return app
        }

    private val cameraManager: CameraManager
        get() = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val initializedCameraIds = mutableSetOf<CameraId>()

    /**
     * This will create, configure, and add the specified CameraCharacteristics to the Robolectric
     * CameraManager, which allows the camera characteristics to be queried for tests and for Fake
     * CameraDevice objects to be created for tests.
     */
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
        // Note: It would be better if this was something like "Fake-Camera-1", but robolectric
        // will not let you open a CameraDevice unless the id is numeric.
        val cameraId = CameraId("FakeCamera-$cameraNumber")

        // Add the camera to the camera service
        shadowCameraManager.addCamera(cameraId.value, characteristics)
        initializedCameraIds.add(cameraId)

        return cameraId
    }

    operator fun get(fakeCameraId: CameraId): CameraCharacteristics {
        check(initializedCameraIds.contains(fakeCameraId))
        return cameraManager.getCameraCharacteristics(fakeCameraId.value)
    }

    fun open(cameraId: CameraId): FakeCamera {
        check(initializedCameraIds.contains(cameraId))
        val characteristics = cameraManager.getCameraCharacteristics(cameraId.value)
        val metadata = CameraMetadataImpl(cameraId, false, characteristics, emptyMap())

        val callback = CameraStateCallback(cameraId)
        cameraManager.openCamera(
            cameraId.value,
            callback,
            Handler()
        )
        shadowOf(Looper.myLooper()).idle()

        val cameraDevice = callback.camera!!
        val cameraDeviceWrapper = AndroidCameraDevice(metadata, cameraDevice, cameraId)

        return FakeCamera(
            cameraId,
            characteristics,
            metadata,
            cameraDevice,
            cameraDeviceWrapper
        )
    }

    /** Remove all fake cameras */
    fun removeAll() {
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
        val cameraDevice: CameraDevice,
        val cameraDeviceWrapper: CameraDeviceWrapper
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

    /**
     * Utility module for testing the Dagger generated graph with a a reasonable default config.
     */
    @Module(includes = [CameraPipeModules::class])
    class FakeCameraPipeModule(
        private val context: Context,
        private val fakeCamera: FakeCamera
    ) {
        @Provides
        @Singleton
        fun provideFakeCameraPipeConfig() = CameraPipe.Config(context)

        @Provides
        @Singleton
        fun provideFakeGraphConfig() = CameraGraph.Config(
            camera = fakeCamera.cameraId,
            streams = listOf(
                StreamConfig(
                    Size(640, 480),
                    StreamFormat.YUV_420_888,
                    fakeCamera.cameraId,
                    StreamType.SURFACE
                )
            ),
            template = RequestTemplate(0)
        )
    }

    @Module(includes = [CameraGraphModules::class])
    class FakeCameraGraphModule
}
