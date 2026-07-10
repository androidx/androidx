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

package androidx.glance.appwidget.remotecompose.todo

import android.R
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.remote.core.WireBuffer
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.remotecompose.BaseRemoteComposeTest
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.appwidget.remotecompose.makeCoreDocumentForDebug
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.size
import kotlin.test.Ignore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TranslateEmittableImageTest : BaseRemoteComposeTest() {

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundImageProvider_androidResourceImageProvider() =
        fakeCoroutineScope.runTest {
            @DrawableRes val backgroundImageResource: Int = R.drawable.ic_secure
            val backgroundProvider = ImageProvider(backgroundImageResource)

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.Companion.size(100.dp, 100.dp)
                                .background(backgroundProvider)
                    ) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            RemoteComposeTestUtils.debugPrintDoc(doc)

            TODO("Translate a box with a background set to an image")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundImage_iconImageProvider() =
        fakeCoroutineScope.runTest { TODO("test IconImageProider") }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundImage_bitmapImageProvider() =
        fakeCoroutineScope.runTest {
            @DrawableRes val id = R.drawable.ic_secure
            val bmp = BitmapFactory.decodeResource(context.resources, id)

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.Companion.size(100.dp, 100.dp)
                                .background(ImageProvider(bmp))
                    ) {
                        // no content
                    }
                }

            TODO("test BitmapImageProvider")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundImage_uriImageProvider() =
        fakeCoroutineScope.runTest { TODO("test uriImageProvider") }
}
