/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.node

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RequireViewTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun requireView_returnsView() {
        lateinit var view: View
        lateinit var node: TestModifierNode
        rule.setContent {
            view = LocalView.current
            Box(Modifier.then(TestModifier(onNode = { node = it })).size(1.dp))
        }

        rule.runOnIdle {
            assertNotNull(node)
            val modifierView = node.requireView()
            assertThat(modifierView).isSameInstanceAs(view)
        }
    }

    @Test
    fun requireView_throws_whenDetached() {
        var attach by mutableStateOf(true)
        lateinit var node: TestModifierNode
        rule.setContent {
            Box(
                Modifier.then(if (attach) TestModifier(onNode = { node = it }) else Modifier)
                    .size(1.dp)
            )
        }

        rule.waitForIdle()
        attach = false

        rule.runOnIdle { assertFailsWith<IllegalStateException> { node.requireView() } }
    }

    private data class TestModifier(val onNode: (TestModifierNode) -> Unit) :
        ModifierNodeElement<TestModifierNode>() {
        override fun create(): TestModifierNode {
            return TestModifierNode(onNode)
        }

        override fun update(node: TestModifierNode) {}
    }

    private class TestModifierNode(private val onNode: (TestModifierNode) -> Unit) :
        Modifier.Node() {
        override fun onAttach() {
            onNode(this)
        }
    }
}
