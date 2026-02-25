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
package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBitmap
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class Material3ImageTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun avatarImage_circleShape() {
        val size = 48
        val sizeDp = size.rdp
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(size, size, context.resources.displayMetrics.densityDpi)
        ) {
            val avatarImage =
                rememberNamedRemoteBitmap(name = "avatarImage") {
                    createImage(size, size).asImageBitmap()
                }
            RemoteAvatarImage(
                avatarImage,
                contentDescription = "background".rs,
                RemoteModifier.size(sizeDp),
            )
        }
    }

    @Test
    fun backgroundImage_roundedShapeAndHasOverlay() {
        val size = 227
        val sizeDp = size.rdp
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(size, size, context.resources.displayMetrics.densityDpi)
        ) {
            val backgroundImage =
                rememberNamedRemoteBitmap(name = "backgroundImage") {
                    createImage(size, size).asImageBitmap()
                }
            RemoteBackgroundImage(
                background = backgroundImage,
                contentDescription = "background".rs,
                modifier = RemoteModifier.size(sizeDp),
                overlayColor = rememberNamedRemoteColor("overlay", Color.Yellow.copy(alpha = 0.6f)),
            )
        }
    }

    internal companion object {
        // Draws a red cross with a blue background
        fun createImage(tw: Int, th: Int): Bitmap {
            val image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            image.eraseColor(android.graphics.Color.BLUE)
            val paint = Paint()
            val canvas = Canvas(image)
            paint.strokeWidth = 3f
            paint.isAntiAlias = true
            paint.setColor(android.graphics.Color.RED)
            canvas.drawLine(0f, 0f, tw.toFloat(), th.toFloat(), paint)
            canvas.drawLine(0f, th.toFloat(), tw.toFloat(), 0f, paint)
            return image
        }
    }
}
