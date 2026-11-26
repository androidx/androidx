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

package androidx.xr.projected.integration.tests

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.testutils.ProjectedScreenshotTestRule
import androidx.xr.testutils.assertAgainstGolden
import androidx.xr.testutils.captureProjectedDisplayToBitmap
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * End-to-end golden screenshot tests verifying UI rendering on a paired Projected display using
 * [ProjectedScreenshotTestRule] and [captureProjectedDisplayToBitmap].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalProjectedApi::class)
class ProjectedDisplayScreenshotTest {

    @get:Rule(order = 0) val screenshotRule = ProjectedScreenshotTestRule(GOLDEN_PATH)

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(60)

    @Test
    fun projectedActivity_rendersOnProjectedDisplay_matchesGolden() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val projectedOptions = ProjectedContext.createProjectedActivityOptions(targetContext)
        val intent = Intent(targetContext, ProjectedDisplayTestActivity::class.java)
        ActivityScenario.launch<ProjectedDisplayTestActivity>(intent, projectedOptions.toBundle())
            .use { scenario ->
                var expectedWidth = 0
                var expectedHeight = 0
                scenario.onActivity { activity ->
                    val bounds = activity.windowManager.currentWindowMetrics.bounds
                    expectedWidth = bounds.width()
                    expectedHeight = bounds.height()
                    activity.setContentView(createProjectedCardView(activity))
                }

                val screenshot = captureProjectedDisplayToBitmap()

                try {
                    assertThat(screenshot.width).isEqualTo(expectedWidth)
                    assertThat(screenshot.height).isEqualTo(expectedHeight)
                    screenshot.assertAgainstGolden(screenshotRule, "projected_display_basic")
                } finally {
                    screenshot.recycle()
                }
            }
    }

    private fun createProjectedCardView(activity: ProjectedDisplayTestActivity): View {
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val root =
            FrameLayout(activity).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        val cardBackground =
            GradientDrawable().apply {
                setColor(Color.rgb(30, 32, 40))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(2), Color.rgb(66, 133, 244))
            }

        val card =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                background = cardBackground
                setPadding(dp(24), dp(20), dp(24), dp(20))
                layoutParams =
                    FrameLayout.LayoutParams(dp(340), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    }
            }

        val accentBar =
            View(activity).apply {
                setBackgroundColor(Color.rgb(52, 168, 83))
                layoutParams =
                    LinearLayout.LayoutParams(dp(120), dp(8)).apply { bottomMargin = dp(16) }
            }

        val title =
            TextView(activity).apply {
                text = "Projected Display"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

        val subtitle =
            TextView(activity).apply {
                text = "AndroidX XR Screenshot Test"
                setTextColor(Color.rgb(180, 210, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                typeface = Typeface.SANS_SERIF
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        .apply { topMargin = dp(8) }
            }

        card.addView(accentBar)
        card.addView(title)
        card.addView(subtitle)
        root.addView(card)
        return root
    }

    private companion object {
        const val GOLDEN_PATH = "xr/projected/integration-tests/xr-integration-tests"
    }
}
