/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.ui.layout

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertRect
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.padding
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class ShowLayoutBoundsTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private fun setIsShowingLayoutBounds(value: Boolean) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("setprop debug.layout $value")
        // 1599295570 is SYSPROPS_TRANSACTION -- used to poke the system properties
        // to notify that a change happened
        uiAutomation.executeShellCommand("service call activity 1599295570")
        rule.waitForIdle()
    }

    @Before
    fun setup() {
        setIsShowingLayoutBounds(true)
    }

    @After
    fun tearDown() {
        setIsShowingLayoutBounds(false)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun showLayoutBoundsNotifiedWhenChanged() {
        lateinit var view: AndroidComposeView
        rule.setContent {
            view = LocalView.current as AndroidComposeView
            Box(Modifier.fillMaxSize())
        }
        val latch = CountDownLatch(1)
        rule.runOnIdle {
            assertThat(view.showLayoutBounds).isTrue()
            view.viewTreeObserver.addOnPreDrawListener {
                latch.countDown()
                true
            }
        }
        setIsShowingLayoutBounds(false)
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        rule.runOnIdle { assertThat(view.showLayoutBounds).isFalse() }
    }

    // Tests that show layout bounds draws outlines around content and modifiers
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun showLayoutBounds_content() {
        rule.setContent {
            // Need to offset the content from the outer View
            FixedSize(size = 40, modifier = Modifier.background(Color.White)) {
                FixedSize(size = 30, modifier = Modifier.background(Color.White)) {
                    FixedSize(size = 10, modifier = Modifier.padding(5).padding(5))
                }
            }
        }
        rule.waitForIdle()

        takeScreenShot(40).apply {
            assertRect(Color.White, size = 8)
            assertRect(Color.Red, size = 10, holeSize = 8)
            assertRect(Color.White, size = 18, holeSize = 10)
            assertRect(Color.Blue, size = 20, holeSize = 18)
            assertRect(Color.White, size = 28, holeSize = 20)
            assertRect(Color.Red, size = 30, holeSize = 28)
        }
    }

    // Ensure that showLayoutBounds is reset in onResume() to whatever is set in the
    // settings.
    @Test
    fun showLayoutBounds_resetOnResume() {
        lateinit var composeView: AndroidComposeView
        rule.setContent { composeView = LocalView.current as AndroidComposeView }

        val activity = rule.activity

        rule.runOnUiThread {
            val intent = Intent(activity, TestActivity::class.java)
            activity.startActivity(intent)
        }

        assertThat(activity.stopLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // change showLayoutBounds to true without poking
        setIsShowingLayoutBounds(true)

        rule.runOnUiThread {
            activity.resumeLatch = CountDownLatch(1)
            TestActivity.resumedActivity!!.finish()
        }

        assertThat(activity.resumeLatch.await(5, TimeUnit.SECONDS)).isTrue()

        rule.runOnIdle {
            // ensure showLayoutBounds was reset in onResume()
            assertThat(composeView.showLayoutBounds).isTrue()
        }
    }

    // waitAndScreenShot() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(width: Int, height: Int = width): Bitmap {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        assertThat(bitmap.width).isEqualTo(width)
        assertThat(bitmap.height).isEqualTo(height)
        return bitmap
    }
}
