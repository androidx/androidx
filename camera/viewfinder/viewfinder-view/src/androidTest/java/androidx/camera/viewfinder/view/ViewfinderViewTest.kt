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

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.TransformationMode
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSession
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
    fun preAppliedTransformation_rendersUprightAcrossRotationChanges() = runViewfinderTest {
        runPreAppliedQuadrantsTest { session ->
            withContext(Dispatchers.Main) {
                assertThat(viewfinder.surfaceImplementationMode).isEqualTo(implementationMode)
            }

            // Includes a 180 degree change, which doesn't change the aspect ratio.
            for (sourceRotation in listOf(270, 0, 180)) {
                withContext(Dispatchers.Main) {
                    viewfinder.transformationInfo = preAppliedTransformationInfo(sourceRotation)
                }
                assertRendersUpright(session.surface, sourceRotation)
            }
        }
    }

    @Test
    fun preAppliedTransformation_180DegreeSourceRotationChange_redrawsPausedStream() =
        runViewfinderTest {
            runPreAppliedQuadrantsTest {
                // Flip sourceRotation by 180 degrees without queuing a new frame to verify paused
                // streams redraw immediately.
                withContext(Dispatchers.Main) {
                    viewfinder.transformationInfo = preAppliedTransformationInfo(270)
                }

                assertQuadrantColorsEventually(
                    expected = listOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED),
                    description = "Paused stream quadrant colors after 90->270 flip",
                ) {
                    delay(100)
                    captureViewfinderQuadrantColors()
                }
            }
        }

    /**
     * Requests a surface session for a PRE_APPLIED source rotated by [initialRotation], checks that
     * the viewfinder renders the quadrants upright, and runs [block] with the session.
     */
    private suspend fun ViewfinderTestScope.runPreAppliedQuadrantsTest(
        initialRotation: Int = 90,
        block: suspend ViewfinderTestScope.(session: ViewfinderSurfaceSession) -> Unit,
    ) {
        assume()
            .withMessage(
                "SurfaceControl is required to rotate PRE_APPLIED content in EXTERNAL mode."
            )
            .that(
                implementationMode == ImplementationMode.EMBEDDED ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            )
            .isTrue()
        val request =
            ViewfinderSurfaceRequest(
                width = QUADRANTS_SURFACE_WIDTH,
                height = QUADRANTS_SURFACE_HEIGHT,
                implementationMode = implementationMode,
                requestId = testName.methodName,
            )
        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) {
                    viewfinder.transformationInfo = preAppliedTransformationInfo(initialRotation)
                    viewfinder.requestSurfaceSession(request)
                }
            }
            .use { session ->
                assertRendersUpright(session.surface, initialRotation)
                block(session)
            }
    }

    private fun preAppliedTransformationInfo(sourceRotation: Int) =
        TransformationInfo(
            sourceRotation = sourceRotation,
            transformationMode = TransformationMode.PRE_APPLIED,
        )

    /**
     * Draws quadrants pre-rotated for [sourceRotation] to [surface] until the viewfinder shows them
     * upright on screen, and fails if it doesn't within a timeout.
     */
    private suspend fun ViewfinderTestScope.assertRendersUpright(
        surface: Surface,
        sourceRotation: Int,
    ) =
        assertQuadrantColorsEventually(
            expected = UPRIGHT_QUADRANT_COLORS,
            description = "Quadrant colors at sourceRotation=$sourceRotation",
        ) {
            withContext(Dispatchers.Main) {
                surface.drawPreRotatedQuadrants(
                    QUADRANTS_SURFACE_WIDTH,
                    QUADRANTS_SURFACE_HEIGHT,
                    sourceRotation,
                )
            }
            delay(100)
            captureViewfinderQuadrantColors()
        }

    /**
     * Calls [capture] until it returns colors close to [expected], and fails if it doesn't within a
     * timeout.
     */
    private suspend fun assertQuadrantColorsEventually(
        expected: List<Int>,
        description: String,
        capture: suspend () -> List<Int>,
    ) {
        var lastColors: List<Int> = emptyList()
        val matched =
            withTimeoutOrNull(3.seconds) {
                while (
                    lastColors.size != expected.size ||
                        !lastColors.zip(expected).all { (actual, expected) ->
                            actual.isCloseTo(expected)
                        }
                ) {
                    lastColors = capture()
                }
                true
            } ?: false

        assertWithMessage(
                "$description (TL, TR, BL, BR)=" + lastColors.map { Integer.toHexString(it) }
            )
            .that(matched)
            .isTrue()
    }

    /** Returns the on-screen colors at the center of each quadrant of the viewfinder. */
    private suspend fun ViewfinderTestScope.captureViewfinderQuadrantColors(): List<Int> {
        val (location, size) =
            withContext(Dispatchers.Main) {
                val location = IntArray(2).also { viewfinder.getLocationOnScreen(it) }
                location to Size(viewfinder.width, viewfinder.height)
            }
        val screenshot =
            checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            return screenshot.quadrantColors(location[0], location[1], size.width, size.height)
        } finally {
            screenshot.recycle()
        }
    }

    /** Returns the colors at the center of each quadrant of the given region of this bitmap. */
    private fun Bitmap.quadrantColors(
        left: Int = 0,
        top: Int = 0,
        width: Int = this.width,
        height: Int = this.height,
    ): List<Int> =
        listOf(1 to 1, 3 to 1, 1 to 3, 3 to 3).map { (x, y) ->
            getPixel(left + x * width / 4, top + y * height / 4)
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
        assume()
            .withMessage("PixelCopy is required for ViewfinderView.getBitmap() in EXTERNAL mode.")
            .that(
                implementationMode == ImplementationMode.EMBEDDED ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            )
            .isTrue()
        val request =
            ViewfinderSurfaceRequest(
                width = 200,
                height = 200,
                implementationMode = implementationMode,
                requestId = testName.methodName,
            )

        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) {
                    viewfinder.transformationInfo = preAppliedTransformationInfo(90)
                    viewfinder.requestSurfaceSession(request)
                }
            }
            .use { session ->
                session.surface.drawPreRotatedQuadrants(200, 200, sourceRotation = 90)

                // Verify getBitmap() applies the 90-degree PRE_APPLIED rotation.
                assertQuadrantColorsEventually(
                    expected = UPRIGHT_QUADRANT_COLORS,
                    description = "getBitmap() quadrant colors",
                ) {
                    delay(50)
                    withContext(Dispatchers.Main) { viewfinder.bitmap }
                        ?.let { bitmap ->
                            try {
                                bitmap.quadrantColors()
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        .orEmpty()
                }
            }
    }
}
