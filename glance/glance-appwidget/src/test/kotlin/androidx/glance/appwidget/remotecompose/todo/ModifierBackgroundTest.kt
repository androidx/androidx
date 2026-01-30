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
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.ColorRes
import androidx.compose.remote.core.WireBuffer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.remotecompose.BaseRemoteComposeTest
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.appwidget.remotecompose.makeCoreDocumentForDebug
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import kotlin.test.Ignore
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ModifierBackgroundTest : BaseRemoteComposeTest() {

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundColor_fixed() =
        fakeCoroutineScope.runTest {
            //            val backgroundColor = Color.Red
            //
            //            val (_, wireBuffer: WireBuffer) =
            //                context.runAndTranslateSingleRoot {
            //                    Box(
            //                        modifier = GlanceModifier.size(100.dp,
            // 100.dp).background(backgroundColor)
            //                    ) {
            //                        // no content
            //                    }
            //                }
            //
            //            val doc: CoreDocument = makeCoreDocumentForDebug(wireBuffer = wireBuffer,
            // platform = platform)
            //
            //            val box = getSimpleLeaf(doc) as BoxLayout
            //            val rcModifierOperation: ModifierOperation? =
            //                box.mComponentModifiers.list.find { it is BackgroundModifierOperation
            // }
            //            rcModifierOperation as? BackgroundModifierOperation
            //                ?: fail("background modifier not found")
            //            val modifierBackgroundColor = Color(red = rcModifierOperation.mR, green =
            // rcModifierOperation.mG, blue = rcModifierOperation.mB)
            //            assertEquals(backgroundColor.toArgb(), modifierBackgroundColor)
            fail("todo: rewrite this test. The fields we were using are now private")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundColor_DayNight() =
        fakeCoroutineScope.runTest {
            var backgroundColor: ColorProvider

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    backgroundColor = GlanceTheme.colors.background

                    Box(
                        modifier =
                            GlanceModifier.Companion.size(100.dp, 100.dp)
                                .background(backgroundColor)
                    ) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            /*
             * TODO:
             * There's a few ways to do daynight. The first is to duplicate a component into
             * day and night versions. This is supported as of 2024-12-13, but it might not be the
             * best solution because it adds more bytes, and the representation of it is a little
             * hard to discern from the data structure. There's some consideration about other ways
             * to implement these sorts of themed colors.
             */
            TODO("write test")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundColor_Resource() =
        fakeCoroutineScope.runTest {
            @ColorRes val backgroundColorResource: Int = R.color.system_background_dark

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.Companion.size(100.dp, 100.dp)
                                .background(backgroundColorResource)
                    ) {
                        // no content
                    }
                }

            TODO("Translate a box with a background set to a color resource")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundImage_fillBounds() =
        fakeCoroutineScope.runTest {
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            bitmap.applyCanvas() {
                val paint = Paint()
                paint.color = Color.CYAN
                paint.style = Paint.Style.FILL
                drawRect(0f, 0f, 100f, 100f, paint)
            }

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.Companion.size(100.dp, 100.dp)
                                .background(
                                    imageProvider = ImageProvider(bitmap),
                                    contentScale = ContentScale.Companion.FillBounds,
                                )
                    ) {
                        // no content
                    }
                }

            TODO("Translate a box with a background set to a bitmap")
        }

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun translateBox_backgroundImage_fit() =
        fakeCoroutineScope.runTest {
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            bitmap.applyCanvas() {
                val paint = Paint()
                paint.color = Color.CYAN
                paint.style = Paint.Style.FILL
                drawRect(0f, 0f, 100f, 100f, paint)
            }

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.Companion.size(100.dp, 150.dp)
                                .background(
                                    imageProvider = ImageProvider(bitmap),
                                    contentScale = ContentScale.Companion.Fit,
                                )
                    ) {
                        // no content
                    }
                }

            TODO("Translate a box with a background set to a bitmap and check the content scaling")
        }
}
