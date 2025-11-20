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

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.Threads.runOnMainSync
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.media3.effect.Contrast
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [Media3Effect]. */
@SuppressLint("RestrictedApiAndroidX")
@SmallTest
@RunWith(AndroidJUnit4::class)
class Media3EffectDeviceTest {
    val context: Context = ApplicationProvider.getApplicationContext()
    val fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(CameraUtil.PreTestCameraIdList())

    @Test
    fun closeAClosedEffect_throwsException() {
        // Arrange.
        val media3Effect =
            Media3Effect(
                context = context,
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it },
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
                context = context,
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it },
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

    @Test
    fun addMedia3EffectWithoutAnyEffect_previewCanWork(): Unit = runBlocking {
        val media3Effect =
            Media3Effect(
                context = context,
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it },
            )
        verifyPreviewWithMedia3Effect(media3Effect)
    }

    @Test
    fun addMedia3EffectWithEffect_previewCanWork(): Unit = runBlocking {
        val media3Effect =
            Media3Effect(
                context = context,
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it },
            )
        withContext(Dispatchers.Main) { media3Effect.setEffects(listOf(Contrast(0.5f))) }
        verifyPreviewWithMedia3Effect(media3Effect)
    }

    @Test
    fun addMedia3Effect_setEffectTwice_previewCanWork(): Unit = runBlocking {
        val media3Effect =
            Media3Effect(
                context = context,
                targets = CameraEffect.PREVIEW,
                executor = mainThreadExecutor(),
                errorListener = { throw it },
            )

        // Set Effect first time
        withContext(Dispatchers.Main) { media3Effect.setEffects(listOf(Contrast(0.5f))) }

        var frameLatch: CountDownLatch? = null
        verifyPreviewWithMedia3Effect(media3Effect, onFrameAvailable = { frameLatch?.countDown() })

        // Set Effect second time
        withContext(Dispatchers.Main) { media3Effect.setEffects(listOf(Contrast(0.7f))) }

        // Make sure the preview is still streaming.
        frameLatch = CountDownLatch(5)
        assertThat(frameLatch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    suspend fun verifyPreviewWithMedia3Effect(
        media3Effect: Media3Effect,
        onFrameAvailable: (() -> Unit)? = null,
    ) {
        // Arrange.
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        assumeTrue(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
        // Act: bindToLifecycle with the media3Effect
        val frameLatch = CountDownLatch(5)
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider { surfaceTexture ->
                    frameLatch.countDown()
                    onFrameAvailable?.invoke()
                }

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(preview).addEffect(media3Effect).build(),
            )
        }

        // Assert: verify if frame is coming
        assertThat(frameLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }
}
