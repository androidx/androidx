/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.locationbutton

import android.app.Activity
import android.app.permissionui.LocationButtonClient
import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Display
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.locationbutton.testing.TestLocationButtonProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.FileInputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that [LocationButton] opens a new session when its window moves to another display. The
 * window is moved with shell commands, so the test is skipped on a device that can't create an
 * overlay display or move the task to it.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
public class LocationButtonDisplayMoveTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /** The overlay displays setting before the test changed it, or `null` if it didn't. */
    private var previousOverlayDisplayDevices: String? = null

    @After
    public fun tearDown() {
        val previousOverlayDisplayDevices = previousOverlayDisplayDevices ?: return
        // The setting is "null" when it was never set, and empty when it was turned off.
        if (previousOverlayDisplayDevices.isEmpty() || previousOverlayDisplayDevices == "null") {
            runShellCommand("settings delete global overlay_display_devices")
        } else {
            runShellCommand(
                "settings put global overlay_display_devices $previousOverlayDisplayDevices"
            )
        }
    }

    @Test
    public fun testSessionFollowsWindowToAnotherDisplay() {
        val provider = SessionDisplayRecordingProvider(instrumentation.targetContext)
        ActivityScenario.launch(LocationButtonConfigChangesTestActivity::class.java).use { scenario
            ->
            lateinit var activity: Activity
            scenario.onActivity {
                activity = it
                val button = LocationButton(it).apply { setLocationButtonProvider(provider) }
                it.setContentView(FrameLayout(it).apply { addView(button) })
            }
            assertThat(
                    waitUntil { provider.openSessionDisplayIds == listOf(Display.DEFAULT_DISPLAY) }
                )
                .isTrue()

            val overlayDisplayId = createOverlayDisplay()
            assumeTrue("No overlay display", overlayDisplayId != Display.INVALID_DISPLAY)
            val moveOutput =
                runShellCommand("am display move-stack ${activity.taskId} $overlayDisplayId")
            assumeTrue(
                "The task didn't move to the overlay display: $moveOutput",
                waitUntil { windowDisplayIdOf(activity) == overlayDisplayId },
            )
            assumeTrue("The activity was recreated", isCurrentActivity(scenario, activity))

            assertThat(waitUntil { provider.openSessionDisplayIds == listOf(overlayDisplayId) })
                .isTrue()

            val moveBackOutput =
                runShellCommand(
                    "am display move-stack ${activity.taskId} ${Display.DEFAULT_DISPLAY}"
                )
            assumeTrue(
                "The task didn't move back to the default display: $moveBackOutput",
                waitUntil { windowDisplayIdOf(activity) == Display.DEFAULT_DISPLAY },
            )

            assertThat(
                    waitUntil { provider.openSessionDisplayIds == listOf(Display.DEFAULT_DISPLAY) }
                )
                .isTrue()
            assertThat(provider.openedDisplayIds)
                .containsExactly(Display.DEFAULT_DISPLAY, overlayDisplayId, Display.DEFAULT_DISPLAY)
                .inOrder()
            // Each session is set up with the density of the display it's opened for.
            val defaultDensityDpi = provider.openedDensityDpis.first()
            assertThat(provider.openedDensityDpis)
                .containsExactly(defaultDensityDpi, OVERLAY_DISPLAY_DENSITY_DPI, defaultDensityDpi)
                .inOrder()
        }
    }

    @Test
    public fun testSessionReopensOnAnotherDisplayAfterError() {
        val provider =
            SessionDisplayRecordingProvider(instrumentation.targetContext, failFirstSession = true)
        val errors = CopyOnWriteArrayList<Throwable>()
        ActivityScenario.launch(LocationButtonConfigChangesTestActivity::class.java).use { scenario
            ->
            lateinit var activity: Activity
            scenario.onActivity {
                activity = it
                val button =
                    LocationButton(it).apply {
                        setLocationButtonProvider(provider)
                        setOnErrorListener { error -> errors.add(error) }
                    }
                it.setContentView(FrameLayout(it).apply { addView(button) })
            }
            assertThat(waitUntil { errors.size == 1 }).isTrue()
            assertThat(provider.openSessionDisplayIds).isEmpty()

            val overlayDisplayId = createOverlayDisplay()
            assumeTrue("No overlay display", overlayDisplayId != Display.INVALID_DISPLAY)
            val moveOutput =
                runShellCommand("am display move-stack ${activity.taskId} $overlayDisplayId")
            assumeTrue(
                "The task didn't move to the overlay display: $moveOutput",
                waitUntil { windowDisplayIdOf(activity) == overlayDisplayId },
            )
            assumeTrue("The activity was recreated", isCurrentActivity(scenario, activity))

            assertThat(waitUntil { provider.openSessionDisplayIds == listOf(overlayDisplayId) })
                .isTrue()
        }
    }

    /** Creates an overlay display, and returns its ID or [Display.INVALID_DISPLAY]. */
    private fun createOverlayDisplay(): Int {
        val displayManager =
            instrumentation.targetContext.getSystemService(DisplayManager::class.java)
        val existingDisplayIds = displayManager.displays.map { it.displayId }.toSet()
        previousOverlayDisplayDevices =
            runShellCommand("settings get global overlay_display_devices").trim()
        runShellCommand("settings put global overlay_display_devices $OVERLAY_DISPLAY_DEVICES")
        var overlayDisplayId = Display.INVALID_DISPLAY
        waitUntil {
            overlayDisplayId =
                displayManager.displays
                    .map { it.displayId }
                    .firstOrNull {
                        it !in existingDisplayIds
                    } ?: Display.INVALID_DISPLAY
            overlayDisplayId != Display.INVALID_DISPLAY
        }
        return overlayDisplayId
    }

    private fun windowDisplayIdOf(activity: Activity): Int {
        var displayId = Display.INVALID_DISPLAY
        instrumentation.runOnMainSync {
            displayId = activity.window.decorView.display?.displayId ?: Display.INVALID_DISPLAY
        }
        return displayId
    }

    private fun isCurrentActivity(
        scenario: ActivityScenario<LocationButtonConfigChangesTestActivity>,
        activity: Activity,
    ): Boolean {
        var isCurrent = false
        scenario.onActivity { isCurrent = it === activity }
        return isCurrent
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) {
                return true
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }

    /** Runs a shell command and returns its output, including any errors it printed. */
    private fun runShellCommand(command: String): String {
        val (stdout, stdin, stderr) = instrumentation.uiAutomation.executeShellCommandRwe(command)
        stdin.close()
        val output = FileInputStream(stdout.fileDescriptor).use { String(it.readBytes()) }
        val errors = FileInputStream(stderr.fileDescriptor).use { String(it.readBytes()) }
        stdout.close()
        stderr.close()
        return output + errors
    }

    private companion object {
        private const val OVERLAY_DISPLAY_DENSITY_DPI = 320
        private const val OVERLAY_DISPLAY_DEVICES =
            "1080x1920/$OVERLAY_DISPLAY_DENSITY_DPI,fixed_content_mode"
        private const val TIMEOUT_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 100L
    }
}

