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

package androidx.compose.integration.macrobenchmark

import android.app.Instrumentation
import android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
import android.content.Intent
import android.os.Build.VERSION_CODES.N
import android.provider.Settings.Secure
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SdkSuppress(minSdkVersion = N)
@LargeTest
@RunWith(Parameterized::class)
class FormFillingBenchmark(private var talkbackEnabled: Boolean, private val type: String) {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()
    private lateinit var instrumentation: Instrumentation
    private var previousTalkbackSettings: String? = null
    private lateinit var device: UiDevice

    @Test
    fun createAccessibilityNodeInfo() {
        if (!talkbackEnabled) return
        benchmarkRule.measureRepeated(
            packageName = PACKAGE,
            metrics =
                @OptIn(ExperimentalMetricApi::class)
                listOf(
                    TraceSectionMetric(
                        sectionName = CREATE_ANI_TRACE,
                        mode = TraceSectionMetric.Mode.Sum
                    ),
                    TraceSectionMetric(
                        sectionName = ACCESSIBILITY_EVENT_TRACE,
                        mode = TraceSectionMetric.Mode.Sum
                    )
                ),
            iterations = 10,
            setupBlock = {
                if (iteration == 0) {
                    startActivityAndWait(
                        Intent()
                            .setAction("$PACKAGE.$ACTIVITY")
                            .putExtra(TYPE, type)
                            .putExtra(MODE, CREATE_ANI_MODE)
                    )
                    device.waitForIdle()

                    // Run one iteration to allow the scroll position to stabilize, and to remove
                    // the effect of the initial frame which draws the accessibility focus box.
                    performScrollAndWait(millis = 10_000)
                }
            },
            measureBlock = {

                // Scroll and pause to allow all frames to complete, for the accessibility events
                // to be sent, for talkback to assign focus, and finally for talkback to trigger
                // createAccessibilityNodeInfo calls which is the thing we want to measure.
                performScrollAndWait(millis = 10_000)
            }
        )
    }

    @Test
    fun frameInfo() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = 10,
            setupBlock = {
                if (iteration == 0) {
                    startActivityAndWait(
                        Intent()
                            .setAction("$PACKAGE.$ACTIVITY")
                            .putExtra(TYPE, type)
                            .putExtra(MODE, FRAME_MEASUREMENT_MODE)
                    )
                    Thread.sleep(2_000)
                    device.waitForIdle()

                    // Run one iteration to allow the scroll position to stabilize, and to remove
                    // the effect of the initial frame which draws the accessibility focus box.
                    performScrollAndWait(millis = 20)
                }
            },
            measureBlock = {
                // Instead of using an animation to scroll (Where the number of frames triggered
                // is not deterministic, we attempt to scroll 100 times with an aim to scroll once
                // every frame deadline of 20ms.
                repeat(100) { performScrollAndWait(millis = 20) }
                Thread.sleep(10_000)
            }
        )
    }

    @Before
    fun setUp() {
        Configurator.getInstance().uiAutomationFlags = FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
        instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
        if (talkbackEnabled) {
            previousTalkbackSettings = instrumentation.enableTalkback()
            // Wait for talkback to turn on.
            Thread.sleep(2_000)
        }
    }

    @After
    fun tearDown() {
        if (talkbackEnabled) {
            instrumentation.disableTalkback(previousTalkbackSettings)
            // Wait for talkback to turn off.
            Thread.sleep(2_000)
        }
    }

    private fun performScrollAndWait(millis: Long) {
        // We don't use UI Automator to scroll because UI Automator itself is an accessibility
        // service, and this affects the benchmark. Instead we send an event to the activity that
        // requests it to scroll.
        instrumentation.context.startActivity(
            Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction("$PACKAGE.$ACTIVITY")
        )

        // Pause to allow all frames to complete, for the accessibility events to be sent,
        // for talkback to assign focus, and finally for talkback to trigger
        // createAccessibilityNodeInfo calls which is the thing we want to measure.
        Thread.sleep(millis)
    }

    companion object {
        private const val PACKAGE = "androidx.compose.integration.macrobenchmark.target"
        private const val ACTIVITY = "FORM_ACTIVITY"
        private const val TYPE = "TYPE"
        private const val COMPOSE = "Compose"
        private const val VIEW = "View"
        const val MODE = "MODE"
        const val CREATE_ANI_MODE = 1
        const val FRAME_MEASUREMENT_MODE = 2
        const val CREATE_ANI_TRACE = "createAccessibilityNodeInfo"
        const val ACCESSIBILITY_EVENT_TRACE = "sendAccessibilityEvent"

        // Manually set up LastPass on the device and use these parameters when running locally.
        // @Parameterized.Parameters(name = "LastPassEnabled=true, type={1}")
        // @JvmStatic
        // fun parameters() = mutableListOf<Array<Any>>().also {
        //    for (type in arrayOf(COMPOSE, VIEW)) {
        //        it.add(arrayOf(false, type))
        //    }
        // }

        @Parameterized.Parameters(name = "TalkbackEnabled={0}, type={1}")
        @JvmStatic
        fun parameters() =
            mutableListOf<Array<Any>>().also {
                for (talkbackEnabled in arrayOf(false, true)) {
                    for (type in arrayOf(COMPOSE, VIEW)) {
                        it.add(arrayOf(talkbackEnabled, type))
                    }
                }
            }
    }
}

private fun Instrumentation.enableTalkback(): String? {
    val talkback =
        "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
    val previousTalkbackSettings =
        Secure.getString(context.contentResolver, Secure.ENABLED_ACCESSIBILITY_SERVICES)
    UiDevice.getInstance(this)
        .executeShellCommand("settings put secure enabled_accessibility_services $talkback")
    return previousTalkbackSettings
}

private fun Instrumentation.disableTalkback(previousTalkbackSettings: String? = null): String {
    return UiDevice.getInstance(this)
        .executeShellCommand(
            if (previousTalkbackSettings == null || previousTalkbackSettings == "") {
                "settings delete secure enabled_accessibility_services"
            } else {
                "settings put secure enabled_accessibility_services $previousTalkbackSettings"
            }
        )
}
