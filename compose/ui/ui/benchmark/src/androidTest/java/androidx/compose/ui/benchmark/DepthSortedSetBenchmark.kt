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

package androidx.compose.ui.benchmark

import androidx.activity.compose.setContent
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.benchmark.LayoutNodeModifierBenchmark.SimpleAndroidBenchmarkRule
import androidx.compose.ui.node.DepthSortedSet
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Measures [DepthSortedSet], the list of nodes waiting to be measured or laid out again. It uses
 * the 255 nodes of a real composed tree, added in a shuffled order so that the set has to sort them
 * by depth. The results also show how many objects each run allocates, which should be zero.
 */
@LargeTest
@RunWith(JUnit4::class)
class DepthSortedSetBenchmark {

    @get:Rule val rule = SimpleAndroidBenchmarkRule()

    private lateinit var nodes: Array<LayoutNode>

    @Before
    fun setup() {
        var view: AndroidComposeView? = null
        rule.activityTestRule.runOnUiThread {
            rule.activityTestRule.activity.setContent {
                view = LocalView.current as AndroidComposeView
                Tree(depth = 7)
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        rule.activityTestRule.runOnUiThread {
            val list = mutableListOf<LayoutNode>()
            collect(view!!.root, list)
            // Mix up the depths so the set has to put the nodes back in order.
            list.shuffle(kotlin.random.Random(0))
            nodes = list.toTypedArray()
        }
    }

    @Test
    fun addAndPopAll() {
        val set = DepthSortedSet(extraAssertions = false, membershipBit = 1)
        val nodes = nodes
        rule.benchmarkRule.measureRepeatedOnMainThread {
            for (i in nodes.indices) set.add(nodes[i])
            while (set.isNotEmpty()) set.pop()
        }
    }

    @Test
    fun addRemoveHalfAndPopAll() {
        val set = DepthSortedSet(extraAssertions = false, membershipBit = 1)
        val nodes = nodes
        rule.benchmarkRule.measureRepeatedOnMainThread {
            for (i in nodes.indices) set.add(nodes[i])
            var i = 0
            while (i < nodes.size) {
                set.remove(nodes[i])
                i += 2
            }
            while (set.isNotEmpty()) set.pop()
        }
    }

    @Test
    fun contains() {
        val set = DepthSortedSet(extraAssertions = false, membershipBit = 1)
        val nodes = nodes
        var i = 0
        while (i < nodes.size) {
            set.add(nodes[i])
            i += 2
        }
        var count = 0
        rule.benchmarkRule.measureRepeatedOnMainThread {
            for (j in nodes.indices) {
                if (set.contains(nodes[j])) count++
            }
        }
        check(count > 0)
    }

    private fun collect(node: LayoutNode, out: MutableList<LayoutNode>) {
        out += node
        node.children.forEach { collect(it, out) }
    }
}

@Composable
private fun Tree(depth: Int) {
    Box {
        if (depth > 0) {
            Tree(depth - 1)
            Tree(depth - 1)
        }
    }
}
