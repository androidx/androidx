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

package androidx.compose.ui.semantics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.calculateOuterBoundsInRoot
import androidx.compose.ui.layout.isTransparent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.offset
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tests for the semantics and [LayoutInfo] queries exposed for [ExperimentalComposeUiHostApi]. */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalComposeUiHostApi::class)
class SemanticsHostApiTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var view: AndroidComposeView

    private val semanticsOwner: SemanticsOwner
        get() = view.semanticsOwner

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            view = LocalView.current as AndroidComposeView
            Root(content)
        }
    }

    private fun node(tag: String): SemanticsNode =
        rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()

    @Test
    fun placeOrderAndZIndex() {
        setContent {
            Box(Modifier.testTag("parent"), size = 30) {
                Box(Modifier.testTag("a"))
                Box(Modifier.testTag("b").zIndex(2f))
            }
            NotPlacingBox(Modifier.testTag("notPlacing")) { Box(Modifier.testTag("unplaced")) }
        }

        rule.runOnIdle {
            assertThat(node("a").layoutInfo.placeOrder).isEqualTo(0)
            assertThat(node("b").layoutInfo.placeOrder).isEqualTo(1)
            assertThat(node("a").layoutInfo.zIndex).isEqualTo(0f)
            assertThat(node("b").layoutInfo.zIndex).isEqualTo(2f)
            val unplaced = node("notPlacing").children.single()
            assertThat(unplaced.layoutInfo.isPlaced).isFalse()
            assertThat(unplaced.layoutInfo.placeOrder).isEqualTo(LayoutInfo.NotPlacedPlaceOrder)
        }
    }

    @Test
    fun propertyQueries_unmergedAndMerged() {
        setContent {
            Box(
                Modifier.testTag("merging").semantics(mergeDescendants = true) {
                    contentDescription = "parent"
                }
            ) {
                Box(
                    Modifier.testTag("child").semantics {
                        this[ExtraKey] = "value"
                        stateDescription = "state"
                    }
                )
            }
        }

        rule.runOnIdle {
            val merging = node("merging")
            val unmerged = merging.unmergedConfig
            val merged = merging.mergedConfig

            assertThat(
                    unmerged.anyProperty { key, _ -> key == SemanticsProperties.ContentDescription }
                )
                .isTrue()
            assertThat(
                    unmerged.anyProperty { key, _ -> key == SemanticsProperties.StateDescription }
                )
                .isFalse()
            val unmergedKeys = mutableSetOf<SemanticsPropertyKey<*>>()
            unmerged.forEachProperty { key, _ -> unmergedKeys += key }
            assertThat(unmergedKeys)
                .containsExactly(
                    SemanticsProperties.ContentDescription,
                    SemanticsProperties.TestTag,
                )
            assertThat(unmerged.accessibilityExtraKeys).isEmpty()
            assertThat(unmerged.isMergingSemanticsOfDescendants).isTrue()
            // The unmerged tree never merges, even though the configuration declares merging.
            assertThat(merging.isMergingSemanticsOfDescendants).isFalse()

            assertThat(merged.getOrNull(SemanticsProperties.StateDescription)).isEqualTo("state")
            assertThat(merged.getOrNull(ExtraKey)).isEqualTo("value")
            assertThat(merged.accessibilityExtraKeys).containsExactly(ExtraKey)
            assertThat(merged.hasKeysMissingFrom(unmerged)).isTrue()
            assertThat(unmerged.hasKeysMissingFrom(merged)).isFalse()

            val child = node("child")
            assertThat(child.unmergedConfig.accessibilityExtraKeys).containsExactly(ExtraKey)
            val copy = (child.unmergedConfig as SemanticsConfiguration).copy()
            assertThat(copy.accessibilityExtraKeys).containsExactly(ExtraKey)
            assertThat(ExtraKey.accessibilityExtraKey).isEqualTo("test.extra")
        }
    }

    @Test
    fun getMergedOrNullAndForEachMergedProperty_matchMergedConfig() {
        var state by mutableStateOf("first")
        setContent {
            Box(
                Modifier.testTag("merging").semantics(mergeDescendants = true) {
                    contentDescription = "parent"
                }
            ) {
                Box(
                    Modifier.semantics {
                        contentDescription = "child"
                        stateDescription = state
                    }
                )
                // Nested merging nodes are not merged into their ancestor.
                Box(Modifier.semantics(mergeDescendants = true) { paneTitle = "nested" })
                // Descendants of a clearing node are not merged.
                Box(Modifier.clearAndSetSemantics {}) {
                    Box(Modifier.semantics { heading() })
                }
            }
        }

        val merging = rule.runOnIdle { node("merging") }
        rule.runOnIdle {
            val merged = merging.mergedConfig
            assertThat(merging.getMergedOrNull(SemanticsProperties.ContentDescription))
                .isEqualTo(merged.getOrNull(SemanticsProperties.ContentDescription))
            assertThat(merging.getMergedOrNull(SemanticsProperties.ContentDescription))
                .containsExactly("parent", "child")
                .inOrder()
            assertThat(merging.getMergedOrNull(SemanticsProperties.StateDescription))
                .isEqualTo("first")
            assertThat(merging.getMergedOrNull(SemanticsProperties.PaneTitle)).isNull()
            assertThat(merging.getMergedOrNull(SemanticsProperties.Heading)).isNull()
            assertThat(SemanticsProperties.PaneTitle in merged).isFalse()

            val visited = mutableListOf<SemanticsPropertyKey<*>>()
            merging.forEachMergedProperty { key, _ -> visited += key }
            assertThat(visited)
                .containsAtLeast(
                    SemanticsProperties.ContentDescription,
                    SemanticsProperties.StateDescription,
                )
            assertThat(visited).doesNotContain(SemanticsProperties.PaneTitle)
            assertThat(visited).doesNotContain(SemanticsProperties.Heading)

            var count = 0
            assertThat(
                    merging.anyMergedProperty { _, _ ->
                        count++
                        true
                    }
                )
                .isTrue()
            assertThat(count).isEqualTo(1)
            assertThat(
                    merging.anyMergedProperty { key, _ ->
                        key == SemanticsProperties.StateDescription
                    }
                )
                .isTrue()
            assertThat(merging.anyMergedProperty { key, _ -> key == SemanticsProperties.PaneTitle })
                .isFalse()
        }

        rule.runOnIdle { state = "second" }

        rule.runOnIdle {
            // mergedConfig isn't cached on the node.
            assertThat(merging.mergedConfig.getOrNull(SemanticsProperties.StateDescription))
                .isEqualTo("second")
            assertThat(merging.getMergedOrNull(SemanticsProperties.StateDescription))
                .isEqualTo("second")
        }
    }

    @Test
    fun accessibilityChildren_fakeNodesClearAndSetAndLinkMarkers() {
        setContent {
            Box(
                Modifier.testTag("button").semantics(mergeDescendants = true) {
                    contentDescription = "desc"
                    role = Role.Button
                }
            ) {
                Box(Modifier.testTag("child").semantics { stateDescription = "s" })
                Box(Modifier.semantics { this[SemanticsProperties.LinkTestMarker] = Unit })
            }
            Box(Modifier.testTag("cleared").clearAndSetSemantics { contentDescription = "c" }) {
                Box(Modifier.semantics { contentDescription = "hidden" })
            }
        }

        rule.runOnIdle {
            val button = node("button")
            val children = mutableListOf<SemanticsNode>()
            button.collectAccessibilityChildren(children)
            assertThat(children.map { it.id })
                .containsExactly(
                    button.id + ContentDescriptionFakeNodeIdOffset,
                    node("child").id,
                    button.id + RoleFakeNodeIdOffset,
                )
                .inOrder()

            val visited = mutableListOf<Int>()
            button.forEachAccessibilityChild { visited += it.id }
            assertThat(visited).isEqualTo(children.map { it.id })
            assertThat(button.hasAccessibilityChild()).isTrue()
            assertThat(node("child").hasAccessibilityChild()).isFalse()

            // Appends without disturbing existing entries.
            val withExisting = mutableListOf(node("child"))
            button.collectAccessibilityChildren(withExisting)
            assertThat(withExisting.map { it.id }).isEqualTo(listOf(node("child").id) + visited)

            val cleared = node("cleared")
            val clearedChildren = mutableListOf<SemanticsNode>()
            cleared.collectAccessibilityChildren(clearedChildren)
            assertThat(clearedChildren).isEmpty()
            assertThat(cleared.hasAccessibilityChild()).isFalse()
            assertThat(cleared.children).hasSize(1)

            // Fake nodes use the bounds of the node that owns them.
            val fake = children.first()
            assertThat(fake.touchBoundsInRoot).isEqualTo(button.touchBoundsInRoot)
        }
    }

    @Test
    fun forEachSemanticsAncestor_nearestFirst_fakeNodeVisitsOwnerFirst() {
        setContent {
            Box(Modifier.testTag("outer").semantics { contentDescription = "outer" }) {
                Box(Modifier) {
                    Box(
                        Modifier.testTag("button").semantics(mergeDescendants = true) {
                            contentDescription = "desc"
                        }
                    ) {
                        Box(Modifier.testTag("child").semantics { stateDescription = "s" })
                    }
                }
            }
        }

        rule.runOnIdle {
            val button = node("button")
            val outerId = node("outer").id
            val childAncestors = mutableListOf<Int>()
            node("child").forEachSemanticsAncestor { layoutInfo, _ ->
                childAncestors += layoutInfo.semanticsId
                false
            }
            assertThat(childAncestors.take(2)).containsExactly(button.id, outerId).inOrder()
            assertThat(node("child").forEachSemanticsAncestor { _, _ -> false }).isNull()

            val children = mutableListOf<SemanticsNode>()
            button.collectAccessibilityChildren(children)
            val fake = children.first { it.isFake }
            var firstMerges = false
            val first = fake.forEachSemanticsAncestor { _, config ->
                firstMerges = config.isMergingSemanticsOfDescendants
                true
            }
            assertThat(first?.semanticsId).isEqualTo(button.id)
            assertThat(firstMerges).isTrue()
        }
    }

    @Test
    fun boundsQueries() {
        setContent {
            Box(Modifier.pad(5).testTag("parent"), size = 40) {
                Box(Modifier.pad(3).testTag("child"), size = 10)
                Box(
                    Modifier.pad(3).semantics { shape = RectangleShape }.testTag("shape"),
                    size = 10,
                )
                Box(
                    Modifier.pad(3)
                        .semantics {
                            // A new instance per application can't be matched to its modifier.
                            shape = NewInstanceShape()
                        }
                        .testTag("unmatchedShape"),
                    size = 10,
                )
            }
        }

        rule.runOnIdle {
            val rect = MutableRect(-1f, -1f, -1f, -1f)
            node("child").calculateBoundsInParent(rect)
            assertThat(rect.toRect()).isEqualTo(Rect(3f, 3f, 13f, 13f))

            node("child").layoutInfo.calculateOuterBoundsInRoot(rect)
            assertThat(rect.toRect()).isEqualTo(Rect(5f, 5f, 21f, 21f))

            assertThat(node("shape").calculateShapeBoundsInRoot(rect)).isTrue()
            assertThat(rect.toRect()).isEqualTo(Rect(8f, 8f, 18f, 18f))

            assertThat(node("unmatchedShape").calculateShapeBoundsInRoot(rect)).isTrue()
            assertThat(rect.toRect()).isEqualTo(Rect(5f, 5f, 21f, 21f))

            assertThat(node("child").calculateShapeBoundsInRoot(rect)).isFalse()
            assertThat(rect.toRect()).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun localBoundingBoxOf_emptyAncestorBounds_keepsOffset() {
        lateinit var parentCoordinates: LayoutCoordinates
        lateinit var childCoordinates: LayoutCoordinates
        setContent {
            Layout(
                content = { Box(Modifier.onPlaced { childCoordinates = it }, size = 5) },
                modifier = Modifier.onPlaced { parentCoordinates = it },
            ) { measurables, _ ->
                val placeable = measurables.single().measure(Constraints())
                layout(10, 0) { placeable.place(3, 4) }
            }
        }

        rule.runOnIdle {
            assertThat(childCoordinates.localBoundingBoxOf(parentCoordinates, clipBounds = false))
                .isEqualTo(Rect(-3f, -4f, 7f, -4f))
        }
    }

    @Test
    fun visibilityQueries() {
        setContent {
            Box(Modifier.alpha(0f)) {
                Box(Modifier.testTag("transparent").semantics { contentDescription = "t" })
            }
            Box(Modifier.testTag("visible").semantics { contentDescription = "v" })
            Box(Modifier.testTag("hidden").semantics { hideFromAccessibility() })
            Box(Modifier.testTag("tagOnly"))
            Box(
                Modifier.testTag("link").semantics {
                    contentDescription = "l"
                    this[SemanticsProperties.LinkTestMarker] = Unit
                }
            )
        }

        rule.runOnIdle {
            assertThat(node("transparent").layoutInfo.isTransparent).isTrue()
            assertThat(node("transparent").isHidden).isTrue()
            assertThat(node("transparent").isImportantForAccessibility).isFalse()
            assertThat(node("visible").layoutInfo.isTransparent).isFalse()
            assertThat(node("visible").isHidden).isFalse()
            assertThat(node("visible").isImportantForAccessibility).isTrue()
            assertThat(node("hidden").isHidden).isTrue()
            assertThat(node("hidden").isImportantForAccessibility).isFalse()
            assertThat(node("tagOnly").isImportantForAccessibility).isFalse()
            assertThat(node("link").isImportantForAccessibility).isFalse()
        }
    }

    @Test
    fun hitTest_topMostFirst_earlyExit_lookups() {
        setContent {
            // Semantics hit testing doesn't continue into siblings below a hit, so nest them.
            Box(Modifier.testTag("bottom").semantics { contentDescription = "b" }, size = 200) {
                Box(Modifier.testTag("top").semantics { contentDescription = "t" }, size = 10)
            }
        }

        rule.runOnIdle {
            val bottomId = node("bottom").id
            val topId = node("top").id

            val hits = mutableListOf<Int>()
            semanticsOwner.hitTest(Offset(5f, 5f)) {
                hits += it.semanticsId
                false
            }
            assertThat(hits.take(2)).containsExactly(topId, bottomId).inOrder()
            assertThat(semanticsOwner.hitTest(Offset(150f, 150f)) { true }?.semanticsId)
                .isEqualTo(bottomId)

            var count = 0
            val firstHit =
                semanticsOwner.hitTest(Offset(5f, 5f)) {
                    count++
                    true
                }
            assertThat(count).isEqualTo(1)
            assertThat(semanticsOwner.hitTest(Offset(5f, 5f)) { false }).isNull()
            assertThat(firstHit?.semanticsId).isEqualTo(topId)
            assertThat(
                    firstHit
                        ?.semanticsConfiguration
                        ?.getOrNull(SemanticsProperties.ContentDescription)
                )
                .containsExactly("t")

            assertThat(semanticsOwner[topId]?.id).isEqualTo(topId)
            assertThat(semanticsOwner[topId]?.mergingEnabled).isFalse()
            assertThat(semanticsOwner.getLayoutInfo(topId)?.semanticsId).isEqualTo(topId)
            assertThat(semanticsOwner[Int.MAX_VALUE - 1]).isNull()
            assertThat(semanticsOwner.getLayoutInfo(Int.MAX_VALUE - 1)).isNull()
        }
    }

    @Test
    fun hitTest_inlineLinkMarker_returnsTextNode() {
        setContent {
            Box(Modifier.testTag("text").semantics { contentDescription = "text" }, size = 20) {
                Box(
                    Modifier.semantics { this[SemanticsProperties.LinkTestMarker] = Unit },
                    size = 20,
                )
            }
        }

        rule.runOnIdle {
            assertThat(semanticsOwner.hitTest(Offset(5f, 5f)) { true }?.semanticsId)
                .isEqualTo(node("text").id)
        }
    }

    @Test
    fun forEachUncoveredSemanticsNode_prunesCoveredNodes() {
        setContent {
            Box(Modifier.testTag("covered").semantics { contentDescription = "c" }, size = 10)
            Box(Modifier.testTag("cover").semantics { contentDescription = "x" }, size = 20)
        }

        rule.runOnIdle {
            val rootId = semanticsOwner.unmergedRootSemanticsNode.id
            val visited = mutableMapOf<Int, Rect>()
            semanticsOwner.forEachUncoveredSemanticsNode { node, left, top, right, bottom, _ ->
                visited[node.id] =
                    Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            }
            assertThat(visited).containsKey(rootId)
            assertThat(visited[node("cover").id]).isEqualTo(Rect(0f, 0f, 20f, 20f))
            assertThat(visited).doesNotContainKey(node("covered").id)

            val map = semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(-1) { false }
            assertThat(map.containsKey(-1)).isTrue()
            assertThat(map.containsKey(rootId)).isFalse()
            assertThat(map.containsKey(node("cover").id)).isTrue()
        }
    }

    @Test
    fun accessibilityDelegate_fakeNodesAreNotScreenReaderFocusable() {
        setContent {
            Box(
                Modifier.testTag("image").semantics(mergeDescendants = true) {
                    role = Role.Image
                    contentDescription = "image"
                }
            ) {
                Box(Modifier.semantics { contentDescription = "child" })
            }
        }

        val delegate =
            ViewCompat.getAccessibilityDelegate(view)
                as AndroidComposeViewAccessibilityDelegateCompat
        delegate.accessibilityForceEnabledForTesting = true
        val imageId = rule.runOnIdle { node("image").id }
        rule.runOnIdle {
            val provider = delegate.getAccessibilityNodeProvider(view)
            val roleFake = provider.createAccessibilityNodeInfo(imageId + RoleFakeNodeIdOffset)!!
            val contentDescriptionFake =
                provider.createAccessibilityNodeInfo(imageId + ContentDescriptionFakeNodeIdOffset)!!
            assertThat(roleFake.isScreenReaderFocusable).isFalse()
            assertThat(contentDescriptionFake.isScreenReaderFocusable).isFalse()
            assertThat(roleFake.className?.toString()).isEqualTo("android.view.View")
        }
    }
}

private val ExtraKey =
    SemanticsPropertyKey<String>(
        "Extra",
        accessibilityExtraKey = "test.extra",
        mergePolicy = { parentValue, childValue -> parentValue ?: childValue },
    )

private class NewInstanceShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Rectangle(size.toRect())
}

/** Measures children with unbounded constraints and places them all at the origin. */
@Composable
private fun Root(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

/** A [size] x [size] pixel layout that places its children at the origin, in order. */
@Composable
private fun Box(
    modifier: Modifier = Modifier,
    size: Int = 10,
    content: @Composable () -> Unit = {},
) {
    Layout(content, modifier) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        layout(size, size) { placeables.forEach { it.place(0, 0) } }
    }
}

/** Measures its children but never places them. */
@Composable
private fun NotPlacingBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, _ ->
        measurables.forEach { it.measure(Constraints()) }
        layout(10, 10) {}
    }
}

/** Adds [padding] pixels on every side. */
private fun Modifier.pad(padding: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.offset(-2 * padding, -2 * padding))
    layout(placeable.width + 2 * padding, placeable.height + 2 * padding) {
        placeable.place(padding, padding)
    }
}
