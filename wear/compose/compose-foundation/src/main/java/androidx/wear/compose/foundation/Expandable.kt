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

package androidx.wear.compose.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Create and [remember] an [ExpandableState]
 *
 * Example of an expandable list:
 * @sample androidx.wear.compose.foundation.samples.ExpandableWithItemsSample
 *
 * Example of an expandable text:
 * @sample androidx.wear.compose.foundation.samples.ExpandableTextSample
 *
 * @param initiallyExpanded The initial value of the state.
 * @param expandAnimationSpec The [AnimationSpec] to use when showing the extra information.
 * @param collapseAnimationSpec The [AnimationSpec] to use when hiding the extra information.
 */
@Composable
public fun rememberExpandableState(
    initiallyExpanded: Boolean = false,
    expandAnimationSpec: AnimationSpec<Float> = ExpandableItemsDefaults.expandAnimationSpec,
    collapseAnimationSpec: AnimationSpec<Float> = ExpandableItemsDefaults.collapseAnimationSpec,
): ExpandableState {
    val scope = rememberCoroutineScope()
    return remember {
        ExpandableState(initiallyExpanded, scope, expandAnimationSpec, collapseAnimationSpec)
    }
}

/**
 * Create and [remember] a mapping from keys to [ExpandableState]s
 * [ExpandableState]s can be requested by key, and we will created with the parameters given here
 * when a mapping didn't exist before.
 * This is mainly useful when you want to have a variable number of expandables, that can change at
 * runtime (for example, elements on a ScalingLazyColumn)
 *
 * @param initiallyExpanded A function to compute the initial state given the key.
 * @param expandAnimationSpec The [AnimationSpec] to use when showing the extra information.
 * @param collapseAnimationSpec The [AnimationSpec] to use when hiding the extra information.
 */
@Composable
public fun <T> rememberExpandableStateMapping(
    initiallyExpanded: (key: T) -> Boolean = { false },
    expandAnimationSpec: AnimationSpec<Float> = ExpandableItemsDefaults.expandAnimationSpec,
    collapseAnimationSpec: AnimationSpec<Float> = ExpandableItemsDefaults.collapseAnimationSpec,
): ExpandableStateMapping<T> {
    val scope = rememberCoroutineScope()
    return remember {
        ExpandableStateMapping(initiallyExpanded, scope, expandAnimationSpec, collapseAnimationSpec)
    }
}

/**
 * Adds a series of items, that will be expanded/collapsed according to the [ExpandableState]
 *
 * Example of an expandable list:
 * @sample androidx.wear.compose.foundation.samples.ExpandableWithItemsSample
 *
 * @param state The [ExpandableState] connected to these items.
 * @param count The number of items
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
public fun ScalingLazyListScope.expandableItems(
    state: ExpandableState,
    count: Int,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable BoxScope.(index: Int) -> Unit
) {
    repeat(count) { itemIndex ->
        // Animations for each item start in inverse order, the first item animates last.
        val animationStart = count - 1 - itemIndex
        val animationProgress =
            (state.expandProgress * count - animationStart).coerceIn(0f, 1f)
        if (animationProgress > 0) {
            item(key = key?.invoke(itemIndex)) {
                Layout(
                    modifier = Modifier.clipToBounds(),
                    content = { Box(content = { itemContent(itemIndex) }) }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    val shownHeight = (placeable.height * animationProgress).roundToInt()
                    layout(placeable.width, shownHeight) {
                        val y = (placeable.height * (animationProgress - 1)).roundToInt()
                        placeable.placeWithLayer(0, y)
                    }
                }
            }
        }
    }
}

/**
 * Adds a single item, that will be expanded/collapsed according to the [ExpandableState].
 *
 * Example of an expandable text:
 * @sample androidx.wear.compose.foundation.samples.ExpandableTextSample
 *
 * The item should support two levels of information display (for example, a text showing a few
 * lines in the collapsed state, and more in the expanded state)
 *
 * @param state The [ExpandableState] connected to this item.
 * @param key A stable and unique key representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param content the content displayed by the item, according to its expanded/collapsed state.
 */
public fun ScalingLazyListScope.expandableItem(
    state: ExpandableState,
    key: Any? = null,
    content: @Composable (expanded: Boolean) -> Unit
) = expandableItemImpl(state, key, content = content)

