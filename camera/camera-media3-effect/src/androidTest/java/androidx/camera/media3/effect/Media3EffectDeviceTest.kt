/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.media3.effect

import android.util.Size
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.Threads.runOnMainSync
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [Media3Effect]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class Media3EffectDeviceTest {

    @Test
    fun closeAClosedEffect_throwsException() {
        // Arrange.
        val media3Effect =
            Media3Effect(
                context = ApplicationProvider.getApplicationContext(),
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it }
            )
        var exception: Exception? = null

        // Act: close the effect twice.
        runOnMainSync {
            media3Effect.close()
            try {
                media3Effect.close()
            } catch (e: IllegalStateException) {
                exception = e
            }
        }

        // Assert: IllegalStateException was thrown.
        assertThat(exception!!).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun closeEffect_pendingRequestIsCancelled() {
        // Arrange: create a Media3Effect and a SurfaceRequest.
        val media3Effect =
            Media3Effect(
                context = ApplicationProvider.getApplicationContext(),
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it }
            )
        val surfaceRequest = SurfaceRequest(Size(10, 10), FakeCamera()) {}

        // Act: provide the surface request and close the effect.
        runOnMainSync {
            media3Effect.surfaceProcessor!!.onInputSurface(surfaceRequest)
            media3Effect.close()
        }

        // Assert: the surface request is cancelled.
        var exception: Exception? = null
        try {
            surfaceRequest.deferrableSurface.surface.get()
        } catch (e: Exception) {
            exception = e
        }
        assertThat(exception!!.message).contains("Surface request will not complete.")
    }
}
