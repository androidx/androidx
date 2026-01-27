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

package androidx.compose.remote.creation.compose.vector

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.R
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
class RemoteVectorPainterTest {
    @TestParameter private lateinit var targetPlayer: TargetPlayer

    @get:Rule
    val remoteComposeTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = targetPlayer,
        )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun fromImageVector() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(48, 48, context.resources.displayMetrics.densityDpi),
            backgroundColor = Color.White,
        ) {
            LoadFromImageVector(
                imageVector = TestImageVectors.VolumeUp,
                modifier = RemoteModifier.size(48.rdp),
            )
        }
    }

    @Test
    fun fromRes() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(48, 48, context.resources.displayMetrics.densityDpi),
            backgroundColor = Color.White,
        ) {
            LoadFromRes(res = R.drawable.android, modifier = RemoteModifier.size(48.rdp))
        }
    }

    @Test
    fun fromRemoteImageVector() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(48, 48, context.resources.displayMetrics.densityDpi),
            backgroundColor = Color.White,
        ) {
            LoadFromRemoteImageVector(
                imageVector = TestImageVectors.RemoteVolumeUp,
                modifier = RemoteModifier.size(48.rdp),
            )
        }
    }

    @Test
    fun tinted_fromImageVector() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(48, 48, context.resources.displayMetrics.densityDpi),
            backgroundColor = Color.White,
        ) {
            LoadFromImageVector(
                imageVector = TestImageVectors.VolumeUp,
                modifier = RemoteModifier.size(48.rdp),
                tint = RemoteColor(Color.Red),
            )
        }
    }

    @Test
    fun tinted_fromRes() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(48, 48, context.resources.displayMetrics.densityDpi),
            backgroundColor = Color.White,
        ) {
            LoadFromRes(
                res = R.drawable.android,
                modifier = RemoteModifier.size(48.rdp),
                tint = RemoteColor(Color.Red),
            )
        }
    }

    @Test
    fun tinted_fromRemoteImageVector() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(48, 48, context.resources.displayMetrics.densityDpi),
            backgroundColor = Color.White,
        ) {
            LoadFromRemoteImageVector(
                imageVector = TestImageVectors.RemoteVolumeUp,
                modifier = RemoteModifier.size(48.rdp),
                tint = RemoteColor(Color.Red),
            )
        }
    }
}

@RemoteComposable
@Composable
private fun LoadFromImageVector(
    imageVector: ImageVector,
    modifier: RemoteModifier = RemoteModifier.size(size),
    tint: RemoteColor = RemoteColor(color),
) {
    RemoteBox(modifier) {
        val painter =
            painterRemoteVector(
                imageVector,
                tint,
                LocalRemoteComposeCreationState.current.remoteDensity,
            )
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { with(painter) { onDraw() } }
    }
}

@RemoteComposable
@Composable
private fun LoadFromRes(
    res: Int,
    modifier: RemoteModifier = RemoteModifier.size(size),
    tint: RemoteColor = RemoteColor(color),
) {
    RemoteBox(modifier) {
        val painter =
            painterRemoteVector(
                ImageVector.vectorResource(res),
                tint,
                LocalRemoteComposeCreationState.current.remoteDensity,
            )
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { with(painter) { onDraw() } }
    }
}

@RemoteComposable
@Composable
private fun LoadFromRemoteImageVector(
    imageVector: RemoteImageVector,
    modifier: RemoteModifier = RemoteModifier.size(size),
    tint: RemoteColor = RemoteColor(color),
) {
    RemoteBox(modifier) {
        val painter = painterRemoteVector(imageVector, tint)
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { with(painter) { onDraw() } }
    }
}

private val size = 48.rdp
private val color = Color.Black

private object TestImageVectors {

    val VolumeUp: ImageVector =
        ImageVector.Builder(
                name = "Volume up",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
                tintColor = Color.Black,
            )
            .addPath(
                PathData {
                    moveTo(3.0f, 9.0f)
                    verticalLineToRelative(6.0f)
                    horizontalLineToRelative(4.0f)
                    lineToRelative(5.0f, 5.0f)
                    lineTo(12.0f, 4.0f)
                    lineTo(7.0f, 9.0f)
                    lineTo(3.0f, 9.0f)
                    close()
                    moveTo(16.5f, 12.0f)
                    curveToRelative(0.0f, (-1.77f), (-1.02f), (-3.29f), (-2.5f), (-4.03f))
                    verticalLineToRelative(8.05f)
                    curveToRelative(1.48f, (-0.73f), 2.5f, (-2.25f), 2.5f, (-4.02f))
                    close()
                    moveTo(14.0f, 3.23f)
                    verticalLineToRelative(2.06f)
                    curveToRelative(2.89f, 0.86f, 5.0f, 3.54f, 5.0f, 6.71f)
                    reflectiveCurveToRelative((-2.11f), 5.85f, (-5.0f), 6.71f)
                    verticalLineToRelative(2.06f)
                    curveToRelative(4.01f, (-0.91f), 7.0f, (-4.49f), 7.0f, (-8.77f))
                    reflectiveCurveToRelative((-2.99f), (-7.86f), (-7.0f), (-8.77f))
                    close()
                },
                fillAlpha = 1f,
                strokeAlpha = 1f,
                fill = SolidColor(Color.Black),
            )
            .build()

    val testRemoteStateScope = NoRemoteCompose()

    val RemoteVolumeUp =
        RemoteImageVector.Builder(
                testRemoteStateScope,
                name = "Volume up",
                viewportWidth = 24.0f.rf,
                viewportHeight = 24.0f.rf,
                tintColor = RemoteColor(Color.Black),
            )
            .addPath(
                RemotePathData(testRemoteStateScope) {
                    moveTo(3.0f.rf, 9.0f.rf)
                    verticalLineToRelative(6.0f.rf)
                    horizontalLineToRelative(4.0f.rf)
                    lineToRelative(5.0f.rf, 5.0f.rf)
                    lineTo(12.0f.rf, 4.0f.rf)
                    lineTo(7.0f.rf, 9.0f.rf)
                    lineTo(3.0f.rf, 9.0f.rf)
                    close()
                    moveTo(16.5f.rf, 12.0f.rf)
                    curveToRelative(
                        0.0f.rf,
                        (-1.77f).rf,
                        (-1.02f).rf,
                        (-3.29f).rf,
                        (-2.5f).rf,
                        (-4.03f).rf,
                    )
                    verticalLineToRelative(8.05f.rf)
                    curveToRelative(
                        1.48f.rf,
                        (-0.73f).rf,
                        2.5f.rf,
                        (-2.25f).rf,
                        2.5f.rf,
                        (-4.02f).rf,
                    )
                    close()
                    moveTo(14.0f.rf, 3.23f.rf)
                    verticalLineToRelative(2.06f.rf)
                    curveToRelative(2.89f.rf, 0.86f.rf, 5.0f.rf, 3.54f.rf, 5.0f.rf, 6.71f.rf)
                    reflectiveCurveToRelative((-2.11f).rf, 5.85f.rf, (-5.0f).rf, 6.71f.rf)
                    verticalLineToRelative(2.06f.rf)
                    curveToRelative(
                        4.01f.rf,
                        (-0.91f).rf,
                        7.0f.rf,
                        (-4.49f).rf,
                        7.0f.rf,
                        (-8.77f).rf,
                    )
                    reflectiveCurveToRelative((-2.99f).rf, (-7.86f).rf, (-7.0f).rf, (-8.77f).rf)
                    close()
                },
                fill = SolidColor(Color.Black),
            )
            .build()
}
