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

package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // captureToImage
class ComposeViewContextResourceCacheTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    /**
     * Two [ComposeView]s share a [ComposeViewContext], one under a night-mode configuration
     * context. The drawable's day variant is a raster image and its night variant is a vector, so
     * reusing the day view's cached resource path in the night view would load the wrong variant.
     *
     * The views are stacked vertically so neither covers the other in the window capture. The night
     * context is a [ContextThemeWrapper] so [captureToImage] can find the Activity.
     */
    @Test
    fun sharedComposeViewContext_resolvesDrawablePerConfiguration() {
        lateinit var dayContext: ComposeViewContext
        lateinit var nightContext: ComposeViewContext
        lateinit var root: LinearLayout
        lateinit var nightContainer: FrameLayout
        rule.runOnUiThread {
            root = LinearLayout(rule.activity).apply { orientation = LinearLayout.VERTICAL }
            rule.activity.setContentView(root)
            nightContainer = FrameLayout(rule.activity.createNightModeContext())
            root.addView(nightContainer)
        }
        rule.waitForIdle()
        rule.runOnUiThread {
            dayContext = ComposeViewContext(root)
            nightContext = dayContext.copy(view = nightContainer)
        }

        // The day view resolves the drawable first, so it populates its resource cache first.
        rule.runOnUiThread {
            val dayView = ComposeView(rule.activity)
            dayView.setContent { DayNightImage("day") }
            dayView.createComposition(dayContext)
            root.addView(dayView, WRAP_CONTENT_PARAMS)
        }
        rule.waitForIdle()
        rule.runOnUiThread {
            val nightView = ComposeView(nightContainer.context)
            nightView.setContent { DayNightImage("night") }
            // Created before attach so the composition uses nightContext rather than the root's.
            nightView.createComposition(nightContext)
            nightContainer.addView(nightView, WRAP_CONTENT_PARAMS)
        }
        rule.waitForIdle()

        assertThat(rule.onNodeWithTag("day").centerPixel()).isEqualTo(Color.Red)
        assertThat(rule.onNodeWithTag("night").centerPixel()).isEqualTo(Color.Blue)
    }

    @Composable
    private fun DayNightImage(tag: String) {
        Image(
            painter = painterResource(R.drawable.test_day_raster_night_vector),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.size(20.dp).testTag(tag),
        )
    }

    private fun SemanticsNodeInteraction.centerPixel(): Color {
        val pixels = captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private fun Context.createNightModeContext(): Context {
        val configuration = Configuration(resources.configuration)
        configuration.uiMode =
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                Configuration.UI_MODE_NIGHT_YES
        return ContextThemeWrapper(this, 0).apply { applyOverrideConfiguration(configuration) }
    }

    private companion object {
        val WRAP_CONTENT_PARAMS =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }
}
