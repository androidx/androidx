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
import androidx.compose.ui.inspection.validators.DoNotChangeMayRequireChangesInAndroidStudio
import androidx.compose.ui.inspection.validators.validate
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
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

@DoNotChangeMayRequireChangesInAndroidStudio
private const val TRACE_BUTTON_EMPTY_INTERACTIONS =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.snapshots.SnapshotStateListKt.getReadable(SnapshotStateList.kt:215)
    at kotlin.collections.CollectionsKt___CollectionsKt.lastOrNull(_Collections.kt:519)
    at androidx.compose.material3.ButtonElevation.animateElevation(Button.kt:969)
    at androidx.compose.material3.ButtonElevation.shadowElevation<any>(Button.kt:932)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:52)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_BUTTON_EMPTY_INTERACTIONS =
    """
    at androidx.compose.material3.ButtonElevation.animateElevation(Button.kt:969)
    at androidx.compose.material3.ButtonElevation.shadowElevation<any>(Button.kt:932)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:52)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val TRACE_BUTTON_INTERACTIONS_WITH_PRESS =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.snapshots.SnapshotStateListKt.getReadable(SnapshotStateList.kt:215)
    at kotlin.collections.CollectionsKt___CollectionsKt.lastOrNull(_Collections.kt:519)
    at androidx.compose.material3.ButtonElevation.animateElevation(Button.kt:969)
    at androidx.compose.material3.ButtonElevation.shadowElevation<any>(Button.kt:932)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:31)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_BUTTON_INTERACTIONS_WITH_PRESS =
    """
    at androidx.compose.material3.ButtonElevation.animateElevation(Button.kt:969)
    at androidx.compose.material3.ButtonElevation.shadowElevation<any>(Button.kt:932)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:31)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:10)
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val TRACE_BUTTON_EMPTY_SHADOW_ELEVATION =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.animation.core.AnimationState.getValue(AnimationState.kt:330)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:52)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_BUTTON_EMPTY_SHADOW_ELEVATION =
    """
    at androidx.compose.animation.core.AnimationState.getValue(AnimationState.kt:330)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:52)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.animation.core.AnimationState.getValue(AnimationState.kt:330)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:31)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS =
    """
    at androidx.compose.animation.core.AnimationState.getValue(AnimationState.kt:330)
    at androidx.compose.material3.ButtonKt.Button(Button.kt:124)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:31)
    at androidx.compose.material3.ButtonKt<any>.invoke(<any>:10)
    """

private const val TRACE_ITEM_UPDATE_COUNT_STATE =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:60)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_ITEM_UPDATE_COUNT_STATE =
    """
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:60)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val TRACE_ITEM_UPDATE_LIST_STATE =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.snapshots.SnapshotStateListKt.getReadable(SnapshotStateList.kt:215)
    at kotlin.collections.CollectionsKt___CollectionsKt.joinTo(_Collections.kt:3490)
    at kotlin.collections.CollectionsKt___CollectionsKt.joinToString(_Collections.kt:3510)
    at kotlin.collections.CollectionsKt___CollectionsKt.joinToString<any>(_Collections.kt:3509)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:60)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_ITEM_UPDATE_LIST_STATE =
    """
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:60)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val TRACE_ANOTHER_ITEM =
    """
    at androidx.compose.runtime.CompositionImpl.recordReadOf(Composition.kt:1015)
    at androidx.compose.runtime.Recomposer.readObserverOf<any>(Recomposer.kt:1519)
    ...
    at androidx.compose.runtime.Recomposer<any>.invoke(<any>:0)
    at androidx.compose.runtime.snapshots.SnapshotKt.readable(Snapshot.kt:2081)
    at androidx.compose.runtime.SnapshotMutableStateImpl.getValue(SnapshotState.kt:142)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:61)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:61)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.AnotherItem(RecompositionTestActivity.kt:71)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:61)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:196)
    at androidx.compose.runtime.<composer>.recomposeToGroupEnd(<composer>.kt:1709)
    at androidx.compose.runtime.<composer>.skipCurrentGroup(<composer>.kt:2045)
    at androidx.compose.runtime.<composer>.doCompose<any>(<composer>.kt:2676)
    at androidx.compose.runtime.<composer>.recompose<any>(<composer>.kt:2600)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:1076)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:1400)
    ...
    """

@DoNotChangeMayRequireChangesInAndroidStudio
private const val UNFOLDED_TRACE_ANOTHER_ITEM =
    """
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:61)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:61)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.AnotherItem(RecompositionTestActivity.kt:71)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity.Item(RecompositionTestActivity.kt:61)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:12)
    at androidx.compose.ui.inspection.testdata.RecompositionTestActivity<any>.invoke(<any>:10)
    """

@LargeTest
class RecompositionTest {
    private val rule = createAndroidComposeRule<RecompositionTestActivity>(StandardTestDispatcher())

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

        var reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.button1.anchorHash,
                recompositionNumberStart = highRecomposition - 1,
                recompositionNumberEnd = highRecomposition,
                includeExtra = false,
            )
        validate(reads, nodes.button1.anchorHash) {
            recomposition(highRecomposition - 1) {
                read {
                    invalidated(true)
                    valueOptions {
                        value(Type.ITERABLE, "List[1]") {
                            parameter("[0]", Type.STRING, "Press") {
                                parameter("pressPosition", Type.STRING, "Offset") {
                                    parameter("x", Type.DIMENSION_DP, 58.66f, 0.01f)
                                    parameter("y", Type.DIMENSION_DP, 20f, 0.01f)
                                }
                            }
                        }
                        value(Type.ITERABLE, "List[2]") {
                            parameter("[0]", Type.STRING, "Focus")
                            parameter("[1]", Type.STRING, "Press") {
                                parameter("pressPosition", Type.STRING, "Offset") {
                                    parameter("x", Type.DIMENSION_DP, 58.66f, 0.01f)
                                    parameter("y", Type.DIMENSION_DP, 20f, 0.01f)
                                }
                            }
                        }
                    }
                    trace(TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                    folding(UNFOLDED_TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                    folding(UNFOLDED_TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                }
            }
            recomposition(highRecomposition) {
                read {
                    invalidated(true)
                    value(Type.ITERABLE, "List[0]")
                    trace(TRACE_BUTTON_EMPTY_INTERACTIONS)
                    folding(UNFOLDED_TRACE_BUTTON_EMPTY_INTERACTIONS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_EMPTY_SHADOW_ELEVATION)
                    folding(UNFOLDED_TRACE_BUTTON_EMPTY_SHADOW_ELEVATION)
                }
            }
        }

        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.button1.anchorHash,
                recompositionNumberStart = highRecomposition - 3,
                recompositionNumberEnd = highRecomposition - 2,
                includeExtra = true,
            )
        validate(reads, nodes.button1.anchorHash) {
            recomposition(highRecomposition - 3) {
                read {
                    invalidated(true)
                    value(Type.ITERABLE, "List[1]") {
                        parameter("[0]", Type.STRING, "Press") {
                            parameter("pressPosition", Type.STRING, "Offset") {
                                parameter("x", Type.DIMENSION_DP, 58.66f, 0.01f)
                                parameter("y", Type.DIMENSION_DP, 20f, 0.01f)
                            }
                        }
                    }
                    trace(TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                    folding(UNFOLDED_TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                    folding(UNFOLDED_TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                }
            }
            recomposition(highRecomposition - 2) {
                read {
                    invalidated(true)
                    value(Type.ITERABLE, "List[0]")
                    trace(TRACE_BUTTON_EMPTY_INTERACTIONS)
                    folding(UNFOLDED_TRACE_BUTTON_EMPTY_INTERACTIONS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_EMPTY_SHADOW_ELEVATION)
                    folding(UNFOLDED_TRACE_BUTTON_EMPTY_SHADOW_ELEVATION)
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
                keepRecomposeCounts = false,
            )
        )

        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 1,
                includeExtra = true,
            )
        assertThat(reads.anchorHash).isEqualTo(nodes.item1.anchorHash)
        assertThat(reads.readCount).isEqualTo(1)
        assertThat(reads.readList.single().recompositionNumber).isEqualTo(1)

        // The state read record is removed after it is read:
        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 1,
                includeExtra = true,
            )
        assertThat(reads.readCount).isEqualTo(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun stateReadsById(): Unit = runBlocking {
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
            )
        )

        // Perform another click 2 times to observe state reads:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        composable =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        nodes = Nodes(composable, parameters)
        assertThat(nodes.button1.recomposeCount).isAtLeast(3)
        assertThat(nodes.text1.recomposeCount).isEqualTo(3)
        assertThat(nodes.item1.recomposeCount).isEqualTo(3)

        var reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 3,
                includeExtra = true,
            )
        validate(reads, nodes.item1.anchorHash) {
            recomposition(2) {
                read {
                    value(Type.INT32, 2)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                    folding(UNFOLDED_TRACE_ITEM_UPDATE_COUNT_STATE)
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
                    folding(UNFOLDED_TRACE_ITEM_UPDATE_LIST_STATE)
                }
                read {
                    value(Type.INT32, 2)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                    folding(UNFOLDED_TRACE_ITEM_UPDATE_COUNT_STATE)
                }
            }
            recomposition(3) {
                read {
                    value(Type.INT32, 3)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                    folding(UNFOLDED_TRACE_ITEM_UPDATE_COUNT_STATE)
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
                    folding(UNFOLDED_TRACE_ITEM_UPDATE_LIST_STATE)
                }
                read {
                    value(Type.INT32, 3)
                    invalidated(true)
                    trace(TRACE_ITEM_UPDATE_COUNT_STATE)
                    folding(UNFOLDED_TRACE_ITEM_UPDATE_COUNT_STATE)
                }
            }
        }

        // There are no more data left in the cache for item1:
        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 3,
                includeExtra = true,
            )
        assertThat(reads.readCount).isEqualTo(0)

        // No state reads should be available for button1 and text1 (not being observed)
        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 3,
                includeExtra = true,
            )
        assertThat(reads.readCount).isEqualTo(0)
        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.button1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 3,
                includeExtra = true,
            )
        assertThat(reads.readCount).isEqualTo(0)

        // Now request state reads for a different composable:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.BY_ID,
                composableToObserve = listOf(nodes.button1.anchorHash),
            )
        )

        // Perform another click to receive data via an event:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.button1.anchorHash,
                recompositionNumberStart = 3,
                recompositionNumberEnd = 3,
                includeExtra = true,
            )
        validate(reads, nodes.button1.anchorHash) {
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
                    folding(UNFOLDED_TRACE_BUTTON_INTERACTIONS_WITH_PRESS)
                }
                read {
                    value(Type.DIMENSION_DP, 0.0f)
                    trace(TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                    folding(UNFOLDED_TRACE_BUTTON_SHADOW_ELEVATION_DURING_PRESS)
                }
            }
        }

        // Stop observing
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.NONE,
            )
        )

        // Restart observing
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = true,
                stateReadKind = StateReadSettings.Kind.BY_ID,
                composableToObserve = listOf(nodes.item1.anchorHash),
            )
        )

        // Perform another 3 clicks:
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Click row 1").performClick()
        rule.waitForIdle()

        composable =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        nodes = Nodes(composable, parameters)
        assertThat(nodes.button1.recomposeCount).isAtLeast(7)
        assertThat(nodes.text1.recomposeCount).isEqualTo(7)
        assertThat(nodes.item1.recomposeCount).isEqualTo(7)

        // A state read record is available for recomposition 5
        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 5,
                recompositionNumberEnd = 5,
                includeExtra = true,
            )
        assertThat(reads.readCount).isEqualTo(1)
        assertThat(reads.readList.single().recompositionNumber).isEqualTo(5)

        // The state read record is removed after it is read, and we receive the next recomposition
        reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.item1.anchorHash,
                recompositionNumberStart = 5,
                recompositionNumberEnd = 5,
                includeExtra = true,
            )
        assertThat(reads.readCount).isEqualTo(1)
        assertThat(reads.readList.single().recompositionNumber).isEqualTo(6)
    }

    @Test
    fun testEmptyStateReads(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = false,
                stateReadKind = StateReadSettings.Kind.ALL,
            )
        )

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

        val reads =
            inspectorTester.getStateReads(
                anchorHash = nodes.anotherItem1.anchorHash,
                recompositionNumberStart = 1,
                recompositionNumberEnd = 5,
                includeExtra = true,
            )

        validate(reads, nodes.anotherItem1.anchorHash) {
            recomposition(1) {
                read {
                    value(Type.INT32, 1)
                    trace(TRACE_ANOTHER_ITEM)
                    folding(UNFOLDED_TRACE_ANOTHER_ITEM)
                }
            }
            recomposition(2) {}
            recomposition(3) {
                read {
                    value(Type.INT32, 3)
                    trace(TRACE_ANOTHER_ITEM)
                    folding(UNFOLDED_TRACE_ANOTHER_ITEM)
                }
            }
            recomposition(4) {}
            recomposition(5) {
                read {
                    value(Type.INT32, 5)
                    trace(TRACE_ANOTHER_ITEM)
                    folding(UNFOLDED_TRACE_ANOTHER_ITEM)
                }
            }
        }
    }

    private suspend fun InspectorTester.getStateReads(
        anchorHash: Int,
        recompositionNumberStart: Int,
        recompositionNumberEnd: Int,
        includeExtra: Boolean,
    ): GetRecompositionStateReadResponse =
        sendCommand(
                GetRecompositionStateReadCommand(
                    anchorHash,
                    recompositionNumberStart,
                    recompositionNumberEnd,
                    includeExtra,
                )
            )
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
        private val anotherItems = composables.filter("AnotherItem")
        val anotherItem1 = anotherItems[0]

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
