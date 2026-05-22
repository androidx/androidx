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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.remote.core.Limits
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBitmap
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.test.R
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Before
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
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            bitmapLoader =
                BitmapLoader {
                    val resources = ApplicationProvider.getApplicationContext<Context>().resources
                    resources.openRawResource(R.drawable.clear)
                },
        )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var originalEnableImageUrls: Boolean = false
    private var originalEnableImageFiles: Boolean = false

    @Before
    fun setUp() {
        originalEnableImageUrls = Limits.ENABLE_IMAGE_URLS
        originalEnableImageFiles = Limits.ENABLE_IMAGE_FILES
        Limits.ENABLE_IMAGE_URLS = true
        Limits.ENABLE_IMAGE_FILES = true
    }

    @After
    fun tearDown() {
        Limits.ENABLE_IMAGE_URLS = originalEnableImageUrls
        Limits.ENABLE_IMAGE_FILES = originalEnableImageFiles
    }

    @Test
    fun remoteImage() {
        val size = 48
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(context, Size(size.toFloat(), size.toFloat())),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val avatarImage =
                rememberNamedRemoteBitmap(name = "avatarImage") {
                    createImage(size, size).asImageBitmap()
                }
            RemoteImage(
                avatarImage,
                contentDescription = "background".rs,
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
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(context, Size(size.toFloat(), size.toFloat())),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val backgroundImage =
                rememberNamedRemoteBitmap(name = "backgroundImage") {
                    createImage(size, size).asImageBitmap()
                }
            RemoteImage(
                remoteBitmap = backgroundImage,
                contentDescription = "background".rs,
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
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(context, Size(size.toFloat(), size.toFloat()))
        ) {
            // Without PlayerState API, will be blank
            val dummyImage =
                rememberNamedRemoteBitmap(
                    name = "dummy",
                    url = "android.resource://androidx.compose.remote.foundation/drawable/dummy",
                )
            RemoteImage(
                dummyImage,
                contentDescription = "background".rs,
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
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(context, Size(size.toFloat(), size.toFloat()))
        ) {
            val backgroundImage = createImage(size, size)
            RemoteImage(
                bitmap = backgroundImage.asImageBitmap(),
                contentDescription = "background".rs,
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
            paint.setColor(android.graphics.Color.RED)
            canvas.drawLine(0f, 0f, tw.toFloat(), th.toFloat(), paint)
            canvas.drawLine(0f, th.toFloat(), tw.toFloat(), 0f, paint)
            return image
        }
    }
}
