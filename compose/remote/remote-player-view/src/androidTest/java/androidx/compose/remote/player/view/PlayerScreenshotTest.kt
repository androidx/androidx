/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.player.core.RemoteComposeDocument
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.view.TestUtils.createDocument
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [RemoteComposePlayer]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerScreenshotTest {
    private val SCREENSHOT_GOLDEN_DIRECTORY = "compose/remote/remote-player-view"

    @get:Rule val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private lateinit var playerView: RemoteComposePlayer

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity {
            val frameLayout = FrameLayout(it)
            frameLayout.layoutParams =
                FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                )
            playerView =
                RemoteComposePlayer(it).apply {
                    layoutParams = FrameLayout.LayoutParams(200, 200, Gravity.CENTER)
                }
            frameLayout.addView(playerView)
            it.setContentView(frameLayout)
        }
    }

    @Test
    fun showCircle() {
        val androidContext = AndroidRemoteContext()

        val remoteComposeDocument: RemoteComposeDocument =
            createDocument(androidContext) { rcDoc ->
                rcDoc.root {
                    rcDoc.box(
                        RecordingModifier().fillMaxSize().background(Color.RED),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        rcDoc.painter.setColor(Color.BLUE).commit()
                        rcDoc.drawCircle(0f, 0f, 100f)
                    }
                }
            }

        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("circle")
    }

    fun assertScreenshot(filename: String) {
        onView(ViewMatchers.withClassName(Matchers.containsString("RemoteComposePlayer")))
            .perform(
                captureToBitmap {
                    it.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$filename")
                }
            )
    }
}
