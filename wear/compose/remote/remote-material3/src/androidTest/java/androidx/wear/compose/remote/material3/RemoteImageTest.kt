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
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteImage as CreationRemoteImage
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberRemoteBitmap
import androidx.compose.remote.creation.compose.state.rememberRemoteBitmapValue
import androidx.compose.remote.creation.compose.state.rememberRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.test.R
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
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY).apply {
            bitmapLoader = BitmapLoader {
                val resources = ApplicationProvider.getApplicationContext<Context>().resources
                resources.openRawResource(R.drawable.clear)
            }
        }
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteImage() {
        val size = 48
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(size, size, context.resources.displayMetrics.densityDpi),
            backgroundColor = androidx.compose.ui.graphics.Color.Black,
        ) {
            val avatarImage =
                rememberRemoteBitmapValue(name = "avatarImage") {
                    createImage(size, size).asImageBitmap()
                }
            CreationRemoteImage(
                avatarImage,
                contentDescription = rememberRemoteString { "background" },
                modifier = RemoteModifier.size(size.rdp),
                contentScale = ContentScale.Fit,
                alpha = DefaultAlpha.rf,
            )
        }
    }

    @Test
    fun remoteImage_withAlpha() {
        val size = 227
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(size, size, context.resources.displayMetrics.densityDpi),
            backgroundColor = androidx.compose.ui.graphics.Color.Black,
        ) {
            val backgroundImage =
                rememberRemoteBitmapValue(name = "backgroundImage") {
                    createImage(size, size).asImageBitmap()
                }
            CreationRemoteImage(
                remoteBitmap = backgroundImage,
                contentDescription = rememberRemoteString { "background" },
                modifier = RemoteModifier.size(size.rdp),
                contentScale = ContentScale.Fit,
                alpha = 0.6f.rf,
            )
        }
    }

    @Test
    fun remoteImageWithDefaultUrl() {
        val size = 48
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(size, size, context.resources.displayMetrics.densityDpi)
        ) {
            // Without PlayerState API, will be blank
            val dummyImage =
                rememberRemoteBitmap(
                    name = "dummy",
                    url = "android.resource://androidx.compose.remote.foundation/drawable/dummy",
                )
            CreationRemoteImage(
                dummyImage,
                contentDescription = rememberRemoteString { "background" },
                modifier = RemoteModifier.size(size.rdp),
                contentScale = ContentScale.Fit,
                alpha = DefaultAlpha.rf,
            )
        }
    }

    @Test
    fun remoteImageWithImageBitmap() {
        val size = 48
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo =
                CreationDisplayInfo(size, size, context.resources.displayMetrics.densityDpi)
        ) {
            val backgroundImage = createImage(size, size)
            CreationRemoteImage(
                bitmap = backgroundImage.asImageBitmap(),
                contentDescription = rememberRemoteString { "background" },
                modifier = RemoteModifier.size(size.rdp),
                contentScale = ContentScale.Fit,
                alpha = 0.6f.rf,
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
