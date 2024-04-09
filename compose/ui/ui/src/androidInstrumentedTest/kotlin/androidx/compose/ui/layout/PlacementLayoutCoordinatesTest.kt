/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.layout

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PlacementLayoutCoordinatesTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * The [Placeable.PlacementScope.coordinates] should not be `null` during normal placement
     * and should have the position of the parent that is placing.
     */
    @Test
    fun coordinatesWhilePlacing() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var locationAtPlace: IntOffset? by mutableStateOf(null)
        var boxSize by mutableStateOf(IntSize.Zero)
        var alignment by mutableStateOf(Alignment.Center)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .align(alignment)
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                locations += coordinates
                                locationAtPlace = coordinates
                                    ?.positionInRoot()
                                    ?.round()
                                boxSize = IntSize(p.width, p.height)
                                p.place(0, 0)
                            }
                        }
                        .size(10.dp, 10.dp)
                )
            }
        }
        rule.waitForIdle()
        assertNotNull(locationAtPlace)
        assertNotEquals(IntOffset.Zero, locationAtPlace)
        assertEquals(1, locations.size)

        locationAtPlace = null
        locations.clear()
        alignment = AbsoluteAlignment.TopLeft
        rule.waitForIdle()
        assertNotNull(locationAtPlace)
        assertEquals(IntOffset.Zero, locationAtPlace)
        assertEquals(1, locations.size)

        locationAtPlace = null
        locations.clear()
        alignment = AbsoluteAlignment.BottomRight
        rule.waitForIdle()
        assertNotNull(locationAtPlace)
        assertEquals(1, locations.size)
        val content = rule.activity.findViewById<View>(android.R.id.content)
        val bottomRight = IntOffset(content.width - boxSize.width, content.height - boxSize.height)
        assertEquals(bottomRight, locationAtPlace)
    }

    /**
     * The [Placeable.PlacementScope.coordinates] should not be `null` during normal placement
     * and should have the position of the parent that is placing.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun coordinatesWhilePlacingWithLookaheadScope() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var locationAtPlace: IntOffset? by mutableStateOf(null)
        var boxSize by mutableStateOf(IntSize.Zero)
        var alignment by mutableStateOf(Alignment.Center)
        rule.setContent {
            LookaheadScope {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .align(alignment)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    locations += coordinates
                                    locationAtPlace = coordinates
                                        ?.positionInRoot()
                                        ?.round()
                                    boxSize = IntSize(p.width, p.height)
                                    p.place(0, 0)
                                }
                            }
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }
        rule.waitForIdle()
        locationAtPlace = null
        locations.clear()
        alignment = AbsoluteAlignment.TopLeft
        rule.waitForIdle()
        assertNotNull(locationAtPlace)
        assertEquals(IntOffset.Zero, locationAtPlace)
        assertEquals(2, locations.size)

        locationAtPlace = null
        locations.clear()
        alignment = AbsoluteAlignment.BottomRight
        rule.waitForIdle()
        assertNotNull(locationAtPlace)
        assertEquals(2, locations.size)
        val content = rule.activity.findViewById<View>(android.R.id.content)
        val bottomRight = IntOffset(content.width - boxSize.width, content.height - boxSize.height)
        assertEquals(bottomRight, locationAtPlace)
    }

    /**
     * The [Placeable.PlacementScope.coordinates] should be `null` while calculating the alignment,
     * but should be non-null after the alignment has been calculated.
     */
    @Test
    fun coordinatesWhileAligning() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        rule.setContent {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.alignByBaseline()) {
                    Text("Hello")
                }
                Box(
                    Modifier
                        .alignByBaseline()
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                locations += coordinates
                                p.place(0, 0)
                            }
                        }) {
                    Text("World")
                }
            }
        }
        rule.waitForIdle()
        assertTrue(locations.size > 1)
        assertNull(locations[0])
        assertNotNull(locations.last())
    }

    /**
     * The [Placeable.PlacementScope.coordinates] should be `null` while calculating the alignment,
     * but should be non-null after the alignment has been calculated.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun coordinatesWhileAligningWithLookaheadScope() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        rule.setContent {
            LookaheadScope {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.alignByBaseline()) {
                        Text("Hello")
                    }
                    Box(
                        Modifier
                            .alignByBaseline()
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    locations += coordinates
                                    p.place(0, 0)
                                }
                            }) {
                        Text("World")
                    }
                }
            }
        }
        rule.waitForIdle()
        // There may be a way to make this only 2 invocations for the first pass rather than 3.
        assertEquals(4, locations.size)
        assertNull(locations[0]) // Lookahead pass
        assertNull(locations[1]) // Lookahead pass - second look at alignment line
        assertNotNull(locations[2]) // Lookahead pass placement
        assertNotNull(locations[3]) // Measure pass
    }

    /**
     * The [Placeable.PlacementScope.coordinates] should be `null` while calculating the alignment,
     * but should be non-null after the alignment has been calculated.
     */
    @Test
    fun coordinatesWhileAligningInLayout() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        rule.setContent {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.alignByBaseline()) {
                    Text("Hello")
                }
                val content = @Composable { Text("World") }
                Layout(content, Modifier.alignByBaseline()) { measurables, constraints ->
                    val p = measurables[0].measure(constraints)
                    layout(p.width, p.height) {
                        locations += coordinates.use()
                        p.place(0, 0)
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(2, locations.size)
        assertNull(locations[0])
        assertNotNull(locations[1])
    }

    /**
     * The [Placeable.PlacementScope.coordinates] should be `null` while calculating the alignment,
     * but should be non-null after the alignment has been calculated.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun coordinatesWhileAligningInLookaheadScope() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        rule.setContent {
            LookaheadScope {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.alignByBaseline()) {
                        Text("Hello")
                    }
                    val content = @Composable { Text("World") }
                    Layout(content, Modifier.alignByBaseline()) { measurables, constraints ->
                        val p = measurables[0].measure(constraints)
                        layout(p.width, p.height) {
                            locations += coordinates.use()
                            p.place(0, 0)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(3, locations.size)
        assertNull(locations[0]) // Lookahead pass
        assertNotNull(locations[1]) // Lookahead pass
        assertNotNull(locations[2]) // Measure pass
    }

    @Test
    fun coordinatesInNestedAlignmentLookup() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var textLayoutInvocations = 0
        rule.setContent {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.alignByBaseline()) {
                    Text("Hello", modifier = Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            textLayoutInvocations++
                            p.place(0, 0)
                        }
                    })
                }
                val content = @Composable { Text("World") }
                Layout(content,
                    Modifier
                        .alignByBaseline()
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height + 10) {
                                p[LastBaseline] // invoke alignment
                                p.place(0, 10)
                            }
                        }) { measurables, constraints ->
                    val p = measurables[0].measure(constraints)
                    layout(p.width, p.height) {
                        locations += coordinates.use()
                        p.place(0, 0)
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, textLayoutInvocations)
        assertTrue(locations.size > 1)
        assertNull(locations[0])
        assertNotNull(locations.last())
    }

    @Test
    fun parentCoordinateChangeCausesRelayout() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var offset by mutableStateOf(DpOffset(0.dp, 0.dp))
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.offset(offset.x, offset.y)) {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    locations += coordinates.use()
                                    p.place(0, 0)
                                }
                            }
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, locations.size)

        locations.clear()
        offset = DpOffset(1.dp, 2.dp)
        rule.waitForIdle()

        assertEquals(1, locations.size)
    }

    @Test
    fun grandParentCoordateChangeCausesRelayout() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var offset by mutableStateOf(DpOffset(0.dp, 0.dp))
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.offset(offset.x, offset.y)) {
                    Box {
                        Box(
                            Modifier
                                .layout { measurable, constraints ->
                                    val p = measurable.measure(constraints)
                                    layout(p.width, p.height) {
                                        locations += coordinates.use()
                                        p.place(0, 0)
                                    }
                                }
                                .size(10.dp, 10.dp)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, locations.size)

        locations.clear()
        offset = DpOffset(1.dp, 2.dp)
        rule.waitForIdle()

        assertEquals(1, locations.size)
    }

    @Test
    fun newlyAddedStillUpdated() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var offset by mutableStateOf(DpOffset(0.dp, 0.dp))
        var showContent2 by mutableStateOf(false)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.offset(offset.x, offset.y)) {
                    Box {
                        Box(Modifier.fillMaxSize())
                        if (showContent2) {
                            Box(
                                Modifier
                                    .layout { measurable, constraints ->
                                        val p = measurable.measure(constraints)
                                        layout(p.width, p.height) {
                                            locations += coordinates.use()
                                            p.place(0, 0)
                                        }
                                    }
                                    .size(10.dp, 10.dp)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        showContent2 = true
        rule.waitForIdle()
        assertEquals(1, locations.size)

        locations.clear()
        offset = DpOffset(1.dp, 2.dp)
        rule.waitForIdle()

        assertEquals(1, locations.size)
    }

    @Test
    fun removedStopsUpdating() {
        var readCoordinates by mutableStateOf(true)
        val layoutCalls = mutableStateListOf<LayoutCoordinates?>()
        var offset by mutableStateOf(DpOffset.Zero)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.offset(offset.x, offset.y)) {
                    Box {
                        Box(
                            Modifier
                                .layout { measurable, constraints ->
                                    val p = measurable.measure(constraints)
                                    layout(p.width, p.height) {
                                        layoutCalls +=
                                            if (readCoordinates) coordinates.use() else null
                                        p.place(0, 0)
                                    }
                                }
                                .size(10.dp, 10.dp)
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, layoutCalls.size)

        layoutCalls.clear()
        offset = DpOffset(10.dp, 5.dp)
        rule.waitForIdle()

        assertEquals(1, layoutCalls.size)

        layoutCalls.clear()
        readCoordinates = false
        rule.waitForIdle()
        assertEquals(1, layoutCalls.size)

        layoutCalls.clear()
        offset = DpOffset.Zero
        rule.waitForIdle()

        assertEquals(0, layoutCalls.size)
    }

    /**
     * When a LayoutNode is moved, its usage of coordinates should follow.
     */
    @Test
    fun movedContentNotifies() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var offset1 by mutableStateOf(DpOffset.Zero)
        var offset2 by mutableStateOf(DpOffset.Zero)
        var showInOne by mutableStateOf(true)
        rule.setContent {
            val usingCoordinates = remember {
                movableContentOf {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    locations += coordinates.use()
                                    p.place(0, 0)
                                }
                            }
                            .size(10.dp, 10.dp)
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .size(50.dp)
                        .offset(offset1.x, offset1.y)
                ) {
                    if (showInOne) {
                        usingCoordinates()
                    }
                }
                Box(
                    Modifier
                        .size(50.dp)
                        .offset(offset2.x, offset2.y)
                ) {
                    if (!showInOne) {
                        usingCoordinates()
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, locations.size)
        offset1 = DpOffset(1.dp, 1.dp)
        rule.waitForIdle()
        assertEquals(2, locations.size)
        showInOne = false
        rule.waitForIdle()
        assertEquals(3, locations.size)
        offset2 = DpOffset(1.dp, 1.dp)
        rule.waitForIdle()
        assertEquals(4, locations.size)
    }

    /**
     * When [Placeable.PlacementScope.coordinates] is accessed during placement then changing the
     * layer properties on an ancestor should cause relayout.
     */
    @Test
    fun ancestorLayerChangesCausesPlacement() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var offset by mutableStateOf(Offset.Zero)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.graphicsLayer {
                    translationX = offset.x
                    translationY = offset.y
                }) {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    locations += coordinates.use()
                                    p.place(0, 0)
                                }
                            }
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, locations.size)

        offset = Offset(1f, 2f)
        rule.waitForIdle()
        assertEquals(2, locations.size)
    }

    /**
     * When [Placeable.PlacementScope.coordinates] is accessed during placement then changing the
     * layer properties of the LayoutNode should cause relayout.
     */
    @Test
    fun layerChangesCausesPlacement() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        var offset by mutableStateOf(Offset.Zero)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                locations += coordinates.use()
                                p.place(0, 0)
                            }
                        }
                        .size(10.dp, 10.dp)
                )
            }
        }
        rule.waitForIdle()
        assertEquals(1, locations.size)

        offset = Offset(1f, 2f)
        rule.waitForIdle()
        assertEquals(2, locations.size)
    }

    @Test
    fun viewPositionChangeCausesPlacement() {
        val locations = mutableStateListOf<LayoutCoordinates?>()
        lateinit var composeView: ComposeView
        rule.runOnUiThread {
            val container = FrameLayout(rule.activity)

            composeView = ComposeView(rule.activity).apply {
                setContent {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    locations += coordinates.use()
                                    p.place(0, 0)
                                }
                            }
                            .size(10.dp)
                    )
                }
            }
            container.addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.LEFT
                )
            )
            container.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            rule.activity.setContentView(container)
        }

        rule.waitForIdle()
        locations.clear()

        rule.runOnUiThread {
            val lp = composeView.layoutParams as FrameLayout.LayoutParams
            lp.gravity = Gravity.CENTER
            composeView.layoutParams = lp
        }
        rule.waitForIdle()
        assertEquals(1, locations.size)
    }

    @Test
    fun readingFromMainLayoutPolicyAfterMultipleMoves() {
        var offset by mutableStateOf(0)
        var layoutBlockCalls = 0
        rule.setContent {
            Layout(content = {
                Layout { _, _ ->
                    layout(10, 10) {
                        coordinates?.positionInParent()
                        layoutBlockCalls++
                    }
                }
            }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(offset, 0)
                }
            }
        }

        rule.runOnIdle {
            layoutBlockCalls = 0
            offset = 1
        }

        rule.runOnIdle {
            assertEquals(1, layoutBlockCalls)
            layoutBlockCalls = 0
            offset = 2
        }

        rule.runOnIdle {
            assertEquals(1, layoutBlockCalls)
        }
    }

    @Test
    fun onlyRealPositionReadsTriggerRelayout() {
        var offset by mutableStateOf(0)
        var coordinatesAction: (LayoutCoordinates) -> Unit by mutableStateOf({})
        var layoutBlockCalls = 0
        rule.setContent {
            Layout(content = {
                Layout { _, _ ->
                    layout(10, 10) {
                        coordinates?.let(coordinatesAction)
                        layoutBlockCalls++
                    }
                }
            }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(offset, 0)
                }
            }
        }

        fun assert(
            relayoutExpected: Boolean,
            description: String,
            action: (LayoutCoordinates) -> Unit
        ) {
            coordinatesAction = action
            rule.runOnIdle {
                layoutBlockCalls = 0
                offset = if (offset == 0) 10 else 0
            }
            rule.runOnIdle {
                assertEquals(
                    "Relayout because of `$description` read was " +
                        "${if (!relayoutExpected) " not" else ""} expected, but " +
                        "$layoutBlockCalls calls happened",
                    if (relayoutExpected) 1 else 0,
                    layoutBlockCalls
                )
            }
        }

        assert(relayoutExpected = true, "positionInParent()") { it.positionInParent() }
        assert(relayoutExpected = true, "positionInRoot()") { it.positionInRoot() }
        assert(relayoutExpected = true, "positionInWindow()") { it.positionInWindow() }
        assert(relayoutExpected = true, "boundsInParent()") { it.boundsInParent() }
        assert(relayoutExpected = true, "boundsInRoot()") { it.boundsInRoot() }
        assert(relayoutExpected = true, "boundsInWindow()") { it.boundsInWindow() }

        assert(relayoutExpected = false, "empty") { }
        assert(relayoutExpected = false, "size") { it.size }
        assert(relayoutExpected = false, "isAttached") { it.isAttached }
        assert(relayoutExpected = false, "providedAlignmentLines") { it.providedAlignmentLines }
    }

    @Test
    fun onlyRealPositionReadsTriggerRelayout_inModifier() {
        var offset by mutableStateOf(0)
        var coordinatesAction: (LayoutCoordinates) -> Unit by mutableStateOf({})
        var layoutBlockCalls = 0
        rule.setContent {
            Layout(content = {
                Box(
                    Modifier
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                coordinates?.let(coordinatesAction)
                                layoutBlockCalls++
                                p.place(0, 0)
                            }
                        }
                )
            }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(offset, 0)
                }
            }
        }

        fun assert(
            relayoutExpected: Boolean,
            description: String,
            action: (LayoutCoordinates) -> Unit
        ) {
            coordinatesAction = action
            rule.runOnIdle {
                layoutBlockCalls = 0
                offset = if (offset == 0) 10 else 0
            }
            rule.runOnIdle {
                assertEquals(
                    "Relayout because of `$description` read was " +
                        "${if (!relayoutExpected) " not" else ""} expected, but " +
                        "$layoutBlockCalls calls happened",
                    if (relayoutExpected) 1 else 0,
                    layoutBlockCalls
                )
            }
        }

        assert(relayoutExpected = true, "positionInParent()") { it.positionInParent() }
        assert(relayoutExpected = true, "positionInRoot()") { it.positionInRoot() }
        assert(relayoutExpected = true, "positionInWindow()") { it.positionInWindow() }
        assert(relayoutExpected = true, "boundsInParent()") { it.boundsInParent() }
        assert(relayoutExpected = true, "boundsInRoot()") { it.boundsInRoot() }
        assert(relayoutExpected = true, "boundsInWindow()") { it.boundsInWindow() }

        assert(relayoutExpected = false, "empty") { }
        assert(relayoutExpected = false, "size") { it.size }
        assert(relayoutExpected = false, "isAttached") { it.isAttached }
        assert(relayoutExpected = false, "providedAlignmentLines") { it.providedAlignmentLines }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun onlyRealPositionReadsTriggerRelayout_inLookahead() {
        var offset by mutableStateOf(0)
        var coordinatesAction: (LayoutCoordinates) -> Unit by mutableStateOf({})
        var intermediateLayoutBlockCalls = 0
        rule.setContent {
            LookaheadScope {
                Layout(content = {
                    Box(
                        Modifier
                            .approachLayout({
                                intermediateLayoutBlockCalls <= 20
                            }) { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    coordinates?.let(coordinatesAction)
                                    intermediateLayoutBlockCalls++
                                    p.place(0, 0)
                                }
                            }
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(10, 10) {
                                    // if we don't read the coordinates here as well
                                    // the read of coordinates in intermediate layout could be
                                    // skipped as both passes share the same
                                    // coordinatesAccessedDuringPlacement property.
                                    // filed b/284153462 to track this issue
                                    coordinates?.let(coordinatesAction)
                                    p.place(0, 0)
                                }
                            }
                    )
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(offset, 0)
                    }
                }
            }
        }

        fun assert(
            relayoutExpected: Boolean,
            description: String,
            action: (LayoutCoordinates) -> Unit
        ) {
            coordinatesAction = action
            rule.runOnIdle {
                intermediateLayoutBlockCalls = 0
                offset = if (offset == 0) 10 else 0
            }
            rule.runOnIdle {
                assertEquals(
                    "Relayout because of `$description` read was " +
                        "${if (!relayoutExpected) " not" else ""} expected, but " +
                        "$intermediateLayoutBlockCalls calls happened",
                    if (relayoutExpected) 1 else 0,
                    intermediateLayoutBlockCalls
                )
            }
        }

        assert(relayoutExpected = true, "positionInParent()") { it.positionInParent() }
        assert(relayoutExpected = true, "positionInRoot()") { it.positionInRoot() }
        assert(relayoutExpected = true, "positionInWindow()") { it.positionInWindow() }
        assert(relayoutExpected = true, "boundsInParent()") { it.boundsInParent() }
        assert(relayoutExpected = true, "boundsInRoot()") { it.boundsInRoot() }
        assert(relayoutExpected = true, "boundsInWindow()") { it.boundsInWindow() }

        assert(relayoutExpected = false, "empty") { }
        assert(relayoutExpected = false, "size") { it.size }
        assert(relayoutExpected = false, "isAttached") { it.isAttached }
        assert(relayoutExpected = false, "providedAlignmentLines") { it.providedAlignmentLines }
    }
}

private fun LayoutCoordinates?.use(): LayoutCoordinates? {
    this?.parentCoordinates
    return this
}
