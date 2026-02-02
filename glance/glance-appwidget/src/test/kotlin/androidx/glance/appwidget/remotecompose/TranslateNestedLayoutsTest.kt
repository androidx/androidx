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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.debugPrintDoc
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.size
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TranslateNestedLayoutsTest : BaseRemoteComposeTest() {

    @Test
    fun translate_Row100x100_BoxWDefaultWeightHMatch() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Row(GlanceModifier.size(100.dp, 100.dp)) {
                        Box(modifier = GlanceModifier.fillMaxHeight().defaultWeight()) {
                            // no content
                        }
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val box = getSimpleLeaf(doc) as BoxLayout
            val widthModifier = box.widthModifier!!
            assertEquals(DimensionModifierOperation.Type.WEIGHT, widthModifier.type)
            assertTrue(widthModifier.hasWeight())
            assertEquals(1f, widthModifier.value)
            assertEquals(DimensionModifierOperation.Type.FILL, box.heightModifier!!.type)

            debugPrintDoc(doc)
        }
}
