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

package androidx.compose.ui.inspection.test

import android.view.inspector.WindowInspector
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.inspection.test.rules.JvmtiRule
import androidx.compose.ui.inspection.test.rules.createInspectorTester
import androidx.compose.ui.inspection.test.rules.sendCommand
import androidx.compose.ui.inspection.test.testdata.ParameterChangeTestActivity
import androidx.compose.ui.inspection.test.util.GetAllParametersCommand
import androidx.compose.ui.inspection.test.util.GetComposablesCommand
import androidx.compose.ui.inspection.test.util.GetRecompositionStateReadCommand
import androidx.compose.ui.inspection.test.util.GetUpdateSettingsCommand
import androidx.compose.ui.inspection.test.util.UnitApplier
import androidx.compose.ui.inspection.test.util.filter
import androidx.compose.ui.inspection.test.util.toMap
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class ParameterChangeTest {
    private val rule = createAndroidComposeRule<ParameterChangeTestActivity>()

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private lateinit var inspectorTester: InspectorTester

    @Before
    fun before() = runBlocking {
        inspectorTester = createInspectorTester()
    }

    @After
    fun after() = runBlocking {
        inspectorTester.sendCommand(GetUpdateSettingsCommand())
        inspectorTester.dispose()
    }

    /**
     * Parameter changes must be recorded for composables that Studio has not received yet, and the
     * composable called to compute a default parameter value must not hide the parameter changes.
     */
    @Test
    fun parameterChangesBeforeComposablesAreReceived(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = false,
                stateReadKind = StateReadSettings.Kind.ALL,
                includeParameterChanges = true,
            )
        )
        rule.onNodeWithText("Increment").performClick()
        rule.onNodeWithText("Increment").performClick()
        rule.onNodeWithText("Increment").performClick()
        rule.waitForIdle()

        // Receive the composables after the recompositions happened:
        val rootId = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }.single()
        val composables =
            inspectorTester
                .sendCommand(GetComposablesCommand(rootId, skipSystemComposables = false))
                .getComposablesResponse
        inspectorTester.sendCommand(GetAllParametersCommand(rootId, skipSystemComposables = false))
        val label = composables.filter("Label").single()
        assertThat(label.recomposeCount).isEqualTo(3)

        val reads =
            inspectorTester
                .sendCommand(
                    GetRecompositionStateReadCommand(
                        label.anchorHash,
                        recompositionNumberStart = 1,
                        recompositionNumberEnd = 3,
                        includeExtra = false,
                    )
                )
                .getRecompositionStateReadResponse
        val strings = reads.stringsList.toMap()
        val changes =
            reads.readList.associate { group ->
                group.recompositionNumber to group.parameterChangesList.map { strings[it.name] }
            }
        assertThat(changes)
            .containsExactly(1, listOf("count"), 2, listOf("count"), 3, listOf("count"))
    }

    /**
     * Smoke test: observing state reads and parameter changes must not break a composition on a
     * background thread.
     *
     * The keys of this composition are not known to the inspector, since it is not part of the
     * Android views sent to Studio. StateReadHandlerThreadingTest checks the parameter changes
     * found on background threads, but it is not run against the inspector jar.
     */
    @Test
    fun compositionOnBackgroundThreadIsNotBroken(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(
                includeRecomposeCounts = true,
                keepRecomposeCounts = false,
                stateReadKind = StateReadSettings.Kind.ALL,
                includeParameterChanges = true,
            )
        )
        val clock = BroadcastFrameClock()
        val job = Job()
        val context = Dispatchers.Default + clock + job
        val recomposer = Recomposer(context)
        CoroutineScope(context).launch { recomposer.runRecomposeAndApplyChanges() }
        val count = mutableIntStateOf(0)
        val composed = AtomicInteger(-1)
        val composition = Composition(UnitApplier(), recomposer)
        try {
            withContext(Dispatchers.Default) {
                composition.setContent { BackgroundItem(count.intValue) { composed.set(it) } }
            }
            repeat(20) { index ->
                val expected = index + 1
                Snapshot.withMutableSnapshot { count.intValue = expected }
                withTimeout(5_000) {
                    while (composed.get() != expected) {
                        clock.sendFrame(System.nanoTime())
                        delay(10)
                    }
                }
            }
        } finally {
            composition.dispose()
            recomposer.cancel()
            job.cancel()
        }
    }
}

@Composable
private fun BackgroundItem(count: Int, onComposed: (Int) -> Unit) {
    SideEffect { onComposed(count) }
}
