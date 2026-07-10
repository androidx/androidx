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

package androidx.compose.ui

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidComposeViewAccessibilityTraversalTest {

    @get:Rule val rule = createComposeRule()

    private var originalTraversalGroupSortingEnabled = true

    @Before
    fun setUp() {
        originalTraversalGroupSortingEnabled = AndroidComposeUiFlags.isTraversalGroupSortingEnabled
    }

    @After
    fun tearDown() {
        AndroidComposeUiFlags.isTraversalGroupSortingEnabled = originalTraversalGroupSortingEnabled
    }

    @Test
    fun findViewByAccessibilityIdTraversal_doesNotCrash() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val view = View(activity)
        // With the fix applied, this should not crash (no IllegalArgumentException).
        // It should safely return null because the accessibility ID (17) does not match the view.
        val result = AndroidComposeView.findViewByAccessibilityIdTraversal(17, view)
        assertThat(result).isNull()
    }

    @Test
    fun testMergedDescendants_respectsTraversalIndex_sortingEnabled() {
        var androidComposeView: AndroidComposeView? = null
        val parentTag = "parent"
        val text1 = "first"
        val text2 = "second"
        val text3 = "third"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            SimpleRow(
                Modifier.semantics(mergeDescendants = true) { isTraversalGroup = true }
                    .testTag(parentTag)
            ) {
                SimpleText(text1, Modifier.semantics { traversalIndex = 1f })
                SimpleText(text3, Modifier.semantics { traversalIndex = 3f })
                SimpleText(text2, Modifier.semantics { traversalIndex = 2f })
            }
        }

        val delegate =
            ViewCompat.getAccessibilityDelegate(androidComposeView!!)
                as AndroidComposeViewAccessibilityDelegateCompat
        delegate.accessibilityForceEnabledForTesting = true

        val child1Id =
            rule.onNodeWithContentDescription(text1, useUnmergedTree = true).fetchSemanticsNode().id
        val child2Id =
            rule.onNodeWithContentDescription(text2, useUnmergedTree = true).fetchSemanticsNode().id
        val child3Id =
            rule.onNodeWithContentDescription(text3, useUnmergedTree = true).fetchSemanticsNode().id

        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        val parentId = rule.onNodeWithTag(parentTag).fetchSemanticsNode().id

        AndroidComposeUiFlags.isTraversalGroupSortingEnabled = true
        val parentInfo = provider.createAccessibilityNodeInfo(parentId)
        assertChildOrder(parentInfo, listOf(child1Id, child2Id, child3Id))
    }

    @Test
    fun testMergedDescendants_respectsTraversalIndex_sortingDisabled() {
        var androidComposeView: AndroidComposeView? = null
        val parentTag = "parent"
        val text1 = "first"
        val text2 = "second"
        val text3 = "third"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            SimpleRow(
                Modifier.semantics(mergeDescendants = true) { isTraversalGroup = true }
                    .testTag(parentTag)
            ) {
                SimpleText(text1, Modifier.semantics { traversalIndex = 1f })
                SimpleText(text3, Modifier.semantics { traversalIndex = 3f })
                SimpleText(text2, Modifier.semantics { traversalIndex = 2f })
            }
        }

        val delegate =
            ViewCompat.getAccessibilityDelegate(androidComposeView!!)
                as AndroidComposeViewAccessibilityDelegateCompat
        delegate.accessibilityForceEnabledForTesting = true

        val child1Id =
            rule.onNodeWithContentDescription(text1, useUnmergedTree = true).fetchSemanticsNode().id
        val child2Id =
            rule.onNodeWithContentDescription(text2, useUnmergedTree = true).fetchSemanticsNode().id
        val child3Id =
            rule.onNodeWithContentDescription(text3, useUnmergedTree = true).fetchSemanticsNode().id

        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        val parentId = rule.onNodeWithTag(parentTag).fetchSemanticsNode().id

        AndroidComposeUiFlags.isTraversalGroupSortingEnabled = false
        val parentInfo = provider.createAccessibilityNodeInfo(parentId)
        assertChildOrder(parentInfo, listOf(child1Id, child3Id, child2Id))
    }

    @Test
    fun testMergedDescendants_respectsTraversalIndex_complex() {
        var androidComposeView: AndroidComposeView? = null
        val parentTag = "parent"
        val groupTag = "nestedGroup"
        val text1 = "first"
        val text2 = "second"
        val text3 = "third"
        val text4 = "fourth"

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            DynamicRow(
                Modifier.semantics(mergeDescendants = true) { isTraversalGroup = true }
                    .testTag(parentTag)
            ) {
                DynamicRow {
                    DynamicRow(Modifier.semantics { isTraversalGroup = true }.testTag(groupTag)) {
                        SimpleText(text2, Modifier.semantics { traversalIndex = 2f })
                        SimpleText(text1, Modifier.semantics { traversalIndex = 1f })
                    }
                }
                DynamicRow {
                    SimpleText(text4, Modifier.semantics { traversalIndex = 4f })
                    SimpleText(text3, Modifier.semantics { traversalIndex = 3f })
                }
            }
        }

        val delegate =
            ViewCompat.getAccessibilityDelegate(androidComposeView!!)
                as AndroidComposeViewAccessibilityDelegateCompat
        delegate.accessibilityForceEnabledForTesting = true

        val child1Id =
            rule.onNodeWithContentDescription(text1, useUnmergedTree = true).fetchSemanticsNode().id
        val child2Id =
            rule.onNodeWithContentDescription(text2, useUnmergedTree = true).fetchSemanticsNode().id
        val child3Id =
            rule.onNodeWithContentDescription(text3, useUnmergedTree = true).fetchSemanticsNode().id
        val child4Id =
            rule.onNodeWithContentDescription(text4, useUnmergedTree = true).fetchSemanticsNode().id
        val groupId = rule.onNodeWithTag(groupTag, useUnmergedTree = true).fetchSemanticsNode().id

        val provider = delegate.getAccessibilityNodeProvider(androidComposeView!!)
        val parentId = rule.onNodeWithTag(parentTag).fetchSemanticsNode().id

        AndroidComposeUiFlags.isTraversalGroupSortingEnabled = true
        val parentInfo = provider.createAccessibilityNodeInfo(parentId)
        assertChildOrder(parentInfo, listOf(groupId, child3Id, child4Id))

        val groupInfo = provider.createAccessibilityNodeInfo(groupId)
        assertChildOrder(groupInfo, listOf(child2Id, child1Id))
    }

    @Composable
    private fun DynamicRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            var width = 0
            var height = 0
            placeables.forEach {
                width += it.width
                height = maxOf(height, it.height)
            }
            layout(width, height) {
                var x = 0
                placeables.forEach {
                    it.place(x, 0)
                    x += it.width
                }
            }
        }
    }

    @Composable
    private fun SimpleRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(100, 100) {
                var x = 0
                placeables.forEach {
                    it.place(x, 0)
                    x += it.width
                }
            }
        }
    }

    @Composable
    private fun SimpleText(text: String, modifier: Modifier = Modifier) {
        Layout(content = {}, modifier = modifier.semantics { contentDescription = text }) { _, _ ->
            layout(10, 10) {}
        }
    }

    private fun assertChildOrder(parentInfo: AccessibilityNodeInfoCompat?, expectedIds: List<Int>) {
        assertThat(parentInfo).isNotNull()
        assertThat(parentInfo!!.childCount).isEqualTo(expectedIds.size)
        for (i in expectedIds.indices) {
            val child = parentInfo.getChild(i)
            assertThat(child).isNotNull()
            assertThat(getVirtualViewId(child!!)).isEqualTo(expectedIds[i])
        }
    }

    private fun getVirtualViewId(nodeInfo: AccessibilityNodeInfoCompat): Int {
        val info = nodeInfo.unwrap()
        val field = info.javaClass.getDeclaredField("mSourceNodeId")
        field.isAccessible = true
        val sourceNodeId = field.get(info) as Long
        return (sourceNodeId shr 32).toInt()
    }
}
