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
package androidx.compose.remote.wear.material3

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rememberRemoteBitmapValue
import androidx.compose.remote.creation.compose.state.rememberRemoteColor
import androidx.compose.remote.creation.compose.state.rememberRemoteString
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.remote.test.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
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
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun avatarImage_circleShape() {
        val size = 48.dp
        remoteComposeTestRule.runScreenshotTest(size = Size(size.value, size.value)) {
            val avatarImage =
                rememberRemoteBitmapValue(name = "avatarImage") {
                    createImage(size.value.toInt(), size.value.toInt())
                }
            AvatarImage(
                avatarImage,
                contentDescription = rememberRemoteString { "background" },
                RemoteModifier.size(size),
            )
        }
    }

    @Test
    fun backgroundImage_roundedShapeAndHasOverlay() {
        val size = 227.dp
        remoteComposeTestRule.runScreenshotTest(size = Size(size.value, size.value)) {
            val backgroundImage =
                rememberRemoteBitmapValue(name = "backgroundImage") {
                    createImage(size.value.toInt(), size.value.toInt())
                }
            BackgroundImage(
                background = backgroundImage,
                contentDescription = rememberRemoteString { "background" },
                modifier = RemoteModifier.size(size),
                overlayColor =
                    rememberRemoteColor("overlay") {
                        androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.6f)
                    },
            )
        }
    }

    internal companion object {
        // Draws a red cross with a blue background
        fun createImage(tw: Int, th: Int): Bitmap {
            val image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            image.eraseColor(Color.BLUE)
            val paint = Paint()
            val canvas = Canvas(image)
            paint.strokeWidth = 3f
            paint.isAntiAlias = true
            paint.setColor(Color.RED)
            canvas.drawLine(0f, 0f, tw.toFloat(), th.toFloat(), paint)
            canvas.drawLine(0f, th.toFloat(), tw.toFloat(), 0f, paint)
            return image
        }
    }
}
