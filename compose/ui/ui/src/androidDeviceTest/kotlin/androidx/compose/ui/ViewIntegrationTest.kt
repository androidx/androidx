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

package androidx.compose.ui

import android.graphics.Bitmap
import android.os.Build
import android.transition.TransitionManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewIntegrationTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private lateinit var activity: TestActivity
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        density = Density(activity)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawDetachedLayoutNode() {
        lateinit var view: ComposeView
        rule.runOnUiThread {
            view = ComposeView(activity)
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity)
            )
            view.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.background(Color.Blue)
                            .requiredSize(30.toDp())
                            .padding(10.toDp())
                            .background(Color.White)
                    )
                }
            }
            activity.setContentView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        rule.runOnIdle {
            val parent = view.parent as ViewGroup
            parent.removeView(view)
        }
        rule.runOnIdle {
            val bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            view.draw(canvas)
            bitmap.assertRect(Color.Blue, holeSize = 10)
            bitmap.assertRect(Color.White, size = 10)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawInvalidationInDetachedLayoutNode() {
        lateinit var view: ComposeView
        var innerColor by mutableStateOf(Color.White)
        rule.runOnUiThread {
            view = ComposeView(activity)
            view.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.background(Color.Blue)
                            .requiredSize(30.toDp())
                            .padding(10.toDp())
                            .drawBehind { drawRect(innerColor) }
                    )
                }
            }
            activity.setContentView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        validateSquareColors(Color.Blue, Color.White, size = 10)

        var parent: ViewGroup? = null
        rule.runOnIdle {
            parent = view.parent as ViewGroup
            parent.removeView(view)
        }
        rule.waitForIdle() // wait for detach

        innerColor = Color.Yellow

        rule.runOnIdle { parent!!.addView(view) }

        validateSquareColors(Color.Blue, Color.Yellow, size = 10)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun sizeInvalidationInDetachedLayoutNode() {
        lateinit var view: ComposeView
        var size by mutableStateOf(10.dp)
        var measuredSize = 0.dp
        val sizeModifier =
            Modifier.layout { measurable, constraints ->
                measuredSize = size
                val pxSize = size.roundToPx()
                layout(pxSize, pxSize) { measurable.measure(constraints).place(0, 0) }
            }
        rule.runOnUiThread {
            view = ComposeView(activity)
            view.setContent { Box(Modifier.background(Color.Blue).then(sizeModifier)) }
            activity.setContentView(view)
        }

        rule.waitForIdle()
        assertEquals(10.dp, measuredSize)

        var parent: ViewGroup? = null
        rule.runOnUiThread {
            parent = view.parent as ViewGroup
            parent.removeView(view)
        }
        rule.waitForIdle()

        size = 30.dp

        rule.runOnUiThread { parent!!.addView(view) }

        rule.waitForIdle()
        assertEquals(measuredSize, 30.dp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun zeroSizedComposeViewCanDrawOutsideItsBounds() {
        val padding = 10
        val size = padding * 2

        lateinit var frameLayout: FrameLayout

        rule.runOnUiThread {
            val composeView = ComposeView(activity)
            composeView.setContent {
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val marginFloat = padding.toFloat()
                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(-marginFloat, -marginFloat),
                            size = Size(marginFloat * 2, marginFloat * 2),
                        )
                    }
                )
            }
            frameLayout = FrameLayout(activity)
            frameLayout.clipToPadding = false
            frameLayout.clipChildren = false
            frameLayout.setPadding(padding, padding, padding, padding)
            frameLayout.addView(composeView, ViewGroup.LayoutParams(0, 0))
            activity.setContentView(
                frameLayout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        rule.waitAndScreenShot(frameLayout).asImageBitmap().assertPixels(
            expectedSize = IntSize(size, size)
        ) {
            Color.Red
        }
    }

    @Test
    fun worksWithTransitions() {
        val frameLayout = FrameLayout(activity)
        rule.runOnUiThread {
            activity.setContentView(frameLayout)
            val composeView = ComposeView(activity).apply { setContent { Box {} } }
            frameLayout.addView(composeView)
        }

        rule.runOnUiThread {
            TransitionManager.beginDelayedTransition(frameLayout)
            frameLayout.removeAllViews()
            val composeView = ComposeView(activity).apply { setContent { Box {} } }
            frameLayout.addView(composeView)
        }

        rule.waitForIdle()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun validateSquareColors(
        outerColor: Color,
        innerColor: Color,
        size: Int,
        offset: Int = 0,
        totalSize: Int = size * 3,
    ) {
        rule.validateSquareColors(outerColor, innerColor, size, offset, totalSize)
    }
}
