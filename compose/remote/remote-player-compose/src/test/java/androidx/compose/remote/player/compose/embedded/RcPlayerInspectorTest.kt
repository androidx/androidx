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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
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

    private fun collectInspectables(rootNode: SemanticsNode): List<InspectableValue> {
        val inspectables = mutableListOf<InspectableValue>()
        val visited = mutableSetOf<LayoutInfo>()
        fun walk(node: SemanticsNode) {
            var current: LayoutInfo? = node.layoutInfo
            while (current != null && visited.add(current)) {
                for (modInfo in current.getModifierInfo()) {
                    val inspectable = modInfo.modifier as? InspectableValue
                    if (inspectable != null) {
                        inspectables.add(inspectable)
                    }
                }
                current = current.parentInfo
            }
            for (child in node.children) {
                walk(child)
            }
        }
        walk(rootNode)
        return inspectables
    }

    private fun collectInspectableNames(rootNode: SemanticsNode): Set<String> =
        collectInspectables(rootNode).mapNotNull { it.nameFallback }.toSet()

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
        val inspectables = collectInspectables(rootNode)
        assertThat(inspectables.filterIsInstance<RcComponentInspectable>()).isEmpty()
        assertThat(inspectables.filterIsInstance<RcModifierInspectable>()).isEmpty()
        val names = inspectables.mapNotNull { it.nameFallback }.toSet()
        assertThat(names).doesNotContain(RcPlayerInspector.INSPECTOR_ROOT_NAME)
        assertThat(names).doesNotContain(RcPlayerInspector.INSPECTOR_CONTENT_NAME)

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

        // Verify RcPlayerState implements InspectableValue directly with lazy document hierarchy
        val stateInspectable: InspectableValue = state
        assertThat(stateInspectable.nameFallback).isEqualTo("RcPlayerState")
        assertThat(stateInspectable.valueOverride).isSameInstanceAs(doc)
        val stateElementNames = stateInspectable.inspectableElements.map { it.name }.toList()
        assertThat(stateElementNames)
            .containsExactly(
                "document",
                "remoteContext",
                "currentTimeMillis",
                "rootComponent",
                "variables",
                "functions",
                "operations",
            )
        assertThat(state.rootComponentInspectable).isInstanceOf(RcRootLayoutInspectable::class.java)
        assertThat(state.rootComponentInspectable?.nameFallback).isEqualTo("rcRootLayout")
        assertThat(state.variablesInspectable).isNotEmpty()

        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val inspectables = collectInspectables(rootNode)
        val names = inspectables.mapNotNull { it.nameFallback }.toSet()
        assertThat(names).contains(RcPlayerInspector.INSPECTOR_ROOT_NAME)
        assertThat(names).contains(RcPlayerInspector.INSPECTOR_CONTENT_NAME)
        assertThat(names).contains("rcColumnLayout")
        assertThat(names).contains("rcBoxLayout")
        assertThat(names).contains("rcPadding")
        assertThat(names).contains("rcWidth")
        assertThat(names).contains("rcHeight")
        assertThat(names).contains("rcSemantics")
        assertThat(names).contains("rcVisibility")

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

    @Test
    fun remoteTextLayoutNode_attachesComposeUiTextAndRcCoreTextAndModifiersSideBySide() {
        isDebugInspectorInfoEnabled = true
        rule.setRemoteContent(
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = ApplicationProvider.getApplicationContext(),
                    size = Size(200f, 200f),
                )
        ) {
            val greeting = remember { createNamedRemoteString("greeting", "Hello RemoteText") }
            val textWidth = remember {
                createNamedRemoteFloatExpression("textWidth") { 120f.rf }
            }
            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 200.rdp)) {
                RemoteText(
                    text = greeting,
                    color = Color.Red.rc,
                    fontSize = 18.rsp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier =
                        RemoteModifier.width(textWidth.asRemoteDp())
                            .height(40.rdp)
                            .padding(4.rdp, 6.rdp, 4.rdp, 6.rdp)
                            .semantics { contentDescription = greeting },
                )
            }
        }
        rule.waitForIdle()

        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val state =
            collectInspectables(rootNode)
                .first { it.nameFallback == RcPlayerInspector.INSPECTOR_ROOT_NAME }
                .valueOverride as RcPlayerState

        // Locate the RemoteText SemanticsNode and walk its LayoutInfo chain
        val textSemanticsNode =
            rule.onNodeWithText("Hello RemoteText", useUnmergedTree = true).fetchSemanticsNode()

        val chainInspectables = mutableListOf<InspectableValue>()
        var curr: LayoutInfo? = textSemanticsNode.layoutInfo
        while (curr != null) {
            for (modInfo in curr.getModifierInfo()) {
                val inspectable = modInfo.modifier as? InspectableValue
                if (inspectable != null) {
                    chainInspectables.add(inspectable)
                }
            }
            if (chainInspectables.any { it is RcCoreTextInspectable }) {
                break
            }
            curr = curr.parentInfo
        }

        // Verify both Compose UI's TextStringSimpleElement and RcCoreTextInspectable
        // ("rcCoreText") are attached on the RemoteText node chain
        val chainNames = chainInspectables.mapNotNull { it.nameFallback }
        assertThat(chainNames).contains("TextStringSimpleElement")
        assertThat(chainNames).contains("rcCoreText")
        assertThat(chainNames).contains("rcWidth")
        assertThat(chainNames).contains("rcHeight")
        assertThat(chainNames).contains("rcPadding")
        assertThat(chainNames).contains("rcSemantics")

        val rcText = chainInspectables.filterIsInstance<RcCoreTextInspectable>().single()
        assertThat(rcText.text).isEqualTo("Hello RemoteText")
        assertThat(rcText.fontWeight).isEqualTo(700f)
        assertThat(rcText.maxLines).isEqualTo(2)
        assertThat(rcText.visibility).isEqualTo("VISIBLE")
        assertThat(rcText.modifiers.map { it.nameFallback })
            .containsAtLeast("rcWidth", "rcHeight", "rcPadding", "rcSemantics")

        val rcWidth = chainInspectables.filterIsInstance<RcWidthModifierInspectable>().first()
        assertThat(rcWidth.value).isEqualTo(120f)

        val rcPadding = chainInspectables.filterIsInstance<RcPaddingModifierInspectable>().first()
        assertThat(rcPadding.left).isEqualTo(4f)
        assertThat(rcPadding.top).isEqualTo(6f)
        assertThat(rcPadding.right).isEqualTo(4f)
        assertThat(rcPadding.bottom).isEqualTo(6f)

        val rcSemantics =
            chainInspectables.filterIsInstance<RcSemanticsModifierInspectable>().first()
        assertThat(rcSemantics.contentDescription).isEqualTo("Hello RemoteText")

        // Verify lazy on-demand evaluation: mutating state updates the existing inspectable's
        // computed properties without reconstructing the inspectable
        val greetingId = RcPlayerInspector.resolveVariableId(state, "greeting")!!
        val textWidthId = RcPlayerInspector.resolveVariableId(state, "textWidth")!!
        state.remoteContext.overrideText(greetingId, "Updated Lazily")
        state.remoteContext.overrideFloat(textWidthId, 160f)
        rule.waitForIdle()

        assertThat(rcText.text).isEqualTo("Updated Lazily")
        assertThat(rcSemantics.contentDescription).isEqualTo("Updated Lazily")
        assertThat(rcWidth.value).isEqualTo(160f)
    }

    @Test
    fun canvasLoopConditionalAndFunctionInspectables_evaluateLazilyOnDemand() {
        val doc = CoreDocument(RemoteClock.SYSTEM)
        val state = RcPlayerState(doc)
        val ctx = state.remoteContext

        val varAId = 42
        ctx.loadFloat(varAId, 10f)

        val drawRect = DrawRect(0f, 0f, 50f, 50f)
        val condOp =
            ConditionalOperations(
                    ConditionalOperations.TYPE_EQ,
                    Utils.asNan(varAId),
                    10f,
                )
                .apply { list.add(drawRect) }

        val condInspectable = RcConditionalInspectable(condOp, ctx)
        assertThat(condInspectable.nameFallback).isEqualTo("rcConditional")
        assertThat(condInspectable.type).isEqualTo("eq")
        assertThat(condInspectable.varA).isEqualTo(10f)
        assertThat(condInspectable.varB).isEqualTo(10f)
        assertThat(condInspectable.conditionMet).isTrue()
        assertThat(condInspectable.operationsInspectable).hasSize(1)
        val childDraw = condInspectable.operationsInspectable.single() as RcDrawOperationInspectable
        assertThat(childDraw.commandName).isEqualTo("drawRect")

        // Mutate variable in RemoteContext and verify RcConditionalInspectable re-evaluates on
        // demand
        ctx.loadFloat(varAId, 25f)
        assertThat(condInspectable.varA).isEqualTo(25f)
        assertThat(condInspectable.conditionMet).isFalse()

        // Verify LoopOperation inspectable
        val loopOp =
            LoopOperation(99, 0f, 2f, 6f).apply {
                list.add(drawRect)
            }
        val loopInspectable = RcLoopInspectable(loopOp, ctx)
        assertThat(loopInspectable.nameFallback).isEqualTo("rcLoop")
        assertThat(loopInspectable.from).isEqualTo(0f)
        assertThat(loopInspectable.until).isEqualTo(6f)
        assertThat(loopInspectable.step).isEqualTo(2f)
        assertThat(loopInspectable.indexVariableId).isEqualTo(99)
        assertThat(loopInspectable.estimatedIterations).isEqualTo(3)
        assertThat(loopInspectable.operationsInspectable).hasSize(1)

        // Verify FloatFunctionDefine and FloatFunctionCall inspectables
        val fnDef =
            FloatFunctionDefine(7, intArrayOf(101, 102)).apply {
                list.add(drawRect)
            }
        val fnDefInspectable = RcFunctionDefineInspectable(fnDef, ctx)
        assertThat(fnDefInspectable.nameFallback).isEqualTo("rcFunctionDefine")
        assertThat(fnDefInspectable.functionId).isEqualTo(7)
        assertThat(fnDefInspectable.argVarIds).containsExactly(101, 102).inOrder()
        assertThat(fnDefInspectable.operationsInspectable).hasSize(1)

        val fnCall = FloatFunctionCall(7, floatArrayOf(Utils.asNan(varAId), 5f))
        val fnCallInspectable = RcFunctionCallInspectable(fnCall, ctx)
        assertThat(fnCallInspectable.nameFallback).isEqualTo("rcFunctionCall")
        assertThat(fnCallInspectable.functionId).isEqualTo(7)
        assertThat(fnCallInspectable.resolvedArgs).containsExactly(25f, 5f).inOrder()

        // Verify PathData inspectable
        val pathData = PathData(300, floatArrayOf(10f, 0f, 0f, 15f), 0)
        val pathInspectable = RcPathOperationInspectable(pathData, ctx)
        assertThat(pathInspectable.nameFallback).isEqualTo("rcPathData")
        assertThat(pathInspectable.instanceId).isEqualTo(300)
        assertThat(pathInspectable.floatPathLength).isEqualTo(4)

        // Verify CanvasLayout inspectable wraps child operations lazily
        val canvasLayout =
            CanvasLayout(null, 10, -1, 0f, 0f, 100f, 100f).apply {
                list.add(condOp)
                list.add(loopOp)
            }
        val canvasInspectable =
            RcComponentInspectable.of(canvasLayout, ctx) as RcCanvasLayoutInspectable
        assertThat(canvasInspectable.nameFallback).isEqualTo("rcCanvasLayout")
        assertThat(canvasInspectable.operations)
            .containsExactlyElementsIn(listOf(condInspectable, loopInspectable))
    }
}
