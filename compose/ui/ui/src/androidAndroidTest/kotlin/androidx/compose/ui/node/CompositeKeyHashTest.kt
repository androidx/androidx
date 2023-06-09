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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class CompositeKeyHashTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nonZeroCompositeKeyHash() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            Box(Modifier.elementOf(node))
        }

        // Act.
        val compositeKeyHash = rule.runOnIdle { node.requireLayoutNode().compositeKeyHash }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun parentAndChildLayoutNodesHaveDifferentCompositeKeyHashes() {
        // Arrange.
        val (parent, child) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Box(Modifier.elementOf(parent)) {
                Box(Modifier.elementOf(child))
            }
        }

        // Act.
        rule.waitForIdle()
        val parentCompositeKeyHash = parent.requireLayoutNode().compositeKeyHash
        val childCompositeKeyHash = child.requireLayoutNode().compositeKeyHash

        // Assert.
        assertThat(parentCompositeKeyHash).isNotEqualTo(childCompositeKeyHash)
    }

    @Test
    fun differentChildrenHaveSameCompositeKeyHashes_Row() {
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
        assertThat(compositeKeyHash1).isEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenWithKeyHaveDifferentCompositeKeyHashes_Row() {
        // Arrange.
        val (node1, node2) = List(3) { object : Modifier.Node() {} }
        rule.setContent {
            Row {
                key(1) {
                    Box(Modifier.elementOf(node1))
                }
                key(2) {
                    Box(Modifier.elementOf(node2))
                }
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
    fun differentChildrenHaveSameCompositeKeyHashes_Box() {
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
        assertThat(compositeKeyHash1).isEqualTo(compositeKeyHash2)
    }

    @Test
    fun differentChildrenWithKeysHaveDifferentCompositeKeyHashes_Box() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box {
                key(1) {
                    Box(Modifier.elementOf(node1))
                }
                key(2) {
                    Box(Modifier.elementOf(node2))
                }
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
                item {
                    Box(Modifier.elementOf(node1))
                }
                item {
                    Box(Modifier.elementOf(node2))
                }
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
            LazyColumn {
                items(2) {
                    Box(Modifier.elementOf(if (it == 0) node1 else node2))
                }
            }
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
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            BasicText(
                text = "text",
                modifier = Modifier.elementOf(node),
            )
        }

        // Act.
        val compositeKeyHash = rule.runOnIdle { node.requireLayoutNode().compositeKeyHash }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun androidView() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            AndroidView(
                factory = { TextView(it) },
                modifier = Modifier.elementOf(node)
            )
        }

        // Act.
        val compositeKeyHash = rule.runOnIdle { node.requireLayoutNode().compositeKeyHash }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun androidView_noOnReset() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            AndroidView(
                factory = { TextView(it) },
                modifier = Modifier.elementOf(node),
                onReset = null
            )
        }

        // Act.
        val compositeKeyHash = rule.runOnIdle { node.requireLayoutNode().compositeKeyHash }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun androidView_withOnReset() {
        // Arrange.
        val node = object : Modifier.Node() {}
        rule.setContent {
            AndroidView(
                factory = { TextView(it) },
                modifier = Modifier.elementOf(node),
                onReset = { }
            )
        }

        // Act.
        val compositeKeyHash = rule.runOnIdle { node.requireLayoutNode().compositeKeyHash }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun Layout1() {
        // Arrange.
        var compositeKeyHash = 0
        rule.setContent {
            Layout1 { compositeKeyHash = it }
        }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun Layout2() { // Add other overloads of Layout here.
        // Arrange.
        var compositeKeyHash = 0
        rule.setContent {
            Layout2 { compositeKeyHash = it }
        }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun Layout3() { // Add other overloads of Layout here.
        // Arrange.
        var compositeKeyHash = 0
        rule.setContent {
            Layout3 { compositeKeyHash = it }
        }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Test
    fun Layout4() { // Add other overloads of Layout here.
        // Arrange.
        var compositeKeyHash = 0
        rule.setContent {
            Layout4 { compositeKeyHash = it }
        }

        // Assert.
        assertThat(compositeKeyHash).isNotEqualTo(0)
    }

    @Composable
    private fun Layout1(onSetCompositionKeyHash: (Int) -> Unit) {
        val node = remember { object : Modifier.Node() {} }
        Layout(
            measurePolicy = { measurables, constraints ->
                measurables.forEach { it.measure(constraints) }
                layout(0, 0) {}
            },
            modifier = Modifier.elementOf(node)
        )
        SideEffect {
            onSetCompositionKeyHash(node.requireLayoutNode().compositeKeyHash)
        }
    }

    @Composable
    private fun Layout2(onSetCompositionKeyHash: (Int) -> Unit) {
        val node = remember { object : Modifier.Node() {} }
        Layout(
            contents = listOf({}, {}),
            measurePolicy = { measurables, constraints ->
                measurables.forEach {
                    it.forEach { measurable ->
                        measurable.measure(constraints)
                    }
                }
                layout(0, 0) {}
            },
            modifier = Modifier.elementOf(node)
        )
        SideEffect {
            onSetCompositionKeyHash.invoke(node.requireLayoutNode().compositeKeyHash)
        }
    }

    @Composable
    private fun Layout3(onSetCompositionKeyHash: (Int) -> Unit) {
        val node = remember { object : Modifier.Node() {} }
        Layout(
            content = { },
            measurePolicy = { measurables, constraints ->
                measurables.forEach { it.measure(constraints) }
                layout(0, 0) {}
            },
            modifier = Modifier.elementOf(node)
        )
        SideEffect {
            onSetCompositionKeyHash.invoke(node.requireLayoutNode().compositeKeyHash)
        }
    }

    @Composable
    private fun Layout4(onSetCompositionKeyHash: (Int) -> Unit) {
        val node = remember { object : Modifier.Node() {} }
        @Suppress("DEPRECATION")
        MultiMeasureLayout(
            content = { },
            measurePolicy = { measurables, constraints ->
                measurables.forEach { it.measure(constraints) }
                layout(0, 0) {}
            },
            modifier = Modifier.elementOf(node)
        )
        SideEffect {
            onSetCompositionKeyHash.invoke(node.requireLayoutNode().compositeKeyHash)
        }
    }
}
