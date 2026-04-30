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

package androidx.compose.ui.inspection

import android.view.inspector.WindowInspector
import androidx.compose.ui.inspection.rules.JvmtiRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.SharedTransitionLayoutTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.filter
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class SharedTransitionLayoutTest {
    private val rule =
        createAndroidComposeRule<SharedTransitionLayoutTestActivity>(StandardTestDispatcher())

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private lateinit var inspectorTester: InspectorTester

    @Before
    fun before() {
        runBlocking {
            inspectorTester = InspectorTester(inspectorId = "layoutinspector.compose.inspection")
        }
    }

    @After
    fun after() {
        inspectorTester.dispose()
    }

    @Test
    fun sharedTransitionLayout(): Unit = runBlocking {
        val rootId = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }.single()
        rule.waitForIdle()
        var composables =
            inspectorTester
                .sendCommand(
                    GetComposablesCommand(rootId, generation = 1, skipSystemComposables = false)
                )
                .getComposablesResponse

        assertThat(composables.filter("SharedTransitionExample")).isNotEmpty()

        rule.onNodeWithTag("MyBox", useUnmergedTree = true).performClick()
        rule.waitForIdle()

        composables =
            inspectorTester
                .sendCommand(
                    GetComposablesCommand(rootId, generation = 2, skipSystemComposables = false)
                )
                .getComposablesResponse

        assertThat(composables.filter("SharedTransitionExample")).isNotEmpty()
    }
}