/**
 * A [TestLocationButtonProvider] that records the displays that sessions are opened for, and can
 * fail the first session with an error.
 */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
private class SessionDisplayRecordingProvider(
    context: Context,
    private val failFirstSession: Boolean = false,
) : TestLocationButtonProvider(context) {
    /** The displays of every session that was requested, in order. */
    val openedDisplayIds = CopyOnWriteArrayList<Int>()

    /** The densities of every session that was requested, in order. */
    val openedDensityDpis = CopyOnWriteArrayList<Int>()

    /** The displays of the sessions that are open and not closed yet. */
    val openSessionDisplayIds = CopyOnWriteArrayList<Int>()

    override fun openSession(
        activity: Activity,
        hostToken: IBinder,
        displayId: Int,
        request: LocationButtonRequest,
        executor: Executor,
        client: LocationButtonClient,
    ) {
        openedDisplayIds.add(displayId)
        openedDensityDpis.add(request.configuration.densityDpi)
        if (failFirstSession && openedDisplayIds.size == 1) {
            executor.execute { client.onSessionError(IllegalStateException("First session")) }
            return
        }
        val recordingClient =
            object : LocationButtonClient {
                override fun onSessionOpened(session: LocationButtonSession) {
                    openSessionDisplayIds.add(displayId)
                    client.onSessionOpened(
                        object : LocationButtonSession by session {
                            override fun close() {
                                openSessionDisplayIds.remove(displayId)
                                session.close()
                            }
                        }
                    )
                }

                override fun onSessionError(throwable: Throwable) {
                    client.onSessionError(throwable)
                }

                override fun onPermissionResult(granted: Boolean) {
                    client.onPermissionResult(granted)
                }
            }
        super.openSession(activity, hostToken, displayId, request, executor, recordingClient)
    }
}
