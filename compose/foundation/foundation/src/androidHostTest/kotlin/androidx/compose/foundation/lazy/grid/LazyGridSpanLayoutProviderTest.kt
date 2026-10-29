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

package androidx.compose.foundation.lazy.grid

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LazyGridSpanLayoutProviderTest {
    private lateinit var observer: SnapshotStateObserver
    private var scopeInvalidated = false
    private val onScopeChanged: (LazyGridSpanLayoutProvider) -> Unit = { scopeInvalidated = true }

    @Before
    fun setUp() {
        scopeInvalidated = false
        observer = SnapshotStateObserver { command -> command() }.apply { start() }
    }

    @After
    fun tearDown() {
        observer.stop()
        observer.clear()
    }

    /**
     * Simulates a `LazyGrid` measure pass:
     * 1. Queries [LazyGridSpanLayoutProvider.getLineIndexOfItem] inside
     *    [Snapshot.withoutReadObservation] (which populates/validates observed states and sets
     *    `needsMeasureObservation = true`).
     * 2. Queries [LazyGridSpanLayoutProvider.getLineConfiguration] across [visibleLineCount] lines
     *    starting at `firstLineIndex` inside [observer]'s `observeReads` scope (which consumes
     *    `needsMeasureObservation` on the first line, exercises the `needsMeasureObservation ==
     *    false` path on subsequent lines, and subscribes [observer] to all accumulated observed
     *    states).
     */
    private fun simulateMeasurePass(
        provider: LazyGridSpanLayoutProvider,
        anchorItemIndex: Int,
        visibleLineCount: Int = 3,
    ): Int {
        Snapshot.notifyObjectsInitialized()
        scopeInvalidated = false
        val firstLineIndex = Snapshot.withoutReadObservation {
            provider.getLineIndexOfItem(anchorItemIndex)
        }
        observer.observeReads(provider, onScopeChanged) {
            repeat(visibleLineCount) { offset ->
                val lineIndex = firstLineIndex + offset
                val config = provider.getLineConfiguration(lineIndex)
                if (config.spans.isEmpty()) return@observeReads
            }
        }
        return firstLineIndex
    }

    /**
     * Verifies that when item spans conditionally read different snapshot states based on item
     * index (e.g. index < 100 vs >= 100), and unrelated snapshot notifications are applied across
     * multiple scroll frames, mutating a state that affects an index subset properly notifies the
     * measure [SnapshotStateObserver], invalidates the span cache, re-subscribes on subsequent
     * measure passes after `observedStates.clear()`, and recalculates line configurations for all
     * lines in the grid.
     */
    @Test
    fun changingIndexDependentStateAcrossFrames_invalidatesCacheAndRelayoutsLines() {
        val totalItems = 3_000
        val slotsPerLine = 3
        val stateA = mutableStateOf(true)
        val stateB = mutableStateOf(false)
        val unrelatedState = mutableStateOf(0)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { index ->
                        if (index < 100) {
                            if (stateA.value) GridItemSpan(3) else GridItemSpan(1)
                        } else {
                            if (stateB.value) GridItemSpan(3) else GridItemSpan(1)
                        }
                    },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine
        Snapshot.notifyObjectsInitialized()

        repeat(10) {
            unrelatedState.value++
            Snapshot.sendApplyNotifications()
            simulateMeasurePass(spanLayoutProvider, anchorItemIndex = totalItems - 1)
            assertThat(scopeInvalidated).isFalse()
        }

        // 1. Mutate off-screen stateA (true -> false): all 3,000 items now have span 1.
        stateA.value = false
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        // Re-run a measure pass anchored at the end of the grid so that getLineIndexOfItem
        // invalidates the cache and re-records both stateA and stateB after observedStates.clear().
        simulateMeasurePass(spanLayoutProvider, anchorItemIndex = totalItems - 1)
        for (lineIndex in 0 until totalItems / slotsPerLine) {
            assertThat(spanLayoutProvider.getLineConfiguration(lineIndex).firstItemIndex)
                .isEqualTo(lineIndex * slotsPerLine)
        }

        // 2. Mutate off-screen stateA a second time (false -> true) to verify re-subscription
        // from the measure pass above.
        stateA.value = true
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        simulateMeasurePass(spanLayoutProvider, anchorItemIndex = totalItems - 1)
        for (lineIndex in 0 until 100) {
            assertThat(spanLayoutProvider.getLineConfiguration(lineIndex).firstItemIndex)
                .isEqualTo(lineIndex)
        }
        assertThat(spanLayoutProvider.getLineConfiguration(100).firstItemIndex).isEqualTo(100)

        // 3. Mutate stateB (false -> true): all 3,000 items now have span 3 (1 item per line).
        stateB.value = true
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = totalItems - 1))
            .isEqualTo(totalItems - 1)
        for (lineIndex in 0 until totalItems) {
            assertThat(spanLayoutProvider.getLineConfiguration(lineIndex).firstItemIndex)
                .isEqualTo(lineIndex)
        }
    }

    /**
     * Verifies that alternating dynamic span states across multiple scroll frames consistently
     * invalidates and recalculates line index mappings without stale bucket pollution.
     */
    @Test
    fun multipleStateChangesAcrossScrollFrames_maintainsConsistentLineCalculations() {
        val totalItems = 500
        val slotsPerLine = 2
        val spanState = mutableStateOf(1)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { index -> GridItemSpan(if (index % 2 == 0) spanState.value else 1) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        // Initial state: spanState=1 (all 1x1 items, 2 items per line)
        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 100)).isEqualTo(50)

        // State changes to 2: even items take 2 columns, odd items start on new lines
        spanState.value = 2
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 49)).isEqualTo(49)
        for (itemIndex in 0 until 50) {
            assertThat(spanLayoutProvider.getLineIndexOfItem(itemIndex)).isEqualTo(itemIndex)
        }

        // State changes back to 1: all 1x1 items again
        spanState.value = 1
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        for (itemIndex in 0 until 50) {
            assertThat(spanLayoutProvider.getLineIndexOfItem(itemIndex))
                .isEqualTo(itemIndex / slotsPerLine)
        }
    }

    /**
     * Verifies that a span lambda reading a [derivedStateOf] properly records dependencies and
     * invalidates the line cache when the underlying state changes.
     */
    @Test
    fun derivedStateInSpanLambda_invalidatesCacheWhenDependencyChanges() {
        val totalItems = 100
        val slotsPerLine = 2
        val baseState = mutableStateOf(true)
        val derivedWide = derivedStateOf { baseState.value }
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { GridItemSpan(if (derivedWide.value) 2 else 1) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(40)

        baseState.value = false
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(20)
    }

    /**
     * Verifies that a span lambda reading a [mutableStateListOf] detects in-place element mutations
     * and invalidates the cached line layout.
     */
    @Test
    fun snapshotStateListInSpanLambda_invalidatesCacheWhenMutatedInPlace() {
        val totalItems = 100
        val slotsPerLine = 2
        val spanList = mutableStateListOf(2, 1)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { index -> GridItemSpan(spanList[index % 2]) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        // Even items have span 2, odd items have span 1 -> each item occupies its own line
        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(40)

        // Mutate list in-place so all items have span 1
        spanList[0] = 1
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(20)
    }

    /**
     * Verifies that a span lambda reading a [mutableStateMapOf] detects in-place entry mutations
     * and invalidates the cached line layout.
     */
    @Test
    fun snapshotStateMapInSpanLambda_invalidatesCacheWhenMutatedInPlace() {
        val totalItems = 100
        val slotsPerLine = 2
        val spanMap = mutableStateMapOf("wide" to true)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { GridItemSpan(if (spanMap["wide"] == true) 2 else 1) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(40)

        spanMap["wide"] = false
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(20)
    }

    /**
     * Verifies that a span lambda reading a [mutableStateSetOf] detects in-place element mutations
     * and invalidates the cached line layout.
     */
    @Test
    fun snapshotStateSetInSpanLambda_invalidatesCacheWhenMutatedInPlace() {
        val totalItems = 100
        val slotsPerLine = 2
        val wideRemainderSet = mutableStateSetOf(0)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { index -> GridItemSpan(if (index % 2 in wideRemainderSet) 2 else 1) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(40)

        wideRemainderSet.remove(0)
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(20)
    }

    /**
     * Verifies that [LazyGridSpanLayoutProvider.getLineConfiguration] independently observes and
     * records snapshot states read during forward line calculations when
     * [LazyGridSpanLayoutProvider.getLineIndexOfItem] at `itemIndex = 0` does not read those
     * states.
     */
    @Test
    fun lineConfigurationObservation_recordsStatesNotReadByGetLineIndexOfItem() {
        val totalItems = 100
        val slotsPerLine = 2
        val tailWideState = mutableStateOf(true)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { index ->
                        // Item 0 and 1 do not read tailWideState; only items >= 2 read it.
                        if (index < slotsPerLine) {
                            GridItemSpan(1)
                        } else {
                            GridItemSpan(if (tailWideState.value) 2 else 1)
                        }
                    },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine
        Snapshot.notifyObjectsInitialized()

        // Calling getLineIndexOfItem(0) only inspects item 0 and does not read tailWideState.
        val firstLine = Snapshot.withoutReadObservation { spanLayoutProvider.getLineIndexOfItem(0) }
        assertThat(firstLine).isEqualTo(0)
        assertThat(spanLayoutProvider.hasObservedStates).isFalse()

        // Walking forward via getLineConfiguration(0..20) reads tailWideState and populates
        // buckets.
        observer.observeReads(spanLayoutProvider, onScopeChanged) {
            for (lineIndex in 0..20) {
                spanLayoutProvider.getLineConfiguration(lineIndex)
            }
        }
        assertThat(spanLayoutProvider.hasObservedStates).isTrue()
        assertThat(spanLayoutProvider.getLineConfiguration(20).firstItemIndex).isEqualTo(21)

        // Run a subsequent measure pass that only queries line 0 (which does not read
        // tailWideState directly), relying on needsMeasureObservation to replay tailWideState.
        simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 0, visibleLineCount = 1)

        // Mutating tailWideState must notify observer and invalidate the cached buckets.
        tailWideState.value = false
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 0, visibleLineCount = 1)
        assertThat(spanLayoutProvider.getLineConfiguration(20).firstItemIndex).isEqualTo(40)
    }

    /**
     * Verifies that the intra-bucket reverse-scroll cache (`cachedBucketIndex` / `cachedBucket`) is
     * cleared when an observed span state changes.
     */
    @Test
    fun reverseScrollCachedBucket_invalidatedWhenStateChanges() {
        val totalItems = 100
        val slotsPerLine = 2
        val wideSpans = mutableStateOf(true)
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { GridItemSpan(if (wideSpans.value) 2 else 1) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        // With totalItems = 100 and slotsPerLine = 2, bucketSize is 8.
        // Querying line 5 from currentLine = 0 (5 - 0 in 2 until 8) populates cachedBucket for
        // bucket 0 (lines 0..4), and querying line 3 in reverse order hits cachedBucket[3].
        simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 0, visibleLineCount = 1)
        assertThat(spanLayoutProvider.getLineConfiguration(5).firstItemIndex).isEqualTo(5)
        assertThat(spanLayoutProvider.getLineConfiguration(3).firstItemIndex).isEqualTo(3)

        // Toggle state so each line holds 2 items instead of 1.
        wideSpans.value = false
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        // Anchor the post-mutation measure pass at item 16 (line 8, the start of bucket 1) so
        // getLineConfiguration(8) starts at currentLine = 8 (lineIndex - currentLine == 0, which
        // does not set cacheThisBucket or overwrite bucket 0's cachedBucket), then query line 3
        // in bucket 0 to verify invalidateCache() cleared cachedBucketIndex and cachedBucket.
        assertThat(
                simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 16, visibleLineCount = 1)
            )
            .isEqualTo(8)
        assertThat(spanLayoutProvider.getLineConfiguration(3).firstItemIndex).isEqualTo(6)
    }

    /**
     * Verifies that a grid with custom spans that do not read any snapshot state (e.g. constant
     * spans) records zero observed states and is never invalidated when unrelated snapshot state
     * changes.
     */
    @Test
    fun unrelatedStateChange_doesNotInvalidateGridWithConstantSpans() {
        val totalItems = 100
        val slotsPerLine = 2
        val unrelatedState = mutableStateOf(0)
        var spanEvaluationCount = 0
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { index ->
                        spanEvaluationCount++
                        GridItemSpan(if (index % 3 == 0) 2 else 1)
                    },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        // Warm the cache by calculating all lines
        val targetLine = totalItems / slotsPerLine - 1
        spanLayoutProvider.getLineConfiguration(targetLine)
        assertThat(spanLayoutProvider.hasObservedStates).isFalse()
        val initialSpanEvaluations = spanEvaluationCount
        assertThat(initialSpanEvaluations).isGreaterThan(0)

        // Querying the same line again in warm steady-state only evaluates the items on that line
        spanLayoutProvider.getLineConfiguration(targetLine)
        val steadyStateEvaluations = spanEvaluationCount - initialSpanEvaluations
        val evaluationsBeforeApply = spanEvaluationCount

        // Mutate unrelated state and send apply notifications
        unrelatedState.value++
        Snapshot.sendApplyNotifications()

        spanLayoutProvider.getLineConfiguration(targetLine)
        val postApplyEvaluations = spanEvaluationCount - evaluationsBeforeApply
        assertThat(postApplyEvaluations).isEqualTo(steadyStateEvaluations)
    }

    /**
     * Verifies that a grid with sticky headers sets [LazyGridIntervalContent.hasCustomSpans] for
     * correct full-width line geometry, and does not record any observed states.
     */
    @Test
    fun stickyHeader_setsCustomSpansForGeometry_withoutRecordingObservedStates() {
        val content =
            LazyGridIntervalContent({
                stickyHeader {}
                items(10) {}
            })
        assertThat(content.hasCustomSpans).isTrue()

        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = 2

        // Line 0 is the sticky header spanning all 2 slots
        val line0 = spanLayoutProvider.getLineConfiguration(0)
        assertThat(line0.firstItemIndex).isEqualTo(0)
        assertThat(line0.spans).hasSize(1)
        assertThat(line0.spans[0].currentLineSpan).isEqualTo(2)

        // Line 1 starts at item 1
        val line1 = spanLayoutProvider.getLineConfiguration(1)
        assertThat(line1.firstItemIndex).isEqualTo(1)
        assertThat(line1.spans).hasSize(2)
        assertThat(spanLayoutProvider.hasObservedStates).isFalse()
    }

    /**
     * Verifies that a [mutableStateOf] configured with [referentialEqualityPolicy] invalidates the
     * cache and relayouts lines when assigned a new instance with an updated span.
     */
    @Test
    fun referentialEqualityMutableState_invalidatesCacheWhenNewInstanceAssigned() {
        class SpanConfig(val span: Int)

        val totalItems = 100
        val slotsPerLine = 2
        val configState = mutableStateOf(SpanConfig(2), referentialEqualityPolicy())
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = { GridItemSpan(configState.value.span) },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(40)

        configState.value = SpanConfig(1)
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40)).isEqualTo(20)
    }

    /**
     * Verifies that a [mutableStateOf] configured with [neverEqualPolicy] does not continuously
     * invalidate the warm bucket cache on steady-state measure passes when its value has not
     * changed, and properly invalidates the cache when its value changes.
     */
    @Test
    fun neverEqualMutableState_preservesWarmCacheWhenUnchangedAndInvalidatesWhenChanged() {
        val totalItems = 100
        val slotsPerLine = 2
        var spanEvaluationCount = 0
        val spanState = mutableStateOf(2, neverEqualPolicy())
        val content =
            LazyGridIntervalContent({
                items(
                    count = totalItems,
                    span = {
                        spanEvaluationCount++
                        GridItemSpan(spanState.value)
                    },
                    contentType = { null },
                    itemContent = {},
                )
            })
        val spanLayoutProvider = content.spanLayoutProvider
        spanLayoutProvider.slotsPerLine = slotsPerLine

        // Warm up buckets up to item 40 (line 40).
        assertThat(
                simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40, visibleLineCount = 5)
            )
            .isEqualTo(40)
        val evaluationsAfterWarmup = spanEvaluationCount

        // A second steady-state measure pass at item 40 must use the warm bucket cache rather than
        // re-walking all 40 items from item 0 on every visible line.
        assertThat(
                simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40, visibleLineCount = 5)
            )
            .isEqualTo(40)
        assertThat(spanEvaluationCount - evaluationsAfterWarmup).isLessThan(20)

        // Mutating spanState to 1 must invalidate the cache and relayout item 40 to line 20.
        spanState.value = 1
        Snapshot.sendApplyNotifications()
        assertThat(scopeInvalidated).isTrue()

        assertThat(
                simulateMeasurePass(spanLayoutProvider, anchorItemIndex = 40, visibleLineCount = 5)
            )
            .isEqualTo(20)
    }
}
