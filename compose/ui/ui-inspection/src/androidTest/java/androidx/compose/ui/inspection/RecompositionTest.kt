/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.view.inspector.WindowInspector
import androidx.compose.ui.inspection.recompositions.DELAY_FOR_STATE_READS
import androidx.compose.ui.inspection.rules.JvmtiRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.RecompositionTestActivity
import androidx.compose.ui.inspection.util.GetAllParametersCommand
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.GetRecompositionStateReadCommand
import androidx.compose.ui.inspection.util.GetUpdateSettingsCommand
import androidx.compose.ui.inspection.util.filter
import androidx.compose.ui.inspection.util.flatten
import androidx.compose.ui.inspection.util.toMap
import androidx.compose.ui.inspection.validators.validate
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Event
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetRecompositionStateReadResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter.Type
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

private const val TRACE_BUTTON_EMPTY_INTERACTIONS =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    at androidx.compose.runtime.Recomposer.<any>(:0)
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.snapshots.SnapshotStateListKt.getReadable(SnapshotStateList.kt:215)
    at kotlin.collections.CollectionsKt___CollectionsKt.lastOrNull(_Collections.kt:519)
    at androidx.compose.material3.ButtonElevation.animateElevation(Button.kt:969)
    at androidx.compose.material3.ButtonElevation.shadowElevation<any>(Button.kt:932)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:52)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(ComposerImpl.kt:1709)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(ComposerImpl.kt:2045)
    at androidx.compose.runtime.ComposerImpl.doCompose<any>(ComposerImpl.kt:2676)
    at androidx.compose.runtime.ComposerImpl.recompose<any>(ComposerImpl.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

private const val TRACE_BUTTON_INTERACTIONS_WITH_PRESS =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    at androidx.compose.runtime.Recomposer.<any>(:0)
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.snapshots.SnapshotStateListKt.getReadable(SnapshotStateList.kt:215)
    at kotlin.collections.CollectionsKt___CollectionsKt.lastOrNull(_Collections.kt:519)
    at androidx.compose.material3.ButtonElevation.animateElevation(Button.kt:969)
    at androidx.compose.material3.ButtonElevation.shadowElevation<any>(Button.kt:932)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.material3.ButtonKt<any>.invoke(:31)
    at androidx.compose.material3.ButtonKt<any>.invoke(:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(ComposerImpl.kt:1709)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(ComposerImpl.kt:2045)
    at androidx.compose.runtime.ComposerImpl.doCompose<any>(ComposerImpl.kt:2676)
    at androidx.compose.runtime.ComposerImpl.recompose<any>(ComposerImpl.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

private const val TRACE_BUTTON_EMPTY_SHADOW_ELEVATION =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    at androidx.compose.runtime.Recomposer.<any>(:0)
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.animation.core.AnimationState.getValue(AnimationState.kt:330)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:52)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(ComposerImpl.kt:1709)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(ComposerImpl.kt:2045)
    at androidx.compose.runtime.ComposerImpl.doCompose<any>(ComposerImpl.kt:2676)
    at androidx.compose.runtime.ComposerImpl.recompose<any>(ComposerImpl.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

private const val TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    at androidx.compose.runtime.Recomposer.<any>(:0)
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.animation.core.AnimationState.getValue(AnimationState.kt:330)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.material3.ButtonKt<any>.invoke(:31)
    at androidx.compose.material3.ButtonKt<any>.invoke(:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(ComposerImpl.kt:1709)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(ComposerImpl.kt:2045)
    at androidx.compose.runtime.ComposerImpl.doCompose<any>(ComposerImpl.kt:2676)
    at androidx.compose.runtime.ComposerImpl.recompose<any>(ComposerImpl.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

private const val TRACE_ITEM_UPDATE_COUNT_STATE =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    at androidx.compose.runtime.Recomposer.<any>(:0)
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:60)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(ComposerImpl.kt:1709)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(ComposerImpl.kt:2045)
    at androidx.compose.runtime.ComposerImpl.doCompose<any>(ComposerImpl.kt:2676)
    at androidx.compose.runtime.ComposerImpl.recompose<any>(ComposerImpl.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

private const val TRACE_ITEM_UPDATE_LIST_STATE =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    at androidx.compose.runtime.Recomposer.<any>(:0)
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.snapshots.SnapshotStateListKt.getReadable(SnapshotStateList.kt:215)
    at kotlin.collections.CollectionsKt___CollectionsKt.joinTo(_Collections.kt:3490)
    at kotlin.collections.CollectionsKt___CollectionsKt.joinToString(_Collections.kt:3510)
    at kotlin.collections.CollectionsKt___CollectionsKt.joinToString<any>(_Collections.kt:3509)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:60)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(ComposerImpl.kt:1709)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(ComposerImpl.kt:2045)
    at androidx.compose.runtime.ComposerImpl.doCompose<any>(ComposerImpl.kt:2676)
    at androidx.compose.runtime.ComposerImpl.recompose<any>(ComposerImpl.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@LargeTest
class RecompositionTest {
    private val rule = createAndroidComposeRule<RecompositionTestActivity>()

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private lateinit var inspectorTester: InspectorTester

    @Before
    fun before() = runBlocking {
        inspectorTester = InspectorTester(inspectorId = "layoutinspector.compose.inspection")
    }

    @After
    fun after() = runBlocking {
        inspectorTester.sendCommand(GetUpdateSettingsCommand())
        inspectorTester.dispose()
    }

    @Test
    fun recomposing(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true, keepRecomposeCounts = false)
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        val rootId = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }.single()
        var composables =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse

        val parameters =
            inspectorTester
                .sendCommand(GetAllParametersCommand(rootId, skipSystemComposables = false))
                .getAllParametersResponse

        if (composables.rootsList.single().nodesList.isEmpty()) {
            error("No nodes returned. Check that the compose inspector was successfully enabled.")
        }

        // Buttons are animated and have unpredictable recompose counts:
        var nodes = Nodes(composables, parameters)
        assertThat(nodes.button1.recomposeCount).isAtLeast(3)
        assertThat(nodes.text1.recomposeCount).isEqualTo(3)
        assertThat(nodes.item1.recomposeCount).isEqualTo(3)
        assertThat(nodes.button2.recomposeCount).isAtLeast(1)
        assertThat(nodes.item2.recomposeCount).isEqualTo(1)
        assertThat(nodes.text2.recomposeCount).isEqualTo(1)

        // Stop counting but keep the current counts:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = false, keepRecomposeCounts = true)
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        composables =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        nodes = Nodes(composables, parameters)

        assertThat(nodes.button1.recomposeCount).isAtLeast(3)
        assertThat(nodes.text1.recomposeCount).isEqualTo(3)
        assertThat(nodes.item1.recomposeCount).isEqualTo(3)
        assertThat(nodes.button2.recomposeCount).isAtLeast(1)
        assertThat(nodes.item2.recomposeCount).isEqualTo(1)
        assertThat(nodes.text2.recomposeCount).isEqualTo(1)

        // Continue counting:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true, keepRecomposeCounts = true)
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        composables =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        nodes = Nodes(composables, parameters)

        assertThat(nodes.button1.recomposeCount).isAtLeast(4)
        assertThat(nodes.text1.recomposeCount).isEqualTo(4)
        assertThat(nodes.item1.recomposeCount).isEqualTo(4)
        assertThat(nodes.button2.recomposeCount).isAtLeast(2)
        assertThat(nodes.item2.recomposeCount).isEqualTo(2)
        assertThat(nodes.text2.recomposeCount).isEqualTo(2)

        // Continue counting but reset the counts:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true, keepRecomposeCounts = false)
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        composables =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        nodes = Nodes(composables, parameters)

        // Buttons have double recompose counts, as they are
        // recomposed on the down event for the press indication
        assertThat(nodes.button1.recomposeCount).isAtLeast(1)
        assertThat(nodes.text1.recomposeCount).isEqualTo(1)
        assertThat(nodes.item1.recomposeCount).isEqualTo(1)
        assertThat(nodes.button2.recomposeCount).isAtLeast(1)
        assertThat(nodes.text2.recomposeCount).isEqualTo(1)
        assertThat(nodes.item2.recomposeCount).isEqualTo(1)
    }

    @Test
    fun stateReadsEnabledForAll(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = false,
                stateReadKind = StateReadSettings.Kind.ALL,
                maxRecompositions = 3,
            )
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        val rootId = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }.single()
        val composables =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        val parameters =
            inspectorTester
                .sendCommand(GetAllParametersCommand(rootId, skipSystemComposables = false))
                .getAllParametersResponse
        val nodes = Nodes(composables, parameters)

        // Buttons are animated and have unpredictable recompose counts:
        assertThat(nodes.button1.recomposeCount).isAtLeast(8)
        assertThat(nodes.item1.recomposeCount).isEqualTo(8)
        val highRecomposition = nodes.button1.recomposeCount

        var reads = inspectorTester.getStateReads(nodes.button1.anchorHash, highRecomposition)
        validate(reads, nodes.button1.anchorHash) {
            recomposition(highRecomposition) {
                read {
                    value(Type.ITERABLE, "List[0]")
                    trace(TRACE_BUTTON_EMPTY_INTERACTIONS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_EMPTY_SHADOW_ELEVATION)
                }
            }
        }

        reads = inspectorTester.getStateReads(nodes.button1.anchorHash, highRecomposition - 1)
        validate(reads, nodes.button1.anchorHash) {
            recomposition(highRecomposition - 1) {
                read {
                    value(Type.ITERABLE, "List[1]") {
                        parameter("[0]", Type.STRING, "Press") {
                            parameter("pressPosition", Type.STRING, "Offset") {
                                parameter("x", Type.DIMENSION_DP, 58.66f, 0.01f)
                                parameter("y", Type.DIMENSION_DP, 20f, 0.01f)
                            }
                        }
                    }
                    trace(TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                }
            }
        }

        // Request the state reads for item1 for recomposition 4.
        // The state reads for recomposition 1..5 should have been discarded.
        // The first available recomposition with state reads is recomposition 6.
        reads = inspectorTester.getStateReads(nodes.item1.anchorHash, 4)
        validate(reads, nodes.item1.anchorHash) {
            recomposition(6) {
                read {
                    value(Type.INT32, 6)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
        }

        // Stop observing:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.NONE,
            )
        )

        // Start observing:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.ALL,
                maxRecompositions = 3,
                keepRecomposeCounts = false,
            )
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        reads = inspectorTester.getStateReads(nodes.item1.anchorHash, 1)
        assertThat(reads.anchorHash).isEqualTo(nodes.item1.anchorHash)
        assertThat(reads.read.recompositionNumber).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun stateReadsOnDemand(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true, keepRecomposeCounts = false)
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        val rootId = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }.single()
        var composable =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        val parameters =
            inspectorTester
                .sendCommand(GetAllParametersCommand(rootId, skipSystemComposables = false))
                .getAllParametersResponse
        var nodes = Nodes(composable, parameters)
        assertThat(nodes.button1.recomposeCount).isAtLeast(1)
        assertThat(nodes.text1.recomposeCount).isEqualTo(1)
        assertThat(nodes.item1.recomposeCount).isEqualTo(1)

        // Start observing item1
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.BY_ID,
                composableToObserve = listOf(nodes.item1.anchorHash),
                maxRecompositions = 3,
            )
        )

        // Perform another click to receive data via an event:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        // An event should be processed and have the data for the recomposition associated with
        // the last button click.
        var event = receive(nodes.item1.anchorHash)
        validate(event.stateReadEvent, nodes.item1.anchorHash) {
            recomposition(2) {
                read {
                    value(Type.INT32, 2)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
        }

        // If we click 4 more times, we should receive another event:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        // Get and check the content of the event with data for 3 clicks.
        event = receive(nodes.item1.anchorHash)
        validate(event.stateReadEvent, nodes.item1.anchorHash) {
            recomposition(3) {
                read {
                    value(Type.INT32, 3)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
            recomposition(4) {
                read {
                    value(Type.INT32, 4)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
            recomposition(5) {
                read {
                    value(Type.INT32, 5)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
        }

        // Click 2 more times.
        // This is not enough for an event to be sent, but we should be able to get the data.
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        // Delay enough time for an event to be sent:
        delay(DELAY_FOR_STATE_READS * 2)
        assertThat(inspectorTester.channel.isEmpty).isTrue()

        // Get state reads for recomposition 6:
        var reads = inspectorTester.getStateReads(nodes.item1.anchorHash, 6)
        validate(reads, nodes.item1.anchorHash) {
            recomposition(6) {
                read {
                    value(Type.INT32, 6)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
        }

        // Get state reads for recomposition 7:
        reads = inspectorTester.getStateReads(nodes.item1.anchorHash, 7)
        validate(reads, nodes.item1.anchorHash) {
            recomposition(7) {
                read {
                    value(Type.INT32, 7)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                }
                read {
                    value(Type.ITERABLE, "List[6]") {
                        parameter("[0]", Type.STRING, "a")
                        parameter("[1]", Type.STRING, "b")
                        parameter("[2]", Type.STRING, "c")
                        parameter("[3]", Type.STRING, "d")
                        parameter("[4]", Type.STRING, "e")
                    }
                    trace(TRACE_ITEM_UPDATE_LIST_STATE)
                }
            }
        }

        // Click 2 more times.
        // Again: This is not enough for an event to be sent.
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        // Delay enough time for an event to be sent:
        delay(DELAY_FOR_STATE_READS * 2)
        assertThat(inspectorTester.channel.isEmpty).isTrue()

        // A 3rd click should cause another event to be sent:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        event = receive(nodes.item1.anchorHash)
        val recompositions = event.stateReadEvent.readList.map { it.recompositionNumber }
        assertThat(recompositions).containsExactly(8, 9, 10)

        composable =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        nodes = Nodes(composable, parameters)

        // Now request state reads for a different composable:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.BY_ID,
                composableToObserve = listOf(nodes.button1.anchorHash),
                maxRecompositions = 3,
            )
        )

        // Perform another click to receive data via an event:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        // An event should be processed and have the data for the recomposition associated with
        // the last button click.
        event = receive(nodes.button1.anchorHash)
        validate(event.stateReadEvent, nodes.button1.anchorHash) {
            recomposition(nodes.button1.recomposeCount + 1) {
                read {
                    value(Type.ITERABLE, "List[1]") {
                        parameter("[0]", Type.STRING, "Press") {
                            parameter("pressPosition", Type.STRING, "Offset") {
                                parameter("x", Type.DIMENSION_DP, 58.67f)
                                parameter("y", Type.DIMENSION_DP, 20.0f)
                            }
                        }
                    }
                    invalidated(true)
                    trace(TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                }
            }
        }

        // Stop observing
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.BY_ID,
                maxRecompositions = 3,
            )
        )

        // Restart observing
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.BY_ID,
                composableToObserve = listOf(nodes.item1.anchorHash),
                maxRecompositions = 3,
            )
        )

        // Perform another click to receive data via an event:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        event = receive(nodes.item1.anchorHash)
        assertThat(event.stateReadEvent.readCount).isEqualTo(1)
        assertThat(event.stateReadEvent.readList.single().recompositionNumber).isEqualTo(13)
    }

    // TODO(b/441069857): Investigate if we can use a TestScope instead of waiting in the test
    private suspend fun receive(anchorHash: Int): Event {
        repeat(100) {
            val result = inspectorTester.channel.tryReceive()
            if (result.isSuccess) {
                val event = Event.parseFrom(result.getOrThrow())
                if (event.stateReadEvent.anchorHash == anchorHash) {
                    return event
                }
            }
            delay(100.milliseconds)
        }
        error("Timeout waiting for event")
    }

    private suspend fun InspectorTester.getStateReads(
        anchorHash: Int,
        recomposition: Int,
    ): GetRecompositionStateReadResponse =
        sendCommand(GetRecompositionStateReadCommand(anchorHash, recomposition))
            .getRecompositionStateReadResponse

    private class Nodes(composables: GetComposablesResponse, parameters: GetAllParametersResponse) {
        val button1 = nodeWithText("Button", composables, parameters) { it == "Click row 1" }
        val button2 = nodeWithText("Button", composables, parameters) { it == "Click row 2" }
        val text1 =
            nodeWithText("Text", composables, parameters) { it.startsWith("Row 1 click count: ") }
        val text2 =
            nodeWithText("Text", composables, parameters) { it.startsWith("Row 2 click count: ") }
        private val items = composables.filter("Item")
        val item1 = items[0]
        val item2 = items[1]

        private fun nodeWithText(
            name: String,
            composables: GetComposablesResponse,
            parameters: GetAllParametersResponse,
            predicate: (String) -> Boolean,
        ): ComposableNode {
            val strings = composables.stringsList.toMap()
            return composables.rootsList
                .single()
                .nodesList
                .flatMap { it.flatten() }
                .single { strings[it.name] == name && hasText(it, parameters, predicate) }
        }

        private fun hasText(
            node: ComposableNode,
            parameters: GetAllParametersResponse,
            predicate: (String) -> Boolean,
        ): Boolean {
            val strings = parameters.stringsList.toMap()
            val group = parameters.parameterGroupsList.single { it.composableId == node.id }
            if (
                group.parameterList.any {
                    strings[it.name] == "text" &&
                        it.type == Type.STRING &&
                        predicate(strings[it.int32Value]!!)
                }
            ) {
                return true
            }
            return node.childrenList.any { hasText(it, parameters, predicate) }
        }
    }
}
