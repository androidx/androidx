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

package androidx.wear.compose.foundation.lazy

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * The vertically scrolling list that only composes and lays out the currently visible items. This
 * is a wear specific version of LazyColumn that adds support for scaling and morphing animations.
 *
 * @sample androidx.wear.compose.foundation.samples.LazyColumnLettersSample
 * @param modifier The modifier to be applied to the layout.
 * @param state The state object to be used to control the list and the applied layout.
 * @param verticalArrangement The vertical arrangement of the items.
 * @param horizontalAlignment The horizontal alignment of the items.
 * @param flingBehavior The fling behavior to be used for the list.
 * @param userScrollEnabled Whether the user should be able to scroll the list.
 * @param content The content of the list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyColumnState = rememberLazyColumnState(),
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(
            space = 4.dp,
            // TODO: b/352513793 - Add support for reverseLayout.
            alignment = Alignment.Top
        ),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyColumnScope.() -> Unit
) {
    val latestContent = rememberUpdatedState(newValue = content)

    val itemProviderLambda by
        remember(state) {
            val scope =
                derivedStateOf(referentialEqualityPolicy()) {
                    LazyColumnScopeImpl(latestContent.value)
                }
            derivedStateOf(referentialEqualityPolicy()) {
                {
                    val intervalContent = scope.value
                    val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                    LazyColumnItemProvider(
                        intervalContent = intervalContent,
                        state = state,
                        keyIndexMap = map
                    )
                }
            }
        }

    val measurePolicy =
        rememberLazyColumnMeasurePolicy(
            itemProviderLambda = itemProviderLambda,
            state = state,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
        )
    val reverseDirection =
        ScrollableDefaults.reverseDirection(
            LocalLayoutDirection.current,
            Orientation.Vertical,
            reverseScrolling = false
        )
    LazyLayout(
        itemProvider = itemProviderLambda,
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .scrollable(
                    state = state,
                    reverseDirection = reverseDirection,
                    enabled = userScrollEnabled,
                    orientation = Orientation.Vertical,
                    flingBehavior = flingBehavior,
                ),
        measurePolicy = measurePolicy
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyColumnItemProvider(
    val intervalContent: LazyLayoutIntervalContent<LazyColumnInterval>,
    val state: LazyColumnState,
    val keyIndexMap: NearestRangeKeyIndexMap
) : LazyLayoutItemProvider {
    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        val itemScope = remember(index) { LazyColumnItemScopeImpl(index, state = state) }
        intervalContent.withInterval(index) { localIndex, content ->
            content.item(itemScope, localIndex)
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyColumnItemProvider) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class NearestRangeKeyIndexMap(
    nearestRange: IntRange,
    intervalContent: LazyLayoutIntervalContent<*>
) {
    private val map: ObjectIntMap<Any>
    private val keys: Array<Any?>
    private val keysStartIndex: Int

    init {
        // Traverses the interval [list] in order to create a mapping from the key to the index for
        // all the indexes in the passed [range].
        val list = intervalContent.intervals
        val first = nearestRange.first
        val last = minOf(nearestRange.last, list.size - 1)
        if (last < first) {
            map = emptyObjectIntMap()
            keys = emptyArray()
            keysStartIndex = 0
        } else {
            val size = last - first + 1
            keys = arrayOfNulls<Any?>(size)
            keysStartIndex = first
            map =
                MutableObjectIntMap<Any>(size).also { map ->
                    list.forEach(
                        fromIndex = first,
                        toIndex = last,
                    ) {
                        val keyFactory = it.value.key
                        val start = maxOf(first, it.startIndex)
                        val end = minOf(last, it.startIndex + it.size - 1)
                        for (i in start..end) {
                            val key =
                                keyFactory?.invoke(i - it.startIndex) ?: getDefaultLazyLayoutKey(i)
                            map[key] = i
                            keys[i - keysStartIndex] = key
                        }
                    }
                }
        }
    }

    fun getIndex(key: Any): Int = map.getOrElse(key) { -1 }

    fun getKey(index: Int) = keys.getOrElse(index - keysStartIndex) { null }
}
