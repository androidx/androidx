/*
 * Copyright 2023 The Android Open Source Project
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

import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CompositeKeyHashTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test fun nonZeroCompositeKeyHash() = compositeKeyHashTest { Box(modifier = it) }

    @Test
    fun parentAndChildLayoutNodesHaveDifferentCompositeKeyHashes() {
        // Arrange.
        val (parent, child) = List(3) { object : Modifier.Node() {} }
        rule.setContent { Box(Modifier.elementOf(parent)) { Box(Modifier.elementOf(child)) } }

        // Act.
        rule.waitForIdle()
        val parentCompositeKeyHash = parent.requireLayoutNode().compositeKeyHash
        val childCompositeKeyHash = child.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(parentCompositeKeyHash).isNotEqualTo(childCompositeKeyHash)
    }

    @Test
    fun differentChildrenHaveDifferentCompositeKeyHashes_Row() {
        // Arrange.
        val (node1, node2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Row {
                Box(Modifier.elementOf(node1))
                Box(Modifier.elementOf(node2))
            }
        }

        // Act.
        rule.waitForIdle()
        val compositeKeyHash1 = node1.requireLayoutNode().compositeKeyHash
        val compositeKeyHash2 = node2.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(compositeKeyHash1).isNotEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenWithKeyHaveDifferentCompositeKeyHashes_Row() {
        // Arrange.
        val (node1, node2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Row {
                key(1) { Box(Modifier.elementOf(node1)) }
                key(2) { Box(Modifier.elementOf(node2)) }
            }
        }

        // Act.
        rule.waitForIdle()
        val compositeKeyHash1 = node1.requireLayoutNode().compositeKeyHash
        val compositeKeyHash2 = node2.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(compositeKeyHash1).isNotEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenHaveDifferentCompositeKeyHashes_Box() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box {
                Box(Modifier.elementOf(node1))
                Box(Modifier.elementOf(node2))
            }
        }

        // Act.
        rule.waitForIdle()
        val compositeKeyHash1 = node1.requireLayoutNode().compositeKeyHash
        val compositeKeyHash2 = node2.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(compositeKeyHash1).isNotEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenWithKeysHaveDifferentCompositeKeyHashes_Box() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box {
                key(1) { Box(Modifier.elementOf(node1)) }
                key(2) { Box(Modifier.elementOf(node2)) }
            }
        }

        // Act.
        rule.waitForIdle()
        val compositeKeyHash1 = node1.requireLayoutNode().compositeKeyHash
        val compositeKeyHash2 = node2.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(compositeKeyHash1).isNotEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenInLazyColumn_item() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            LazyColumn {
                item { Box(Modifier.elementOf(node1)) }
                item { Box(Modifier.elementOf(node2)) }
            }
        }

        // Act.
        rule.waitForIdle()
        val compositeKeyHash1 = node1.requireLayoutNode().compositeKeyHash
        val compositeKeyHash2 = node2.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(compositeKeyHash1).isNotEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenInLazyColumn_Items() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            LazyColumn { items(2) { Box(Modifier.elementOf(if (it == 0) node1 else node2)) } }
        }

        // Act.
        rule.waitForIdle()
        val compositionKeyHash1 = node1.requireLayoutNode().compositeKeyHash
        val compositionKeyHash2 = node2.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(compositionKeyHash1).isNotEqualTo(compositionKeyHash2)
    }

    @Test
    fun text() {
        compositeKeyHashTest { BasicText(text = "text", modifier = it) }
    }

    @Test
    fun androidView() {
        // Arrange.
        compositeKeyHashTest { AndroidView(factory = { TextView(it) }, modifier = it) }
    }

    @Test
    fun androidView_noOnReset() {
        // Arrange.
        compositeKeyHashTest {
            AndroidView(factory = { TextView(it) }, modifier = it, onReset = null)
        }
    }

    @Test
    fun androidView_withOnReset() {
        // Arrange.
        compositeKeyHashTest {
            AndroidView(factory = { TextView(it) }, modifier = it, onReset = {})
        }
    }

    @Test
    fun Layout1() {
        compositeKeyHashTest {
            Layout(
                measurePolicy = { measurables, constraints ->
                    measurables.forEach { it.measure(constraints) }
                    layout(0, 0) {}
                },
                modifier = it,
            )
        }
    }

    @Test
    fun Layout2() { // Add other overloads of Layout here.
        compositeKeyHashTest {
            Layout(
                contents = listOf({}, {}),
                measurePolicy = { measurables, constraints ->
                    measurables.forEach {
                        it.forEach { measurable -> measurable.measure(constraints) }
                    }
                    layout(0, 0) {}
                },
                modifier = it,
            )
        }
    }

    @Test
    fun Layout3() { // Add other overloads of Layout here.
        compositeKeyHashTest {
            Layout(
                content = {},
                measurePolicy = { measurables, constraints ->
                    measurables.forEach { it.measure(constraints) }
                    layout(0, 0) {}
                },
                modifier = it,
            )
        }
    }

    @Test
    fun Layout4() { // Add other overloads of Layout here.
        compositeKeyHashTest {
            @Suppress("DEPRECATION")
            MultiMeasureLayout(
                content = {},
                measurePolicy = { measurables, constraints ->
                    measurables.forEach { it.measure(constraints) }
                    layout(0, 0) {}
                },
                modifier = it,
            )
        }
    }

    @Test
    fun SubcomposeLayout_Content() {
        compositeKeyHashTest { SubcomposeLayout(modifier = it) { _ -> layout(0, 0) {} } }
    }

    fun compositeKeyHashTest(content: @Composable (Modifier) -> Unit) {
        val node = object : Modifier.Node() {}
        var key by mutableStateOf(false)
        var modifier by mutableStateOf(Modifier.elementOf(node))
        rule.setContent { ReusableContent(key) { content(modifier) } }
        var compositeKeyHash = 0
        rule.runOnIdle {
            compositeKeyHash = node.requireLayoutNode().compositeKeyHash
            assertThat(compositeKeyHash).isNotEqualTo(0)
        }

        // detach modifier in case the layout node is recreated on reuse (e.g. AndroidView)
        modifier = Modifier
        rule.waitForIdle()

        key = !key
        modifier = Modifier.elementOf(node)
        rule.runOnIdle {
            val newCompositeKeyHash = node.requireLayoutNode().compositeKeyHash
            assertThat(newCompositeKeyHash).isNotEqualTo(compositeKeyHash)
        }
    }
}
