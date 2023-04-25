/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AE
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AF
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AWB
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringAction.FLAG_AE
import androidx.camera.core.FocusMeteringAction.FLAG_AF
import androidx.camera.core.FocusMeteringAction.FLAG_AWB
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule.Companion.isLensFacingEnabledInLabTest
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FocusMeteringDeviceTest(
    private val selectorName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig
) {
    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraXConfig)
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "selector={0},config={2}")
        fun data() = listOf(
            arrayOf(
                "front",
                CameraSelector.DEFAULT_FRONT_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "front",
                CameraSelector.DEFAULT_FRONT_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            ),
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            )
        )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider

    private val meteringPointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
    private val validMeteringPoint = meteringPointFactory.createPoint(0.5f, 0.5f)
    private val invalidMeteringPoint = meteringPointFactory.createPoint(0f, 1.1f)

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        withContext(Dispatchers.Main) {
            val fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
            camera = cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector,
                ImageCapture.Builder().build()
            )
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }

        if (selectorName == "front" && implName == CameraPipeConfig::class.simpleName) {
            // TODO(b/264332446): Replace this delay with some API like closeAll() once available
            delay(5000)
        }
    }

    @Test
    fun futureCompletes_whenFocusMeteringStarted() = runBlocking {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        assertFutureCompletes(resultFuture)
    }

    @Test
    fun futureCompletes_whenFocusMeteringIsCancelled() {
        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        val resultFuture = camera.cameraControl.cancelFocusAndMetering()

        assertFutureCompletes(resultFuture)
    }

    @Test
    fun isFocusMeteringSupported_whenMeteringPointValid() = runBlocking {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        assertThat(camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction)).isTrue()
    }

    @Test
    fun isFocusMeteringUnsupported_whenMeteringPointInvalid() = runBlocking {
        val focusMeteringAction = FocusMeteringAction.Builder(invalidMeteringPoint).build()

        assertThat(camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction)).isFalse()
    }

    @Test
    fun focusMeteringSucceeds_whenSupported() = runBlocking {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        assumeThat(
            "FocusMeteringAction not supported!",
            camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction), equalTo(true)
        )

        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)

        assertFutureCompletes(resultFuture)
    }

    @Test
    fun focusMeteringFailsWithIllegalArgumentException_whenMeteringPointInvalid() = runBlocking {
        val focusMeteringAction = FocusMeteringAction.Builder(invalidMeteringPoint).build()

        assumeThat(
            "FocusMeteringAction supported!",
            camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction), equalTo(false)
        )

        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)

        assertFutureFailsWithIllegalArgumentException(resultFuture)
    }

    @Test
    fun focusMeteringFailsWithOperationCanceledException_whenNoUseCaseIsBound() = runBlocking {
        withContext(Dispatchers.Main) {
            cameraProvider.unbindAll()
        }

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()
        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)

        assertFutureFailsWithOperationCancellation(resultFuture)
    }

    @Test
    fun futureCompletes_whenFocusMeteringWithAe() {
        assumeThat(
            "No AE region available on this device!",
            hasMeteringRegion(cameraSelector, FLAG_AE), equalTo(true)
        )

        val action = FocusMeteringAction.Builder(validMeteringPoint, FLAG_AE).build()
        val future = camera.cameraControl.startFocusAndMetering(action)

        assertFutureCompletes(future)
    }

    @Test
    fun futureCompletes_whenFocusMeteringWithAwb() {
        assumeThat(
            "No AWB region available on this device!",
            hasMeteringRegion(cameraSelector, FLAG_AWB), equalTo(true)
        )

        val action = FocusMeteringAction.Builder(validMeteringPoint, FLAG_AWB).build()
        val future = camera.cameraControl.startFocusAndMetering(action)

        assertFutureCompletes(future)
    }

    @Test
    fun futureCompletes_whenFocusMeteringWithAeAwb() {
        assumeThat(
            "No AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector, FLAG_AE or FLAG_AWB), equalTo(true)
        )

        val action = FocusMeteringAction.Builder(validMeteringPoint, FLAG_AE or FLAG_AWB).build()
        val future = camera.cameraControl.startFocusAndMetering(action)

        assertFutureCompletes(future)
    }

    @Test
    fun futureCompletes_whenFocusMeteringWithMorePointsThanSupported() {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        // Most devices don't support 4 AF/AE/AWB regions. but it should still complete.
        val action = FocusMeteringAction.Builder(factory.createPoint(0f, 0f))
            .addPoint(factory.createPoint(1f, 0f))
            .addPoint(factory.createPoint(0.2f, 0.2f))
            .addPoint(factory.createPoint(0.3f, 0.4f))
            .build()
        val future = camera.cameraControl.startFocusAndMetering(action)

        assertFutureCompletes(future)
    }

    /**
     * The following tests check if a device can complete 3A convergence, by setting an auto
     * cancellation with [FocusMeteringAction.Builder.setAutoCancelDuration] which ensures throwing
     * an exception in case of a timeout.
     *
     * Since some devices may require a long time to complete convergence, we are setting a long
     * [FocusMeteringAction.mAutoCancelDurationInMillis] in these tests.
     */

    @Test
    fun futureCompletes_whenFocusMeteringStartedWithLongCancelDuration() = runBlocking {
        Assume.assumeTrue(
            "Not CameraX lab environment," +
                " or lensFacing:${cameraSelector.lensFacing!!} camera is not enabled",
            isLensFacingEnabledInLabTest(lensFacing = cameraSelector.lensFacing!!)
        )

        Assume.assumeTrue(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint)
            .setAutoCancelDuration(5_000, TimeUnit.MILLISECONDS)
            .build()

        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)

        assertFutureCompletes(resultFuture)
    }

    @Test
    fun futureCompletes_whenOnlyAfFocusMeteringStartedWithLongCancelDuration() = runBlocking {
        Assume.assumeTrue(
            "Not CameraX lab environment," +
                " or lensFacing:${cameraSelector.lensFacing!!} camera is not enabled",
            isLensFacingEnabledInLabTest(lensFacing = cameraSelector.lensFacing!!)
        )

        Assume.assumeTrue(
            "No AF region available on this device!",
            hasMeteringRegion(cameraSelector, FLAG_AF)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(
            validMeteringPoint,
            FLAG_AF
        ).setAutoCancelDuration(5_000, TimeUnit.MILLISECONDS)
            .build()

        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)

        assertFutureCompletes(resultFuture)
    }

    @Test
    fun futureCompletes_whenAeAwbFocusMeteringStartedWithLongCancelDuration() = runBlocking {
        Assume.assumeTrue(
            "Not CameraX lab environment," +
                " or lensFacing:${cameraSelector.lensFacing!!} camera is not enabled",
            isLensFacingEnabledInLabTest(lensFacing = cameraSelector.lensFacing!!)
        )

        Assume.assumeTrue(
            "No AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector, FLAG_AE or FLAG_AWB)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(
            validMeteringPoint,
            FLAG_AE or FLAG_AWB
        ).setAutoCancelDuration(5_000, TimeUnit.MILLISECONDS)
            .build()

        val resultFuture = camera.cameraControl.startFocusAndMetering(focusMeteringAction)

        assertFutureCompletes(resultFuture)
    }

    private fun hasMeteringRegion(
        selector: CameraSelector,
        @FocusMeteringAction.MeteringMode flags: Int = FLAG_AF or FLAG_AE or FLAG_AWB
    ): Boolean {
        return try {
            val cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                selector.lensFacing!!
            )
            cameraCharacteristics?.run {
                (if (flags.hasFlag(FLAG_AF)) (get(CONTROL_MAX_REGIONS_AF)!! > 0) else false) ||
                (if (flags.hasFlag(FLAG_AE)) (get(CONTROL_MAX_REGIONS_AE)!! > 0) else false) ||
                (if (flags.hasFlag(FLAG_AWB)) (get(CONTROL_MAX_REGIONS_AWB)!! > 0) else false)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun Int.hasFlag(flag: Int) = (this and flag) != 0

    private fun <T> assertFutureCompletes(future: ListenableFuture<T>) {
        try {
            future[10, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Assert.fail("future fail:$e")
        }
    }

    private fun <T> assertFutureFailsWithIllegalArgumentException(future: ListenableFuture<T>) {
        Assert.assertThrows(ExecutionException::class.java) {
            future[10, TimeUnit.SECONDS]
        }.apply {
            assertThat(cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    private fun <T> assertFutureFailsWithOperationCancellation(future: ListenableFuture<T>) {
        Assert.assertThrows(ExecutionException::class.java) {
            future[10, TimeUnit.SECONDS]
        }.apply {
            assertThat(cause).isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
    }
}
