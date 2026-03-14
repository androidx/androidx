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

package androidx.compose.remote.creation.compose.capture.shapes

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteShapeTest {
    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }
    private val context: Context = ApplicationProvider.getApplicationContext()

    val size = Size(500f, 500f)
    private val creationDisplayInfo =
        CreationDisplayInfo(
            size.width.toInt(),
            size.height.toInt(),
            context.resources.displayMetrics.densityDpi,
        )

    @Test
    fun circleShape() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            DrawRemoteShape(RemoteCircleShape)
        }
    }

    @Test
    fun roundedUniformPercentCorners() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val rounded = RemoteRoundedCornerShape(25)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun roundedUniformRemoteDpCorners() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val rounded = RemoteRoundedCornerShape(25.rdp)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun roundedUniformPxCorners() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val rounded = RemoteRoundedCornerShape(25f.rf)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun roundedDifferentRemoteDpRadius() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val topStart = 12.rdp
            val topEnd = 22.rdp
            val bottomEnd = 32.rdp
            val bottomStart = 42.rdp
            val rounded = RemoteRoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun roundedDifferentPercentRadius() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val topStart = 50
            val topEnd = 25
            val bottomEnd = 25
            val bottomStart = 50
            val rounded = RemoteRoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun roundedDifferentPercentRadiusRTL() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            val topStart = 50
            val topEnd = 25
            val bottomEnd = 25
            val bottomStart = 50
            val rounded = RemoteRoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun roundedDifferentPxRadius() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val topStart = 12f.rf
            val topEnd = 22f.rf
            val bottomEnd = 32f.rf
            val bottomStart = 42f.rf
            val rounded = RemoteRoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
            DrawRemoteShape(rounded)
        }
    }

    @Test
    fun zeroSizedCorners() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            val rounded = RemoteRoundedCornerShape(0f.rf)
            DrawRemoteShape(rounded)
        }
    }

    @Composable
    @RemoteComposable
    private fun DrawRemoteShape(shape: RemoteShape) {
        RemoteBox(
            contentAlignment = RemoteAlignment.Center,
            modifier = RemoteModifier.width(200.rdp).height(200.rdp),
        ) {
            RemoteCanvas(RemoteModifier.width(100.rdp).height(100.rdp)) {
                val w = width
                val h = height
                val size = RemoteSize(w, h)
                val paint = RemotePaint { color = androidx.compose.ui.graphics.Color.Red.rc }
                with(shape.createOutline(size, remoteDensity, layoutDirection)) {
                    drawOutline(paint)
                }
            }
        }
    }
}
