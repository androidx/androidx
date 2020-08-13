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
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.pipe.CameraId
import androidx.test.core.app.ApplicationProvider
import kotlinx.atomicfu.atomic
import org.robolectric.Shadows
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

/**
 * Utility class for creating, configuring, and interacting with FakeCamera objects via Robolectric
 *
 * TODO: Implement a utility method to create a fake CameraDevice when robolectric is updated to 4.4
 */
object FakeCameras {
    private val cameraIds = atomic(0)

    val application: Application
        get() {
            val app: Application = ApplicationProvider.getApplicationContext()
            val shadowApp: ShadowApplication = Shadows.shadowOf(app)
            shadowApp.grantPermissions(Manifest.permission.CAMERA)
            return app
        }

    private val cameraManager: CameraManager
        get() = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager

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

        return cameraId
    }

    operator fun get(camera: CameraId): CameraCharacteristics {
        return cameraManager.getCameraCharacteristics(camera.value)
    }
}
