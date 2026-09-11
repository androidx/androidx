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

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.util.TestProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.core.platform.AndroidComponentSupport
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import java.io.ByteArrayInputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end connected screenshot test verifying that custom components created via
 * [androidx.compose.remote.creation.compose.capture.RecordingCanvas.custom] render correctly
 * through the Java player ([androidx.compose.remote.player.view.RemoteComposePlayer]).
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RecordingCanvasCustomComponentScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    private fun getTests(): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        listOf(
            "triangle_red" to
                @Composable @RemoteComposable {
                    CustomShapeComponent(
                        color = Color.Red.rc,
                        edges = 3.ri,
                    )
                },
            "diamond_green" to
                @Composable @RemoteComposable {
                    CustomShapeComponent(
                        color = Color(0xFF2E7D32).rc,
                        edges = 4.ri,
                    )
                },
            "pentagon_blue" to
                @Composable @RemoteComposable {
                    CustomShapeComponent(
                        color = Color(0xFF1565C0).rc,
                        edges = 5.ri,
                    )
                },
            "hexagon_magenta" to
                @Composable @RemoteComposable {
                    CustomShapeComponent(
                        color = Color(0xFFAD1457).rc,
                        edges = 6.ri,
                    )
                },
            "octagon_orange_expr" to
                @Composable @RemoteComposable {
                    // Verify expression hoisting with dynamic RemoteInt
                    CustomShapeComponent(
                        color = Color(0xFFEF6C00).rc,
                        edges = 5.ri + 3.ri,
                    )
                },
            "circle_cyan" to
                @Composable @RemoteComposable {
                    CustomShapeComponent(
                        color = Color(0xFF00838F).rc,
                        edges = 0.ri,
                    )
                },
        )

    @Test
    fun grid() {
        val customSupport =
            AndroidCustomContextImpl(
                initialDelegates = mapOf("ShapeComponent" to ShapeComponentSupport())
            )
        composeTestRule.runScreenshotTest(
            profile = TestProfiles.androidXExperimental,
            customSupport = customSupport,
        ) {
            gridScreenshotUI.GridContent(getTests())
        }
    }

    @Test
    fun grid_captureSingleRemoteDocument() = runTest {
        val customSupport =
            AndroidCustomContextImpl(
                initialDelegates = mapOf("ShapeComponent" to ShapeComponentSupport())
            )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val creationDisplayInfo = composeTestRule.remoteCreationDisplayInfo
        val profile = TestProfiles.androidXExperimental

        val captured =
            captureSingleRemoteDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
            ) {
                gridScreenshotUI.GridContent(getTests())
            }

        val doc =
            CoreDocument().apply {
                ByteArrayInputStream(captured.bytes).use {
                    val buffer = RemoteComposeBuffer.fromInputStream(it)
                    buffer.setVersion(
                        profile.apiLevel,
                        profile.operationsProfiles,
                        profile.supportedOperations,
                    )
                    initFromBuffer(buffer)
                }
            }

        composeTestRule.composeTestRule.setContent {
            Box(
                Modifier.requiredSize(creationDisplayInfo.widthDp, creationDisplayInfo.heightDp)
                    .testTag(RemoteScreenshotTestRule.ROOT_TEST_TAG)
            ) {
                RemoteDocumentPlayer(
                    document = doc,
                    documentWidth = creationDisplayInfo.size.width.toInt(),
                    documentHeight = creationDisplayInfo.size.height.toInt(),
                    customSupport = customSupport,
                )
            }
        }

        composeTestRule.verifyScreenshot()
    }

    @Composable
    @RemoteComposable
    private fun CustomShapeComponent(
        color: RemoteColor,
        edges: RemoteInt,
        modifier: RemoteModifier = RemoteModifier.size(80.rdp, 80.rdp),
    ) {
        RemoteCustomComponent(
            name = "ShapeComponent",
            modifier = modifier,
        ) {
            property(ShapeComponentSupport.PROP_COLOR, color)
            property(ShapeComponentSupport.PROP_EDGES, edges)
        }
    }

    private class ShapeCustomView(context: Context) : View(context) {
        var fillColor: Int = AndroidColor.BLACK
        var edges: Int = 3

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(width, height)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = fillColor
            paint.style = Paint.Style.FILL

            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val radius = min(cx, cy) * 0.85f

            path.reset()
            if (edges <= 2) {
                canvas.drawCircle(cx, cy, radius, paint)
            } else {
                val angleStep = (2.0 * PI / edges)
                for (i in 0 until edges) {
                    val angle = i * angleStep - PI / 2.0
                    val x = (cx + radius * cos(angle)).toFloat()
                    val y = (cy + radius * sin(angle)).toFloat()
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    private class ShapeComponentSupport : AndroidComponentSupport {
        companion object {
            const val PROP_COLOR: Int = 0
            const val PROP_EDGES: Int = 1
        }

        override fun createView(context: Context): View {
            return ShapeCustomView(context).apply {
                layoutParams = FrameLayout.LayoutParams(0, 0)
            }
        }

        override fun configure(view: View, type: Int, value: String) {}

        override fun configure(view: View, type: Int, value: Int) {
            if (view is ShapeCustomView) {
                when (type) {
                    PROP_COLOR -> {
                        view.fillColor = value
                        view.invalidate()
                    }
                    PROP_EDGES -> {
                        view.edges = value
                        view.invalidate()
                    }
                }
            }
        }

        override fun configure(view: View, type: Int, value: Float) {}
    }
}
