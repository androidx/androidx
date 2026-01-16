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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.ForEachSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.layout.Box
import androidx.glance.layout.size
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TranslateSizeBoxTest : BaseRemoteComposeTest() {
    @Test
    fun translateSingleSizeWidget() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.size(100.dp, 100.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val box = getSimpleLeaf(doc) as BoxLayout

            assertEquals(100f, box.width)
            assertEquals(100f, box.height)
        }

    @Test
    fun translateBox_100x100_100x200_sizeModeResponsive() =
        fakeCoroutineScope.runTest {
            val responsiveSizeMode =
                SizeMode.Responsive(setOf(DpSize(100.dp, 100.dp), DpSize(100.dp, 200.dp)))

            val results: List<Pair<DpSize, GlanceToRemoteComposeTranslation.Single>> =
                context.runAndTranslateMultiRoot {
                    ForEachSize(sizeMode = responsiveSizeMode, minSize = DpSize(100.dp, 100.dp)) {
                        Box(modifier = GlanceModifier.size(100.dp, 100.dp)) {
                            // no content
                        }
                    }
                }

            results.forEach {
                (dpSize, translation): Pair<DpSize, GlanceToRemoteComposeTranslation.Single> ->
                val doc =
                    makeCoreDocumentForDebug(
                        wireBuffer = translation.remoteComposeContext.buffer.buffer
                    )

                val box = getSimpleLeaf(doc) as BoxLayout

                assertEquals(100f, box.width)
                assertEquals(100f, box.height)
            }
        }

    @Test
    fun translateBox_sizeModeResponsive_producesCorrectPairs() =
        fakeCoroutineScope.runTest {
            val size100x100 = DpSize(100.dp, 100.dp)
            val size100x200 = DpSize(100.dp, 200.dp)

            val responsiveSizeMode = SizeMode.Responsive(setOf(size100x100, size100x200))

            val results: List<Pair<DpSize, GlanceToRemoteComposeTranslation.Single>> =
                context.runAndTranslateMultiRoot {
                    ForEachSize(sizeMode = responsiveSizeMode, minSize = DpSize(100.dp, 100.dp)) {
                        Box(modifier = GlanceModifier.size(1.dp, 1.dp)) {
                            // no content
                        }
                    }
                }

            val sizes = results.map { pair -> pair.first }

            assertContains(sizes, size100x100)
            assertContains(sizes, size100x200)
        }
}
