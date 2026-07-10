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
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.R
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.debugPrintDoc
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

// TODO: improve on and expand the tests here b/450985714
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ModifierCornersTest : BaseRemoteComposeTest() {
    @Test
    fun translateBox_corners_px() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.size(100.dp, 100.dp).cornerRadius(33.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val rc = GlanceDebugCreationContext()
            doc.rootLayoutComponent!!.layout(rc)
            doc.rootLayoutComponent!!.paint(rc.paintContext!!)

            val box = getSimpleLeaf(doc) as BoxLayout
            debugPrintDoc(doc)

            val clipOperations =
                box.componentModifiers.list.filterIsInstance<RoundedClipRectModifierOperation>()
            assertEquals(1, clipOperations.size)
            val clipOp: RoundedClipRectModifierOperation = clipOperations.first()
            val stringer = StringSerializer()
            clipOp.serializeToString(0, stringer)
            val modifierStr: String = stringer.toString()
            val expected = "33.0, 33.0, 33.0, 33.0]"
            assertContains(modifierStr, expected)
        }

    @Test
    fun translateBox_corners_areResource() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.size(100.dp, 100.dp)
                                .cornerRadius(R.dimen.glance_component_square_icon_button_corners)
                    ) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val docStr = doc.toNestedString()
            assertContains(docStr, "RoundedClipRectModifierOperation 16.0 16.0 16.0 16.0")
        }
}
