/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TranslateRowTest : BaseRemoteComposeTest() {

    @Test
    fun translateRow_100x100() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Row(modifier = GlanceModifier.size(100.dp, 100.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val row = getSimpleLeaf(doc) as RowLayout
            assertEquals(100f, row.width)
            assertEquals(100f, row.height)
        }

    @Test
    fun translateRow_MatchWidth_100dpHeight() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Row(modifier = GlanceModifier.fillMaxWidth().height(100.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val row = getSimpleLeaf(doc) as RowLayout

            assertEquals(DimensionModifierOperation.Type.FILL, row.widthModifier!!.type)
            assertEquals(100f, row.height)
        }

    @Test
    fun translateRow_FillMaxSize() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val row = getSimpleLeaf(doc) as RowLayout

            assertEquals(DimensionModifierOperation.Type.FILL, row.widthModifier!!.type)
            assertEquals(DimensionModifierOperation.Type.FILL, row.heightModifier!!.type)
        }

    @Test
    fun translateRow_wrapContent() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val row = getSimpleLeaf(doc) as RowLayout

            assertEquals(DimensionModifierOperation.Type.FILL, row.widthModifier!!.type)
            assertEquals(DimensionModifierOperation.Type.FILL, row.heightModifier!!.type)
        }
} // end tests
