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
package androidx.camera.viewfinder.view

import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.Surface
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.TransformationMode
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ViewfinderViewTest(private val implementationMode: ImplementationMode) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(ImplementationMode.EMBEDDED, ImplementationMode.EXTERNAL)
    }

    @get:Rule val testName = TestName()

    private val requestNum = AtomicInteger(1)
    private val surfaceRequest: ViewfinderSurfaceRequest
        get() =
            ViewfinderSurfaceRequest(
                width = ANY_WIDTH,
                height = ANY_HEIGHT,
                implementationMode = implementationMode,
                requestId = "${testName.methodName}[${requestNum.andIncrement}]",
            )

    @Test
    fun doNotProvideSurface_ifSurfaceTextureNotAvailableYet() =
        runViewfinderTest(viewfinderInitiallyAttached = false) {
            assertThrows<TimeoutCancellationException> {
                withTimeout(REQUEST_TIMEOUT) {
                    withContext(Dispatchers.Main) {
                        viewfinder.requestSurfaceSession(surfaceRequest).close()
                    }
                }
            }
        }

    @Test
    fun provideSurface_ifSurfaceTextureAvailable() = runViewfinderTest {
        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) { viewfinder.requestSurfaceSession(surfaceRequest) }
            }
            .use { session -> assertThat(session.surface.isValid).isTrue() }
    }

    @Test
    fun provideSurface_ifSurfaceTextureAvailable_whenTransformInfoProvided() = runViewfinderTest {
        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) {
                    viewfinder.transformationInfo = ANY_TRANSFORMATION_INFO
                    viewfinder.requestSurfaceSession(surfaceRequest)
                }
            }
            .use { session -> assertThat(session.surface.isValid).isTrue() }
    }

    @Test
    fun surfaceReleased_afterViewRemoved_thenSessionClosed() = runViewfinderTest {
        assume()
            .withMessage(
                "SurfaceView does not support keeping the Surface valid once the View is " +
                    "detached from the window on API version < 29."
            )
            .that(
                implementationMode == ImplementationMode.EMBEDDED ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            )
            .isTrue()

        val surface: Surface
        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) { viewfinder.requestSurfaceSession(surfaceRequest) }
            }
            .use { session ->
                // Ensure session has an initially valid Surface
                assertThat(session.surface.isValid).isTrue()

                detachViewfinder()
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()

                // After detaching, Surface should still be valid since we are keeping the session
                // open
                assertThat(session.surface.isValid).isTrue()
                surface = session.surface
            }

        // Both view has been detached and session is closed. Surface should be released.
        assertThat(surface.isValid).isFalse()
    }

    @Test
    fun surfaceReleased_afterSessionClosed_thenViewRemoved() = runViewfinderTest {
        val surface: Surface
        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) { viewfinder.requestSurfaceSession(surfaceRequest) }
            }
            .use { session ->
                // Ensure session has an initially valid Surface
                assertThat(session.surface.isValid).isTrue()
                surface = session.surface
            }
        // After session is closed, surface should still be valid since view is still attached
        assertThat(surface.isValid).isTrue()

        detachViewfinder()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // Both view has been detached and session is closed. Surface should be released.
        assertThat(surface.isValid).isFalse()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun oldSurfaceRequestCancelled_whenNewSurfaceRequestSent() =
        runViewfinderTest(viewfinderInitiallyAttached = false) {
            val mainContext: CoroutineContext = Dispatchers.Main
            val firstRequestJob =
                launch(mainContext, start = CoroutineStart.ATOMIC) {
                    // Viewfinder is not attached, so this should never complete. It will be
                    // cancelled
                    // by the second request.
                    viewfinder.requestSurfaceSession(surfaceRequest)
                }

            // Send second request asynchronously to cancel first
            val secondRequestFuture =
                withContext(mainContext) { viewfinder.requestSurfaceSessionAsync(surfaceRequest) }
            try {
                firstRequestJob.join()
                assertThat(firstRequestJob.isCancelled).isTrue()
            } finally {
                // Clean up: Ensure we cancel second request
                secondRequestFuture.cancel(false)
            }
        }

    @Test
    fun preAppliedTransformation_getBitmap_rotatesContentBySourceRotation() = runViewfinderTest {
        assume().that(implementationMode).isEqualTo(ImplementationMode.EMBEDDED)
        val request =
            ViewfinderSurfaceRequest(
                width = 200,
                height = 200,
                implementationMode = implementationMode,
                requestId = testName.methodName,
            )

        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) {
                    viewfinder.transformationInfo =
                        TransformationInfo(
                            sourceRotation = 90,
                            transformationMode = TransformationMode.PRE_APPLIED,
                        )
                    viewfinder.requestSurfaceSession(request)
                }
            }
            .use { session ->
                // Pre-rotate quadrants by -90 degrees (TL=Green, TR=Yellow, BL=Red, BR=Blue) so a
                // 90-degree clockwise viewfinder rotation produces (TL=Red, TR=Green, BL=Blue,
                // BR=Yellow).
                val paint = Paint().apply { style = Paint.Style.FILL }
                val canvas = session.surface.lockHardwareCanvas()
                try {
                    paint.color = Color.GREEN
                    canvas.drawRect(0f, 0f, 100f, 100f, paint)
                    paint.color = Color.YELLOW
                    canvas.drawRect(100f, 0f, 200f, 100f, paint)
                    paint.color = Color.RED
                    canvas.drawRect(0f, 100f, 100f, 200f, paint)
                    paint.color = Color.BLUE
                    canvas.drawRect(100f, 100f, 200f, 200f, paint)
                } finally {
                    session.surface.unlockCanvasAndPost(canvas)
                }

                // Wait for TextureView to receive the frame and verify getBitmap() applies the
                // 90-degree PRE_APPLIED rotation.
                var bitmap = withContext(Dispatchers.Main) { viewfinder.bitmap }
                var attempts = 0
                while (
                    attempts < 30 &&
                        (bitmap == null ||
                            Color.red(bitmap.getPixel(bitmap.width / 4, bitmap.height / 4)) < 200)
                ) {
                    delay(50)
                    bitmap = withContext(Dispatchers.Main) { viewfinder.bitmap }
                    attempts++
                }
                val captured = checkNotNull(bitmap)
                val w = captured.width
                val h = captured.height
                assertThat(Color.red(captured.getPixel(w / 4, h / 4))).isGreaterThan(200)
                assertThat(Color.green(captured.getPixel(3 * w / 4, h / 4))).isGreaterThan(200)
                assertThat(Color.blue(captured.getPixel(w / 4, 3 * h / 4))).isGreaterThan(200)
                assertThat(Color.red(captured.getPixel(3 * w / 4, 3 * h / 4))).isGreaterThan(200)
                assertThat(Color.green(captured.getPixel(3 * w / 4, 3 * h / 4))).isGreaterThan(200)
            }
    }
}
