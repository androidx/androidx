/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.OverlappingTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.isAncestorOf
import androidx.compose.ui.inspection.util.nodes
import androidx.compose.ui.inspection.util.toMap
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode.Flags
import org.junit.Rule
import org.junit.Test

@LargeTest
class OverlappingTest {
    @get:Rule val rule = ComposeInspectionRule(OverlappingTestActivity::class)

    @Test
    fun testDrawFlag(): Unit = runBlocking {
        val response =
            rule.inspectorTester
                .sendCommand(
                    GetComposablesCommand(rootViewId = rule.rootId, skipSystemComposables = false)
                )
                .getComposablesResponse
        val strings = response.stringsList.toMap()
        val composables = response.nodes()

        // Exactly 4 composables are contributing to drawing in this test app.
        // All other composables should be filtered out by the HAS_DRAW_MODIFIER flag.
        val withChildDrawModifiers =
            composables
                .filter { (it.flags and Flags.SYSTEM_CREATED_VALUE) == 0 }
                .filter { (it.flags and Flags.HAS_CHILD_DRAW_MODIFIER_VALUE) != 0 }
        assertThat(withChildDrawModifiers).hasSize(4)
        assertThat(strings[withChildDrawModifiers[0].name]).isEqualTo("Box")
        assertThat(strings[withChildDrawModifiers[1].name]).isEqualTo("Text")
        assertThat(strings[withChildDrawModifiers[2].name]).isEqualTo("Box")
        assertThat(strings[withChildDrawModifiers[3].name]).isEqualTo("Text")

        // The system nodes with the draw nodes have those composables as ancestors:
        val withDrawModifiers =
            composables.filter { (it.flags and Flags.HAS_DRAW_MODIFIER_VALUE) != 0 }
        assertThat(withDrawModifiers).hasSize(4)
        assertThat(strings[withDrawModifiers[0].name]).isEqualTo("ReusableComposeNode")
        assertThat(strings[withDrawModifiers[1].name]).isEqualTo("ReusableComposeNode")
        assertThat(strings[withDrawModifiers[2].name]).isEqualTo("ReusableComposeNode")
        assertThat(strings[withDrawModifiers[3].name]).isEqualTo("ReusableComposeNode")
        for (i in withDrawModifiers.indices) {
            assertThat(withDrawModifiers[i].isAncestorOf(withChildDrawModifiers[i], response))
                .isTrue()
        }
    }
}
