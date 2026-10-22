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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatFunctionCall
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LoopOperation
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
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

    private operator fun InspectableValue.get(name: String): Any? =
        inspectableElements.firstOrNull { it.name == name }?.value

    private fun collectModifierInfos(rootNode: SemanticsNode): List<ModifierInfo> {
        val infos = mutableListOf<ModifierInfo>()
        val visited = mutableSetOf<LayoutInfo>()
        fun walk(node: SemanticsNode) {
            var current: LayoutInfo? = node.layoutInfo
            while (current != null && visited.add(current)) {
                infos.addAll(current.getModifierInfo())
                current = current.parentInfo
            }
            for (child in node.children) {
                walk(child)
            }
        }
        walk(rootNode)
        return infos
    }

    private fun collectInspectables(rootNode: SemanticsNode): List<InspectableValue> =
        collectModifierInfos(rootNode).mapNotNull { it.modifier as? InspectableValue }

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
        val names = collectInspectables(rootNode).mapNotNull { it.nameFallback }.toSet()
        assertThat(names.filter { it.startsWith("rc") }).isEmpty()
    }

    private fun captureDoc(content: @Composable @RemoteComposable () -> Unit): CoreDocument =
        runBlocking {
            val bytes =
                captureSingleRemoteDocument(
                        context = ApplicationProvider.getApplicationContext(),
                        creationDisplayInfo =
                            createCreationDisplayInfo(
                                context = ApplicationProvider.getApplicationContext(),
                                size = Size(200f, 200f),
                            ),
                        content = content,
                    )
                    .bytes
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun whenInspectorEnabled_exposesInspectableValuesAndCapturesTreeSnapshot() {
        isDebugInspectorInfoEnabled = true
        val doc = captureDoc { TestDocumentContent() }
        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = doc) } }
        rule.waitForIdle()

        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val modInfos = collectModifierInfos(rootNode)
        val inspectables = modInfos.mapNotNull { it.modifier as? InspectableValue }
        val names = inspectables.mapNotNull { it.nameFallback }.toSet()
        assertThat(names).contains("rcPlayerRoot")
        assertThat(names).contains("rcComponentContent")
        assertThat(names).contains("rcColumnLayout")
        assertThat(names).contains("rcBoxLayout")
        assertThat(names).contains("rcPadding")
        assertThat(names).contains("rcWidth")
        assertThat(names).contains("rcHeight")
        assertThat(names).contains("rcSemantics")
        assertThat(names).contains("rcVisibility")

        // Discover live RcPlayerState from the attached rcPlayerRoot InspectableValue
        val rootModInfo = modInfos.first {
            (it.modifier as? InspectableValue)?.nameFallback == "rcPlayerRoot"
        }
        val state = (rootModInfo.modifier as InspectableValue).valueOverride as RcPlayerState

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
        val rootComp = stateInspectable["rootComponent"] as InspectableValue
        assertThat(rootComp.nameFallback).isEqualTo("rcRootLayout")
        assertThat(rootComp["kind"]).isEqualTo("RootLayoutComponent")

        val variables = stateInspectable["variables"] as List<InspectableValue>
        assertThat(variables).isNotEmpty()
        assertThat(variables.first { it["variableName"] == "USER:speed" }["value"]).isEqualTo(80f)
        assertThat(variables.first { it["variableName"] == "USER:label" }["value"])
            .isEqualTo("Hello Inspector")
        assertThat(state.floatState("speed").value).isEqualTo(80f)
        assertThat(state.stringState("label").value).isEqualTo("Hello Inspector")
        assertThat(stateInspectable["operations"] as List<InspectableValue>).isNotEmpty()

        // Verify live layout coordinates and visibility through ModifierInfo + InspectableValue
        assertThat(rootModInfo.coordinates.size).isEqualTo(IntSize(200, 200))

        val colOuterInfo = modInfos.first {
            (it.modifier as? InspectableValue)?.nameFallback == "rcColumnLayout"
        }
        assertThat(colOuterInfo.coordinates.size).isEqualTo(IntSize(200, 200))
        val colId = (colOuterInfo.modifier as InspectableValue)["componentId"] as Int
        val colContentCoords =
            modInfos
                .first {
                    val m = it.modifier as? InspectableValue
                    m?.nameFallback == "rcComponentContent" && m.valueOverride == colId
                }
                .coordinates

        val boxInfos =
            modInfos
                .filter { (it.modifier as? InspectableValue)?.nameFallback == "rcBoxLayout" }
                .distinctBy { (it.modifier as InspectableValue)["componentId"] }
        assertThat(boxInfos).hasSize(2)

        val visibleBox = boxInfos.single {
            (it.modifier as InspectableValue)["visibility"] == "VISIBLE"
        }
        assertThat(visibleBox.coordinates.size).isEqualTo(IntSize(80, 40))
        // Placed at (0, 0) in ColumnLayout's content coordinates (10px, 15px from root after
        // padding)
        assertThat(colContentCoords.localPositionOf(visibleBox.coordinates, Offset.Zero))
            .isEqualTo(Offset(0f, 0f))
        assertThat(rootModInfo.coordinates.localPositionOf(visibleBox.coordinates, Offset.Zero))
            .isEqualTo(Offset(10f, 15f))

        val invisibleBox = boxInfos.single {
            (it.modifier as InspectableValue)["visibility"] == "INVISIBLE"
        }
        assertThat(invisibleBox.coordinates.size).isEqualTo(IntSize(60, 30))
        assertThat(colContentCoords.localPositionOf(invisibleBox.coordinates, Offset.Zero))
            .isEqualTo(Offset(0f, 40f))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun remoteTextLayoutNode_attachesComposeUiTextAndRcCoreTextAndModifiersSideBySide() {
        isDebugInspectorInfoEnabled = true
        val doc = captureDoc {
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
        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = doc) } }
        rule.waitForIdle()

        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val state =
            collectInspectables(rootNode).first { it.nameFallback == "rcPlayerRoot" }.valueOverride
                as RcPlayerState

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
            if (chainInspectables.any { it.nameFallback == "rcCoreText" }) {
                break
            }
            curr = curr.parentInfo
        }

        // Verify both Compose UI's TextStringSimpleElement and "rcCoreText" are attached on the
        // RemoteText node chain
        val chainNames = chainInspectables.mapNotNull { it.nameFallback }
        assertThat(chainNames).contains("TextStringSimpleElement")
        assertThat(chainNames).contains("rcCoreText")
        assertThat(chainNames).contains("rcWidth")
        assertThat(chainNames).contains("rcHeight")
        assertThat(chainNames).contains("rcPadding")
        assertThat(chainNames).contains("rcSemantics")

        val rcText = chainInspectables.single { it.nameFallback == "rcCoreText" }
        assertThat(rcText["text"]).isEqualTo("Hello RemoteText")
        assertThat(rcText["fontWeight"]).isEqualTo(700f)
        assertThat(rcText["maxLines"]).isEqualTo(2)
        assertThat(rcText["visibility"]).isEqualTo("VISIBLE")
        val rcTextModifiers = rcText["modifiers"] as List<InspectableValue>
        assertThat(rcTextModifiers.map { it.nameFallback })
            .containsAtLeast("rcWidth", "rcHeight", "rcPadding", "rcSemantics")

        val rcWidth = chainInspectables.first { it.nameFallback == "rcWidth" }
        assertThat(rcWidth["value"]).isEqualTo(120f)

        val rcPadding = chainInspectables.first { it.nameFallback == "rcPadding" }
        assertThat(rcPadding["left"]).isEqualTo(4f)
        assertThat(rcPadding["top"]).isEqualTo(6f)
        assertThat(rcPadding["right"]).isEqualTo(4f)
        assertThat(rcPadding["bottom"]).isEqualTo(6f)

        val rcSemantics = chainInspectables.first { it.nameFallback == "rcSemantics" }
        assertThat(rcSemantics["contentDescription"]).isEqualTo("Hello RemoteText")

        // Verify lazy on-demand evaluation: mutating state updates both the rendered Compose node
        // and the InspectableValue tree exposed by RcPlayerState
        state.stringState("greeting").value = "Updated Lazily"
        state.floatState("textWidth").value = 160f
        rule.waitForIdle()

        rule.onNodeWithText("Updated Lazily", useUnmergedTree = true).assertExists()
        val updatedRoot = state["rootComponent"] as InspectableValue
        val updatedCol = (updatedRoot["children"] as List<InspectableValue>).single()
        val updatedText = (updatedCol["children"] as List<InspectableValue>).single()
        val updatedModifiers = updatedText["modifiers"] as List<InspectableValue>
        assertThat(updatedText["text"]).isEqualTo("Updated Lazily")
        assertThat(
                updatedModifiers.first { it.nameFallback == "rcSemantics" }["contentDescription"]
            )
            .isEqualTo("Updated Lazily")
        assertThat(updatedModifiers.first { it.nameFallback == "rcWidth" }["value"]).isEqualTo(160f)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun canvasLoopConditionalAndFunctionInspectables_evaluateLazilyOnDemand() {
        val doc = CoreDocument(RemoteClock.SYSTEM)
        val varAId = 42
        val drawRect = DrawRect(0f, 0f, 50f, 50f)
        val condOp =
            ConditionalOperations(
                    ConditionalOperations.TYPE_EQ,
                    Utils.asNan(varAId),
                    10f,
                )
                .apply { list.add(drawRect) }
        val loopOp = LoopOperation(99, 0f, 2f, 6f).apply { list.add(drawRect) }
        val fnDef = FloatFunctionDefine(7, intArrayOf(101, 102)).apply { list.add(drawRect) }
        val fnCall = FloatFunctionCall(7, floatArrayOf(Utils.asNan(varAId), 5f))
        val pathData = PathData(300, floatArrayOf(10f, 0f, 0f, 15f), 0)
        val canvasLayout =
            CanvasLayout(null, 10, -1, 0f, 0f, 100f, 100f).apply {
                list.add(condOp)
                list.add(loopOp)
            }

        doc.operations.add(FloatConstant(varAId, 10f))
        doc.operations.add(NamedVariable(varAId, NamedVariable.FLOAT_TYPE, "USER:varA"))
        doc.operations.add(fnDef)
        doc.operations.add(fnCall)
        doc.operations.add(pathData)
        doc.operations.add(canvasLayout)

        val state = RcPlayerState(doc)
        val operations = state["operations"] as List<InspectableValue>

        val canvasInspectable = operations.single { it.nameFallback == "rcCanvasLayout" }
        val canvasOps = canvasInspectable["operations"] as List<InspectableValue>
        assertThat(canvasOps).hasSize(2)

        val condInspectable = canvasOps.single { it.nameFallback == "rcConditional" }
        assertThat(condInspectable["type"]).isEqualTo("eq")
        assertThat(condInspectable["varA"]).isEqualTo(10f)
        assertThat(condInspectable["varB"]).isEqualTo(10f)
        assertThat(condInspectable["conditionMet"]).isEqualTo(true)
        val condChildOps = condInspectable["operations"] as List<InspectableValue>
        assertThat(condChildOps).hasSize(1)
        assertThat(condChildOps.single().nameFallback).isEqualTo("drawRect")
        assertThat(condChildOps.single()["command"]).isEqualTo("drawRect")

        // Mutate variable through RcPlayerState and verify RcConditionalInspectable re-evaluates on
        // demand
        state.floatState("varA").value = 25f
        assertThat(condInspectable["varA"]).isEqualTo(25f)
        assertThat(condInspectable["conditionMet"]).isEqualTo(false)

        // Verify LoopOperation inspectable
        val loopInspectable = canvasOps.single { it.nameFallback == "rcLoop" }
        assertThat(loopInspectable["from"]).isEqualTo(0f)
        assertThat(loopInspectable["until"]).isEqualTo(6f)
        assertThat(loopInspectable["step"]).isEqualTo(2f)
        assertThat(loopInspectable["indexVariableId"]).isEqualTo(99)
        assertThat(loopInspectable["estimatedIterations"]).isEqualTo(3)
        assertThat(loopInspectable["operations"] as List<InspectableValue>).hasSize(1)

        // Verify FloatFunctionDefine and FloatFunctionCall inspectables
        val functions = state["functions"] as List<InspectableValue>
        val fnDefInspectable = functions.single()
        assertThat(fnDefInspectable.nameFallback).isEqualTo("rcFunctionDefine")
        assertThat(fnDefInspectable["functionId"]).isEqualTo(7)
        assertThat(fnDefInspectable["argVarIds"] as List<Int>).containsExactly(101, 102).inOrder()
        assertThat(fnDefInspectable["operations"] as List<InspectableValue>).hasSize(1)

        val fnCallInspectable = operations.single { it.nameFallback == "rcFunctionCall" }
        assertThat(fnCallInspectable["functionId"]).isEqualTo(7)
        assertThat(fnCallInspectable["args"] as List<Float>).containsExactly(25f, 5f).inOrder()

        // Verify PathData inspectable
        val pathInspectable = operations.single { it.nameFallback == "rcPathData" }
        assertThat(pathInspectable["instanceId"]).isEqualTo(300)
        assertThat(pathInspectable["floatPathLength"]).isEqualTo(4)
    }
}
