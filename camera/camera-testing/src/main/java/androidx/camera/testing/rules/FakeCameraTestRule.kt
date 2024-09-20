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

package androidx.camera.testing.rules

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraSelector.Builder
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Logger
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.lifecycle.Lifecycle.State.RESUMED
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [FakeCameraTestRule] prepares fake camera resources when the test rule is applied and also
 * provides some convenience functions for camera-related tests.
 *
 * The test rule ensures that a [CameraXConfig] is configured before start of each test and cleared
 * again at the end of each test.
 *
 * @param cameraXConfig A custom [CameraXConfig] to be used for configuration, this should usually
 *   be created via [FakeAppConfig] so that it aligns with the [getFakeCamera] method.
 * @property context The application [Context].
 * @property timeoutMillis The timeout duration in milliseconds for retrieving a
 *   [ProcessCameraProvider] instance, 5 seconds by default. See [ProcessCameraProvider.getInstance]
 *   for details.
 */
@VisibleForTesting
@SuppressLint("UnsafeOptInUsageError") // for ExperimentalCameraProviderConfiguration
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // used in prototypes first to confirm usefulness
public class FakeCameraTestRule
@JvmOverloads
constructor(
    private val context: Context,
    private val timeoutMillis: Long = 5_000L,
    cameraXConfig: CameraXConfig? = null // TODO - Ensure this is from FakeAppConfig.
) : TestRule {
    private val config = cameraXConfig ?: FakeAppConfig.create()

    /**
     * The [ProcessCameraProvider] instance that is used for binding [UseCase]s to camera,
     * initialized when the test rule is applied.
     */
    public lateinit var cameraProvider: ProcessCameraProvider
        private set

    /**
     * A fake [androidx.lifecycle.LifecycleOwner] that is used to drive the CameraX operations in
     * test and started/resumed whenever required (e.g. binding use cases).
     *
     * The lifecycle is started by default and should be in [RESUMED] state during test.
     */
    // TODO - Consider exposing this (both getting and setting) in future if required
    private val fakeLifecycleOwner: FakeLifecycleOwner =
        FakeLifecycleOwner().apply { startAndResume() }

    /**
     * Initiates the [ProcessCameraProvider] singleton instance with a timeout duration of
     * [timeoutMillis].
     *
     * @throws AssertionError When the initialization does not complete within `timeoutMillis`.
     */
    private fun initCameraProvider() {
        val latch = CountDownLatch(1)
        ProcessCameraProvider.getInstance(context)
            .addListener(
                {
                    this.cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    latch.countDown()
                },
                CameraXExecutors.directExecutor()
            )

        assertWithMessage("ProcessCameraProvider.getInstance timed out!")
            .that(latch.await(timeoutMillis, TimeUnit.MILLISECONDS))
            .isTrue()
    }

    /**
     * Binds use cases to the camera with a [fakeLifecycleOwner].
     *
     * The fake lifecycle owner is automatically created and started by this test rule.
     */
    @MainThread
    public fun bindUseCases(lensFacing: @LensFacing Int, useCases: List<UseCase>) {
        cameraProvider.bindToLifecycle(
            fakeLifecycleOwner,
            Builder().requireLensFacing(lensFacing).build(),
            *useCases.toTypedArray()
        )
    }

    /** Unbinds use cases from the camera they are bound to. */
    @MainThread
    public fun unbindUseCases(useCases: List<UseCase>) {
        cameraProvider.unbind(*useCases.toTypedArray())
    }

    /**
     * Gets the underlying [FakeCamera] that drives CameraX operations.
     *
     * @throws NotImplementedError if lensFacing is not [LENS_FACING_BACK] or [LENS_FACING_FRONT].
     */
    public fun getFakeCamera(lensFacing: @LensFacing Int): FakeCamera =
        when (lensFacing) {
            LENS_FACING_BACK -> FakeAppConfig.getBackCamera()
            LENS_FACING_FRONT -> FakeAppConfig.getFrontCamera()
            else -> throw NotImplementedError("Only back and front cameras are supported")
        }

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                ProcessCameraProvider.configureInstance(config)
                Logger.d(TAG, "apply.evaluate: ProcessCameraProvider.configureInstance() completed")

                initCameraProvider()
                Logger.d(TAG, "apply.evaluate: initCameraProvider completed")

                try {
                    base?.evaluate()
                } finally {
                    ProcessCameraProvider.clearConfiguration()
                }
            }
        }
    }

    @VisibleForTesting internal fun isCameraProviderInitialized() = ::cameraProvider.isInitialized

    private companion object {
        const val TAG = "FakeCameraTestRule"
    }
}
