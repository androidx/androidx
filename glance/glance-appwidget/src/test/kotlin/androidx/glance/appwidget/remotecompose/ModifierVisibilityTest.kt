/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Visibility
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.debugPrintDoc
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.visibility
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ModifierVisibilityTest : BaseRemoteComposeTest() {
    @Test
    fun translateBox_visible() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.visibility(Visibility.Visible)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val debugContext = GlanceDebugCreationContext()
            //            doc.initializeContext(debugContext)

            val box = getSimpleLeaf(doc) as BoxLayout
            box.updateVariables(debugContext)
            debugPrintDoc(doc)
            val modifiers = box.mList[0] as ComponentModifiers
            val modifier = modifiers.list[0] as ComponentVisibilityOperation

            assertEquals(Component.Visibility.VISIBLE, box.mVisibility)
        }

    @Test
    fun translateBox_invisible() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.size(300.dp, 150.dp).visibility(Visibility.Invisible)
                    ) {}
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val debugContext = GlanceDebugCreationContext()
            doc.initializeContext(debugContext)

            val box = getSimpleLeaf(doc) as BoxLayout
            doc.paint(debugContext, Theme.UNSPECIFIED)
            debugPrintDoc(doc)
            assertEquals(Component.Visibility.INVISIBLE, box.mVisibility)
        }

    @Test
    fun translateBox_gone() =
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier = GlanceModifier.size(300.dp, 150.dp).visibility(Visibility.Gone)
                    ) {}
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val debugContext = GlanceDebugCreationContext()
            doc.initializeContext(debugContext)

            val box = getSimpleLeaf(doc) as BoxLayout
            doc.paint(debugContext, Theme.UNSPECIFIED)
            box.updateVariables(debugContext)
            debugPrintDoc(doc)

            assertEquals(Component.Visibility.GONE, box.mVisibility)
        }
}
