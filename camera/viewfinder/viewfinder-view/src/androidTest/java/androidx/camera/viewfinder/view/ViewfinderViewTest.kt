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

import android.os.Build
import android.view.Surface
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
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
@SdkSuppress(minSdkVersion = 21)
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

        var surface: Surface
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
        var surface: Surface
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
    @OptIn(ExperimentalCoroutinesApi::class)
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
}
