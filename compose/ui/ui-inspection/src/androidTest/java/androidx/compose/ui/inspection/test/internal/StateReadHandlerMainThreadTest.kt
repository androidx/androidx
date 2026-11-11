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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.ui.inspection.inspector.InlineClassConverter
import androidx.compose.ui.inspection.recompositions.StateReadHandler
import androidx.compose.ui.inspection.test.rules.JvmtiRule
import androidx.compose.ui.inspection.test.testdata.TestActivity
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.inspection.DefaultArtTooling
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

private const val MAIN_THREAD_RECOMPOSITIONS = 10

/**
 * Parameter changes must be recorded correctly when a sub-composition is composed on the main
 * thread during the composition of its parent, inside the composition of an Android view.
 *
 * The events of the 2 compositions interleave on the same thread. The keys of both composables are
 * known, like for composables that Studio has received.
 */
@LargeTest
@OptIn(ComposeToolingApi::class)
class StateReadHandlerMainThreadTest {
    private val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private val anchorMap = AnchorMap()
    private val recorder = CompositionRecorder(anchorMap)
    private lateinit var handler: StateReadHandler

    @SuppressLint("RestrictedApi")
    @Before
    fun before() {
        handler =
            StateReadHandler(DefaultArtTooling(INSPECTOR_ID), anchorMap, InlineClassConverter())
        val settings =
            StateReadSettings.newBuilder()
                .setAll(StateReadSettings.All.newBuilder().setIncludeParameterChanges(true))
                .build()
        handler.changeCollectionMode(
            includeRecomposeCounts = true,
            keepRecomposeCounts = false,
            settings = settings,
        )
    }

    @After
    fun after() {
        handler.dispose()
        isDebugInspectorInfoEnabled = debugInspectorInfoEnabled
    }

    private val debugInspectorInfoEnabled = isDebugInspectorInfoEnabled

    @Test
    fun subCompositionComposedDuringParentComposition() {
        val count = mutableIntStateOf(0)
        rule.runOnUiThread {
            // With debug inspector info enabled, the AndroidComposeView keeps the slot tables used
            // by the inspector to find the composition data, and parameter information is
            // collected. The inspector enables this when it starts.
            isDebugInspectorInfoEnabled = true
            rule.activity.setContent { Outer(count.intValue, recorder) }
        }
        rule.waitForIdle()

        // Register the keys, like Studio does for the composables it has received:
        rule.runOnIdle { recorder.registerKeys("outer", "inner") }

        for (value in 1..MAIN_THREAD_RECOMPOSITIONS) {
            Snapshot.withMutableSnapshot { count.intValue = value }
            rule.waitForIdle()
        }

        rule.runOnIdle {
            assertParameterChanges("outer")
            assertParameterChanges("inner")
        }
    }

    private fun assertParameterChanges(name: String) {
        val anchorHash = recorder.anchorHash(name)
        val reads = handler.getReadsAndRemove(anchorHash, 1, MAIN_THREAD_RECOMPOSITIONS, true)
        val changes = reads.associate { read ->
            read.recomposition to read.parameterChanges.map { it.name to it.value }
        }
        for (recomposition in 1..MAIN_THREAD_RECOMPOSITIONS) {
            assertWithMessage("Recomposition $recomposition of $name: $changes")
                .that(changes[recomposition])
                .isEqualTo(listOf("value" to recomposition))
        }
    }
}
