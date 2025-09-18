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
package androidx.compose.remote.foundation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.size
import androidx.compose.remote.frontend.state.rememberRemoteBitmapValue
import androidx.compose.remote.frontend.state.rememberRemoteFloatValue
import androidx.compose.remote.frontend.state.rememberRemoteString
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
class RemoteImageTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun remoteImage() {
        val size = 48.dp
        remoteComposeTestRule.runScreenshotTest(
            size = Size(size.value, size.value),
            backgroundColor = androidx.compose.ui.graphics.Color.Black,
        ) {
            val avatarImage =
                rememberRemoteBitmapValue(name = "avatarImage") {
                    createImage(size.value.toInt(), size.value.toInt())
                }
            RemoteImage(
                avatarImage,
                contentDescription = rememberRemoteString { "background" },
                RemoteModifier.size(size),
            )
        }
    }

    @Test
    fun remoteImage_withAlpha() {
        val size = 227.dp
        remoteComposeTestRule.runScreenshotTest(
            size = Size(size.value, size.value),
            backgroundColor = androidx.compose.ui.graphics.Color.Black,
        ) {
            val backgroundImage =
                rememberRemoteBitmapValue(name = "backgroundImage") {
                    createImage(size.value.toInt(), size.value.toInt())
                }
            RemoteImage(
                remoteBitmap = backgroundImage,
                alpha = rememberRemoteFloatValue { 0.6f },
                contentDescription = rememberRemoteString { "background" },
                modifier = RemoteModifier.size(size),
            )
        }
    }

    companion object {
        fun createImage(tw: Int, th: Int): Bitmap {
            val image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
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