/**
 * Adds a single item, for the button that controls expandable item(s). The button will be animated
 * out when the corresponding expandables are expanded.
 *
 * Example of an expandable text:
 * @sample androidx.wear.compose.foundation.samples.ExpandableTextSample
 *
 * @param state The [ExpandableState] to connect this button to.
 * @param key A stable and unique key representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param content the content displayed, this should usually be a CompactChip or OutlineCompactChip.
 */
public fun ScalingLazyListScope.expandableButton(
    state: ExpandableState,
    key: Any? = null,
    content: @Composable () -> Unit
) = expandableItemImpl(state, key, invertProgress = true, content = { if (it) content() })

private fun ScalingLazyListScope.expandableItemImpl(
    state: ExpandableState,
    key: Any? = null,
    invertProgress: Boolean = false,
    content: @Composable (expanded: Boolean) -> Unit
) {
    val progress = if (invertProgress) 1f - state.expandProgress else state.expandProgress

    item(key = key) {
        Layout(
            content = {
                Box { content(false) }
                Box { content(true) }
            },
            modifier = Modifier.clipToBounds()
        ) { measurables, constraints ->
            val placeables = measurables.fastMap { it.measure(constraints) }

            val width = lerp(placeables[0].width, placeables[1].width, progress)
            val height = lerp(placeables[0].height, placeables[1].height, progress)

            // Keep the items horizontally centered.
            val off0 = (width - placeables[0].width) / 2
            val off1 = (width - placeables[1].width) / 2

            layout(width, height) {
                if (progress < 1f) {
                    placeables[0].placeWithLayer(off0, 0, zIndex = 1 - progress) {
                        alpha = 1 - progress
                    }
                }
                if (progress > 0f) {
                    placeables[1].placeWithLayer(off1, 0, zIndex = progress) { alpha = progress }
                }
            }
        }
    }
}

/**
 * State of the Expandable composables.
 *
 * It's used to control the showing/hiding of extra information either directly or connecting it
 * with something like a button.
 */
public class ExpandableState internal constructor(
    initiallyExpanded: Boolean,
    private val coroutineScope: CoroutineScope,
    private val expandAnimationSpec: AnimationSpec<Float>,
    private val collapseAnimationSpec: AnimationSpec<Float>
) {
    private val _expandProgress = Animatable(if (initiallyExpanded) 1f else 0f)

    /**
     * While in the middle of the animation, this represents the progress from 0f (collapsed) to
     * 1f (expanded), or the other way around.
     * If no animation is running, it's either 0f if the extra content is not showing, or 1f if
     * the extra content is showing.
     */
    val expandProgress
        get() = _expandProgress.value

    /**
     * Represents the current state of the component, true means it's showing the extra information.
     * If its in the middle of an animation, the value of this field takes into account only the
     * target of that animation.
     *
     * Modifying this value triggers a change to show/hide the extra information.
     */
    var expanded
        @JvmName("isExpanded")
        get() = _expandProgress.targetValue == 1f
        set(newValue) {
            if (expanded != newValue) {
                coroutineScope.launch {
                    if (newValue) {
                        _expandProgress.animateTo(1f, expandAnimationSpec)
                    } else {
                        _expandProgress.animateTo(0f, collapseAnimationSpec)
                    }
                }
            }
        }
}

/**
 * A class that maps from keys of the given type to [ExpandableState].
 * An instance can be created and remembered with [rememberExpandableStateMapping]
 */
public class ExpandableStateMapping<T> internal constructor(
    private val initiallyExpanded: (key: T) -> Boolean,
    private val coroutineScope: CoroutineScope,
    private val expandAnimationSpec: AnimationSpec<Float>,
    private val collapseAnimationSpec: AnimationSpec<Float>
) {

    private val states = mutableStateMapOf<T, ExpandableState>()

    /**
     * Returns the [ExpandableState] for the given key if the value is present and not null.
     * Otherwise, creates a new one, puts it into the map under the given key and returns it.
     * The parameters used to create the new [ExpandableState] are the ones passed to
     * [rememberExpandableStateMapping]
     */
    public fun getOrPutNew(key: T) = states.getOrPut(key) {
        ExpandableState(initiallyExpanded(key), coroutineScope,
            expandAnimationSpec, collapseAnimationSpec)
    }
}

/**
 * Contains the default values used by Expandable components.
 */
public object ExpandableItemsDefaults {
    /**
     * Default animation used to show extra information.
     */
    val expandAnimationSpec: AnimationSpec<Float> = TweenSpec(1000)

    /**
     * Default animation used to hide extra information.
     */
    val collapseAnimationSpec: AnimationSpec<Float> = TweenSpec(1000)
}
