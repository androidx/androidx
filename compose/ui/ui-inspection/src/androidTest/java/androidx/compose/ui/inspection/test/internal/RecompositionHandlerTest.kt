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

package androidx.compose.ui.inspection.test.internal

import android.annotation.SuppressLint
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.IdentifiableRecomposeScope
import androidx.compose.ui.inspection.inspector.InlineClassConverter
import androidx.compose.ui.inspection.recompositions.StateReadHandler
import androidx.compose.ui.inspection.test.rules.JvmtiRule
import androidx.compose.ui.inspection.test.testdata.TestActivity
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.inspection.DefaultArtTooling
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
@OptIn(ComposeToolingApi::class)
class RecompositionHandlerTest {
    private val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private val anchorMap = AnchorMap()
    private val recorder = CompositionRecorder(anchorMap)
    private lateinit var handler: StateReadHandler
    private val debugInspectorInfoEnabled = isDebugInspectorInfoEnabled

    @SuppressLint("RestrictedApi")
    @Before
    fun before() {
        handler =
            StateReadHandler(DefaultArtTooling(INSPECTOR_ID), anchorMap, InlineClassConverter())
        handler.changeCollectionMode(
            includeRecomposeCounts = true,
            keepRecomposeCounts = false,
            settings = ALL_STATE_READS,
        )
    }

    @After
    fun after() {
        handler.dispose()
        isDebugInspectorInfoEnabled = debugInspectorInfoEnabled
    }

    // Regression test for b/580889181: the compiler calls skipToGroupEnd() to skip the defaults
    // group of a restarted composable, while its body still executes. That must not be counted as
    // a recomposition skip (or drop state reads), while real skips must still be counted.
    @Test
    fun recomposingAndSkippingWithComposableDefaultParameter() {
        val parentState = mutableIntStateOf(0)
        val withDefaultsState = mutableIntStateOf(0)
        val withoutDefaultsState = mutableIntStateOf(0)

        rule.runOnUiThread {
            isDebugInspectorInfoEnabled = true
            rule.activity.setContent {
                Column {
                    Value(parentState.intValue)
                    WithDefaults(withDefaultsState, recorder)
                    WithoutDefaults(withoutDefaultsState, recorder, "x")
                }
            }
        }
        rule.waitForIdle()

        // Ensure any main-thread coroutines installing the composition observer have run, then
        // reset counts from initial composition while keeping the observer attached:
        rule.runOnIdle {}
        rule.runOnIdle {
            handler.changeCollectionMode(
                includeRecomposeCounts = true,
                keepRecomposeCounts = false,
                settings = ALL_STATE_READS,
            )
        }

        // Trigger 3 self-recompositions of each composable:
        for (step in 1..3) {
            Snapshot.withMutableSnapshot {
                withDefaultsState.intValue = step
                withoutDefaultsState.intValue = step
            }
            rule.waitForIdle()
        }

        // Trigger 2 parent recompositions where both children skip:
        for (step in 1..2) {
            Snapshot.withMutableSnapshot { parentState.intValue = step }
            rule.waitForIdle()
        }

        rule.runOnIdle {
            for (name in listOf("WithDefaults", "WithoutDefaults")) {
                val anchorHash = recorder.anchorHash(name)
                val counts = handler.getCounts(anchorHash)
                assertThat(counts).isNotNull()
                assertWithMessage("$name count").that(counts!!.count).isEqualTo(3)
                assertWithMessage("$name skips").that(counts.skips).isEqualTo(2)

                val reads = handler.getReadsAndRemove(anchorHash, 1, 3, includeExtra = false)
                assertWithMessage("$name recompositions with reads")
                    .that(reads.map { it.recomposition })
                    .containsExactly(1, 2, 3)
                    .inOrder()
                assertWithMessage("$name reads per recomposition")
                    .that(reads.map { it.reads.size })
                    .containsExactly(1, 1, 1)
            }
        }
    }
}

private val ALL_STATE_READS: StateReadSettings =
    StateReadSettings.newBuilder().setAll(StateReadSettings.All.getDefaultInstance()).build()

@Composable private fun rememberLabel(): String = remember { "x" }

@Composable private fun rememberSuffix(): String = remember { "y" }

// A composable with multiple default parameters, which are calculated in a single defaults group.
@OptIn(ComposeToolingApi::class)
@Composable
private fun WithDefaults(
    state: IntState,
    recorder: CompositionRecorder,
    label: String = rememberLabel(),
    suffix: String = rememberSuffix(),
) {
    val c = state.intValue
    val data = currentComposer.compositionData
    val scope = currentRecomposeScope
    SideEffect {
        recorder.record("WithDefaults", c, data, (scope as IdentifiableRecomposeScope).identity)
    }
    Value(c + label.length + suffix.length)
}

@OptIn(ComposeToolingApi::class)
@Composable
private fun WithoutDefaults(state: IntState, recorder: CompositionRecorder, label: String) {
    val c = state.intValue
    val data = currentComposer.compositionData
    val scope = currentRecomposeScope
    SideEffect {
        recorder.record("WithoutDefaults", c, data, (scope as IdentifiableRecomposeScope).identity)
    }
    Value(c + label.length)
}
