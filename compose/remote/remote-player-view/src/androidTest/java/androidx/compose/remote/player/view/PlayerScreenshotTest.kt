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

import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.player.core.RemoteDocument
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

        val remoteComposeDocument: RemoteDocument =
            createDocument(androidContext) { rcDoc ->
                rcDoc.root {
                    rcDoc.box(
                        RecordingModifier().fillMaxSize().background(Color.RED),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        rcDoc.canvas(RecordingModifier().fillMaxSize()) {
                            rcDoc.painter.setColor(Color.BLUE).commit()
                            rcDoc.drawCircle(100f, 100f, 100f)
                        }
                    }
                }
            }

        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("circle")
    }

    @Test
    fun showUrlBitmapWithCorrectSize() {
        val androidContext = AndroidRemoteContext()
        val bitmapWidth = 100
        val bitmapHeight = 50
        val bitmap = TestUtils.createImage(bitmapWidth, bitmapHeight, false)
        val url = "https://example.com/test.png"

        activityScenarioRule.scenario.onActivity {
            playerView.setBitmapLoader { requestedUrl ->
                if (requestedUrl == url) {
                    val os = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    ByteArrayInputStream(os.toByteArray())
                } else {
                    throw java.io.IOException("Unknown URL: $requestedUrl")
                }
            }
        }

        val remoteComposeDocument: RemoteDocument =
            createDocument(androidContext) { rcDoc ->
                rcDoc.root {
                    val imageId = rcDoc.writer.addBitmapUrl(url, bitmapWidth, bitmapHeight)
                    rcDoc.image(
                        RecordingModifier().fillMaxSize(),
                        imageId,
                        ImageScaling.SCALE_FIT,
                        1f,
                    )
                }
            }

        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("url_bitmap_correct_size")
    }

    @Test
    fun showEllipses() {
        val androidContext = AndroidRemoteContext()
        playerView.layoutParams = FrameLayout.LayoutParams(600, 600, Gravity.CENTER)
        val remoteComposeDocument: RemoteDocument =
            createDocument(
                androidContext,
                7,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            ) { rcDoc ->
                rcDoc.root {
                    rcDoc.column(RecordingModifier().background(Color.YELLOW).fillMaxSize()) {
                        val fontName = "DancingScript-Regular"
                        val content2 = "The quick brown fox jumps over the lazy dog"
                        rcDoc.text(
                            content2,
                            RecordingModifier().background(Color.LTGRAY).fillMaxWidth(),
                            fontFamily = fontName,
                            color = Color.RED,
                            fontSize = 80f,
                            overflow = CoreText.OVERFLOW_ELLIPSIS,
                            maxLines = 1,
                        )
                        rcDoc.text(
                            content2,
                            RecordingModifier().background(Color.LTGRAY).fillMaxWidth(),
                            fontFamily = fontName,
                            color = Color.GREEN,
                            fontSize = 80f,
                            overflow = CoreText.OVERFLOW_MIDDLE_ELLIPSIS,
                            maxLines = 1,
                        )
                        rcDoc.text(
                            content2,
                            RecordingModifier().background(Color.LTGRAY).fillMaxWidth(),
                            fontFamily = fontName,
                            color = Color.BLUE,
                            fontSize = 80f,
                            overflow = CoreText.OVERFLOW_START_ELLIPSIS,
                            maxLines = 1,
                        )
                    }
                }
            }
        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("ellipses")
    }

    @Test
    fun showAutosize1() {
        val androidContext = AndroidRemoteContext()
        playerView.layoutParams = FrameLayout.LayoutParams(600, 600, Gravity.CENTER)
        val remoteComposeDocument: RemoteDocument =
            createDocument(
                androidContext,
                7,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            ) { rcDoc ->
                rcDoc.root {
                    rcDoc.column(RecordingModifier().background(Color.YELLOW).fillMaxSize()) {
                        val content = "The quick brown fox jumps over the lazy dog"
                        rcDoc.text(
                            content,
                            RecordingModifier().background(Color.LTGRAY).size(400),
                            color = Color.BLUE,
                            autosize = true,
                        )
                    }
                }
            }
        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("autosize1")
    }

    @Test
    fun showAutosize2() {
        val androidContext = AndroidRemoteContext()
        playerView.layoutParams = FrameLayout.LayoutParams(600, 600, Gravity.CENTER)
        val remoteComposeDocument: RemoteDocument =
            createDocument(
                androidContext,
                7,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            ) { rcDoc ->
                rcDoc.root {
                    rcDoc.column(RecordingModifier().background(Color.YELLOW).fillMaxSize()) {
                        val content = "The quick brown fox jumps over the lazy dog"
                        rcDoc.text(
                            content,
                            RecordingModifier().background(Color.LTGRAY).size(200),
                            color = Color.BLUE,
                            minFontSize = 60f,
                            autosize = true,
                        )
                    }
                }
            }
        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("autosize2")
    }

    @Test
    fun showAutosize3() {
        val androidContext = AndroidRemoteContext()
        playerView.layoutParams = FrameLayout.LayoutParams(600, 600, Gravity.CENTER)
        val remoteComposeDocument: RemoteDocument =
            createDocument(
                androidContext,
                7,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            ) { rcDoc ->
                rcDoc.root {
                    rcDoc.column(RecordingModifier().background(Color.YELLOW).fillMaxSize()) {
                        val content = "The quick brown fox jumps over the lazy dog"
                        rcDoc.text(
                            content,
                            RecordingModifier().background(Color.LTGRAY).size(200),
                            color = Color.BLUE,
                            minFontSize = 60f,
                            autosize = true,
                            overflow = CoreText.OVERFLOW_ELLIPSIS,
                        )
                    }
                }
            }
        activityScenarioRule.scenario.onActivity { playerView.setDocument(remoteComposeDocument) }

        assertScreenshot("autosize3")
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
