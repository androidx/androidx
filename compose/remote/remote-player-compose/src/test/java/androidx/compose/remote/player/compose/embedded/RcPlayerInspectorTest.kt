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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerInspectorTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Before
    fun setUp() {
        isDebugInspectorInfoEnabled = false
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @Composable
    @RemoteComposable
    private fun TestDocumentContent() {
        val speed = remember { createNamedRemoteFloatExpression("speed") { 80f.rf } }
        val label = remember { createNamedRemoteString("label", "Hello Inspector") }

        RemoteColumn(
            modifier = RemoteModifier.size(200.rdp, 200.rdp).padding(10.rdp, 15.rdp, 10.rdp, 15.rdp)
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(speed.asRemoteDp(), 40.rdp).semantics {
                        contentDescription = label
                    }
            )
            RemoteBox(
                modifier =
                    RemoteModifier.size(60.rdp, 30.rdp)
                        .visibility(Component.Visibility.INVISIBLE.ri)
            )
        }
    }

    private fun collectInspectableNames(rootNode: SemanticsNode): Set<String> {
        val names = mutableSetOf<String>()
        val visited = mutableSetOf<LayoutInfo>()
        fun walk(node: SemanticsNode) {
            var current: LayoutInfo? = node.layoutInfo
            while (current != null && visited.add(current)) {
                for (modInfo in current.getModifierInfo()) {
                    val inspectable = modInfo.modifier as? InspectableValue
                    val name = inspectable?.nameFallback
                    if (name != null) {
                        names.add(name)
                    }
                }
                current = current.parentInfo
            }
            for (child in node.children) {
                walk(child)
            }
        }
        walk(rootNode)
        return names
    }

    @Test
    fun whenInspectorDisabled_noRcInspectorModifiersAttached() {
        isDebugInspectorInfoEnabled = false
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = ApplicationProvider.getApplicationContext(),
                    size = Size(200f, 200f),
                )
        ) {
            TestDocumentContent()
        }
        rule.waitForIdle()

        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val names = collectInspectableNames(rootNode)
        assertThat(names).doesNotContain(RcPlayerInspector.INSPECTOR_ROOT_NAME)
        assertThat(names).doesNotContain(RcPlayerInspector.INSPECTOR_OUTER_NAME)
        assertThat(names).doesNotContain(RcPlayerInspector.INSPECTOR_CONTENT_NAME)
        assertThat(names).doesNotContain(RcPlayerInspector.INSPECTOR_MODIFIER_NAME)

        val snapshot = RcPlayerInspector.captureTreeSnapshot(rootNode)
        assertThat(snapshot).isEmpty()
    }

    @Test
    fun whenInspectorEnabled_exposesInspectableValuesAndCapturesTreeSnapshot() {
        isDebugInspectorInfoEnabled = true
        val doc =
            rule.setRemoteContent(
                remoteCreationDisplayInfo =
                    createCreationDisplayInfo(
                        context = ApplicationProvider.getApplicationContext(),
                        size = Size(200f, 200f),
                    )
            ) {
                TestDocumentContent()
            }
        rule.waitForIdle()

        val state = RcPlayerState(doc)

        // Verify RcPlayerState implements InspectableValue directly
        val stateInspectable: InspectableValue = state
        assertThat(stateInspectable.nameFallback).isEqualTo("RcPlayerState")
        assertThat(stateInspectable.valueOverride).isSameInstanceAs(doc)
        val stateElementNames = stateInspectable.inspectableElements.map { it.name }.toList()
        assertThat(stateElementNames)
            .containsExactly("document", "remoteContext", "currentTimeMillis")

        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val names = collectInspectableNames(rootNode)
        assertThat(names).contains(RcPlayerInspector.INSPECTOR_ROOT_NAME)
        assertThat(names).contains(RcPlayerInspector.INSPECTOR_OUTER_NAME)
        assertThat(names).contains(RcPlayerInspector.INSPECTOR_CONTENT_NAME)
        assertThat(names).contains(RcPlayerInspector.INSPECTOR_MODIFIER_NAME)

        // Capture tree snapshot without passing state (discovered via rcPlayerRoot
        // InspectableValue)
        val snapshot = RcPlayerInspector.captureTreeSnapshot(rootNode)
        assertThat(snapshot).isNotEmpty()

        val rootSnap = snapshot.first { it.kind == "RootLayoutComponent" }
        assertThat(rootSnap.width).isEqualTo(200f)
        assertThat(rootSnap.height).isEqualTo(200f)

        val colSnap = snapshot.first { it.kind == "ColumnLayout" }
        assertThat(colSnap.x).isEqualTo(0f)
        assertThat(colSnap.y).isEqualTo(0f)
        assertThat(colSnap.width).isEqualTo(200f)
        assertThat(colSnap.height).isEqualTo(200f)

        val boxes = snapshot.filter { it.kind == "BoxLayout" }
        assertThat(boxes).hasSize(2)
        // First box is placed inside ColumnLayout's content coordinates (after 10px left, 15px top
        // padding)
        assertThat(boxes[0].x).isEqualTo(0f)
        assertThat(boxes[0].y).isEqualTo(0f)
        assertThat(boxes[0].width).isEqualTo(80f)
        assertThat(boxes[0].height).isEqualTo(40f)
        assertThat(boxes[0].visibility).isEqualTo("VISIBLE")

        // Second box is placed below the first box (y = 40) and is INVISIBLE
        assertThat(boxes[1].x).isEqualTo(0f)
        assertThat(boxes[1].y).isEqualTo(40f)
        assertThat(boxes[1].width).isEqualTo(60f)
        assertThat(boxes[1].height).isEqualTo(30f)
        assertThat(boxes[1].visibility).isEqualTo("INVISIBLE")

        // Verify variable and operation probes via RcPlayerInspector
        assertThat(RcPlayerInspector.resolveFloat(state, "USER:speed")).isEqualTo(80f)
        assertThat(RcPlayerInspector.resolveFloat(state, "speed")).isEqualTo(80f)
        assertThat(RcPlayerInspector.resolveText(state, "USER:label")).isEqualTo("Hello Inspector")
        assertThat(RcPlayerInspector.collectAllOperations(doc)).isNotEmpty()
    }
}
