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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll as hostVerticalScroll
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that component-level touch expressions in the embedded player stop tracking the pointer
 * once an ancestor in the host Compose hierarchy claims the gesture, matching the core
 * `Component.onTouchCancel` semantics (release at the last forwarded position, with no velocity).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerAncestorGestureTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun ancestorConsumesBeforeLock_stopsComponentTouchTracking() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                // Consumes every move from the second one on, on the Main pass, before the
                // touch expression has crossed its slop and locked the drag.
                Box(modifier = Modifier.size(300.dp).consumeMovesFrom(2)) { content() }
            }
        ) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_X)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)
        assertThat(displayedValue()).isEqualTo(0)

        canvas.performTouchInput { down(Offset(20f, 20f)) }
        rule.waitForIdle()
        // First move: below slop, not consumed by the ancestor, so it's forwarded.
        canvas.performTouchInput { moveBy(Offset(2f, 0f)) }
        rule.waitForIdle()
        val beforeSteal = displayedValue()
        assertThat(beforeSteal).isGreaterThan(0)

        // Second move: the ancestor takes the gesture.
        canvas.performTouchInput { moveBy(Offset(3f, 0f)) }
        rule.waitForIdle()
        val atSteal = displayedValue()

        // Later moves must no longer drive the expression.
        canvas.performTouchInput { moveBy(Offset(60f, 0f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(atSteal)
        canvas.performTouchInput { moveBy(Offset(60f, 0f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(atSteal)

        canvas.performTouchInput { up() }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(atSteal)
    }

    @Test
    fun lockedDrag_insideScrollableHost_isNotCancelledByOwnConsumption() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            }
        ) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_Y)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 20f)) }
        canvas.performTouchInput { moveBy(Offset(0f, 40f)) }
        canvas.performTouchInput { moveBy(Offset(0f, 40f)) }
        rule.waitForIdle()
        val midDrag = displayedValue()
        canvas.performTouchInput { moveBy(Offset(0f, 40f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isGreaterThan(midDrag)
        canvas.performTouchInput { up() }
        rule.waitForIdle()

        // The touch expression locked the drag, so the host did not scroll.
        assertThat(hostScrollState.value).isEqualTo(0)
    }

    @Test
    fun dragOnUnusedAxis_insideScrollableHost_releasesAndLetsHostScroll() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            }
        ) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_X)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 200f)) }
        canvas.performTouchInput { moveBy(Offset(0f, -60f)) }
        rule.waitForIdle()
        val afterReject = displayedValue()
        // Horizontal drift after the vertical drag was handed to the host is ignored.
        canvas.performTouchInput { moveBy(Offset(40f, -60f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(afterReject)
        canvas.performTouchInput { up() }
        rule.waitForIdle()

        assertThat(displayedValue()).isEqualTo(afterReject)
        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun ancestorInterceptsOnInitialPass_afterLock_stopsComponentTouchTracking() {
        var intercept = false
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp).interceptOnInitialPassWhen { intercept }) {
                    content()
                }
            }
        ) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_X)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 20f)) }
        // Past the slop along the expression's axis: the expression locks the drag.
        canvas.performTouchInput { moveBy(Offset(40f, 0f)) }
        rule.waitForIdle()
        val beforeIntercept = displayedValue()
        assertThat(beforeIntercept).isGreaterThan(0)

        // The ancestor now intercepts every change on the Initial pass, before the player sees it.
        intercept = true
        canvas.performTouchInput { moveBy(Offset(60f, 0f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(beforeIntercept)
        canvas.performTouchInput { moveBy(Offset(60f, 0f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(beforeIntercept)

        canvas.performTouchInput { up() }
        rule.waitForIdle()
        assertThat(displayedValue()).isEqualTo(beforeIntercept)
    }

    @Test
    fun ancestorInterceptsDownOnInitialPass_componentNeverTracks() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp).interceptOnInitialPassWhen { true }) {
                    content()
                }
            }
        ) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_X)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput {
            down(Offset(20f, 20f))
            moveBy(Offset(40f, 0f))
            moveBy(Offset(40f, 0f))
            up()
        }
        rule.waitForIdle()

        assertThat(displayedValue()).isEqualTo(0)
    }

    @Test
    fun indirectSingleAxisExpression_dragOnOtherAxis_letsHostScroll() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(playComposableWrapper = scrollableHost { hostScrollState = it }) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_X, indirect = true)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 200f)) }
        canvas.performTouchInput { moveBy(Offset(0f, -60f)) }
        canvas.performTouchInput { moveBy(Offset(0f, -60f)) }
        canvas.performTouchInput { up() }
        rule.waitForIdle()

        // The expression reads X through a helper expression, so a vertical drag goes to the host.
        assertThat(hostScrollState.value).isGreaterThan(0)
        assertThat(displayedValue()).isEqualTo(0)
    }

    @Test
    fun indirectSingleAxisExpression_dragOnItsAxis_locksDrag() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(playComposableWrapper = scrollableHost { hostScrollState = it }) {
            TouchValueContent(axis = RemoteContext.FLOAT_TOUCH_POS_X, indirect = true)
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 20f)) }
        canvas.performTouchInput { moveBy(Offset(40f, 0f)) }
        rule.waitForIdle()
        val midDrag = displayedValue()
        assertThat(midDrag).isGreaterThan(0)
        canvas.performTouchInput { moveBy(Offset(40f, 0f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isGreaterThan(midDrag)
        canvas.performTouchInput { up() }
        rule.waitForIdle()

        assertThat(hostScrollState.value).isEqualTo(0)
    }

    @Test
    fun twoAxisExpression_verticalDrag_letsHostScroll() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(playComposableWrapper = scrollableHost { hostScrollState = it }) {
            TouchValueContent(
                axis = RemoteContext.FLOAT_TOUCH_POS_X,
                secondAxis = RemoteContext.FLOAT_TOUCH_POS_Y,
            )
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 200f)) }
        canvas.performTouchInput { moveBy(Offset(0f, -60f)) }
        rule.waitForIdle()
        val atClaim = displayedValue()
        canvas.performTouchInput { moveBy(Offset(0f, -60f)) }
        rule.waitForIdle()
        // The host claimed the drag along its axis: the expression stopped tracking.
        assertThat(displayedValue()).isEqualTo(atClaim)
        canvas.performTouchInput { up() }
        rule.waitForIdle()

        assertThat(hostScrollState.value).isGreaterThan(0)
        assertThat(displayedValue()).isEqualTo(atClaim)
    }

    @Test
    fun twoAxisExpression_horizontalDrag_insideVerticalHost_locksDrag() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(playComposableWrapper = scrollableHost { hostScrollState = it }) {
            TouchValueContent(
                axis = RemoteContext.FLOAT_TOUCH_POS_X,
                secondAxis = RemoteContext.FLOAT_TOUCH_POS_Y,
            )
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 100f)) }
        canvas.performTouchInput { moveBy(Offset(40f, 0f)) }
        rule.waitForIdle()
        val midDrag = displayedValue()
        assertThat(midDrag).isGreaterThan(0)
        // Once locked, the expression follows the drag on both axes and keeps it from the host.
        canvas.performTouchInput { moveBy(Offset(20f, 60f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isGreaterThan(midDrag)
        canvas.performTouchInput { up() }
        rule.waitForIdle()

        assertThat(hostScrollState.value).isEqualTo(0)
    }

    @Test
    fun twoAxisExpression_withoutScrollableHost_followsDiagonalDrag() {
        rule.setRemoteContent {
            TouchValueContent(
                axis = RemoteContext.FLOAT_TOUCH_POS_X,
                secondAxis = RemoteContext.FLOAT_TOUCH_POS_Y,
            )
        }
        val canvas = rule.onNodeWithContentDescription(CANVAS)

        canvas.performTouchInput { down(Offset(20f, 20f)) }
        canvas.performTouchInput { moveBy(Offset(30f, 30f)) }
        rule.waitForIdle()
        val midDrag = displayedValue()
        assertThat(midDrag).isGreaterThan(0)
        canvas.performTouchInput { moveBy(Offset(0f, 50f)) }
        rule.waitForIdle()
        assertThat(displayedValue()).isGreaterThan(midDrag)
        canvas.performTouchInput { up() }
    }

    /** The touch expression's value, shown by a [RemoteText] in the document. */
    private fun displayedValue(): Int =
        rule
            .onNodeWithContentDescription(VALUE, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .first()
            .text
            .toInt()

    /**
     * A canvas with a delta-mode touch expression driven by [axis] (the touch position in px,
     * clamped to [0, 1000]), plus a text showing its value. With [secondAxis] the expression is the
     * sum of both axes. With [indirect], [axis] is read through a helper float expression rather
     * than directly by the touch expression.
     */
    @Composable
    @RemoteComposable
    private fun TouchValueContent(
        axis: Float,
        secondAxis: Float? = null,
        indirect: Boolean = false,
    ) {
        val touchValue = remember { MutableRemoteFloat(0f) }
        RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
            RemoteCanvas(
                modifier =
                    RemoteModifier.size(300.rdp, 250.rdp).semantics {
                        contentDescription = CANVAS.rs
                    }
            ) {
                val doc = remoteComposeCreationState.document
                val outputId =
                    Utils.idFromNan(
                        touchValue.getFloatIdForCreationState(remoteComposeCreationState)
                    )
                doc.buffer.addTouchExpression(
                    outputId,
                    0f,
                    0f,
                    1000f,
                    0f,
                    0,
                    touchExpression(doc, axis, secondAxis, indirect),
                    TouchExpression.STOP_INSTANTLY,
                    null,
                    null,
                )
            }
            RemoteText(
                touchValue.toRemoteInt().toRemoteString(),
                modifier = RemoteModifier.semantics { contentDescription = VALUE.rs },
            )
        }
    }

    private fun touchExpression(
        doc: RemoteComposeWriter,
        axis: Float,
        secondAxis: Float?,
        indirect: Boolean,
    ): FloatArray {
        val source = if (indirect) doc.floatExpression(axis, 1f, Rc.FloatExpression.MUL) else axis
        return if (secondAxis == null) {
            floatArrayOf(source, 1f, Rc.FloatExpression.MUL)
        } else {
            floatArrayOf(source, secondAxis, Rc.FloatExpression.ADD)
        }
    }

    /** Hosts the player at the top of a vertically scrollable column, as a feed card would be. */
    private fun scrollableHost(
        onScrollState: (ScrollState) -> Unit
    ): @Composable (@Composable () -> Unit) -> Unit = { content ->
        val scrollState = rememberScrollState()
        onScrollState(scrollState)
        Column(modifier = Modifier.size(300.dp).hostVerticalScroll(scrollState)) {
            Box(modifier = Modifier.size(300.dp)) { content() }
            Box(modifier = Modifier.size(300.dp))
        }
    }

    private companion object {
        const val CANVAS = "TouchCanvas"
        const val VALUE = "TouchValue"
    }
}

/**
 * A host ancestor that consumes every move of a gesture from the [fromMove]th one on, on the Main
 * pass (after its descendants have seen it), like a scrollable claiming a drag.
 */
private fun Modifier.consumeMovesFrom(fromMove: Int): Modifier =
    pointerInput(fromMove) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var moves = 0
            do {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type == PointerEventType.Move && ++moves >= fromMove) {
                    event.changes.fastForEach { it.consume() }
                }
            } while (event.changes.fastAny { it.pressed })
        }
    }

/**
 * A host ancestor that, while [intercept] returns true, consumes every change on the Initial pass
 * (before its descendants see it), like a parent intercepting a gesture.
 */
private fun Modifier.interceptOnInitialPassWhen(intercept: () -> Boolean): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (intercept()) event.changes.fastForEach { it.consume() }
            }
        }
    }
