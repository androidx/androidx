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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutAnimateItemElement
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutAnimationSpecsNode
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.wear.compose.foundation.lazy.layout.MutableIntervalList

/** Receiver scope being used by the item content parameter of [TransformingLazyColumn]. */
@TransformingLazyColumnScopeMarker
public sealed interface TransformingLazyColumnItemScope {
    /**
     * Scroll progress of the item before height transformation is applied using
     * [Modifier.transformedHeight]. Is [TransformingLazyColumnItemScrollProgress.Unspecified] for
     * the item that is off screen.
     */
    public val DrawScope.scrollProgress: TransformingLazyColumnItemScrollProgress

    /**
     * Scroll progress of the item before height transformation is applied using
     * [Modifier.transformedHeight]. Is [TransformingLazyColumnItemScrollProgress.Unspecified] for
     * the item that is off screen.
     */
    public val GraphicsLayerScope.scrollProgress: TransformingLazyColumnItemScrollProgress

    /**
     * Applies the new height of the item depending on its scroll progress and measured height.
     *
     * @param heightProvider The transformation to be applied. The first parameter is the height of
     *   the item returned during measurement. The second parameter is the scroll progress of the
     *   item. This lambda should not read from any state values.
     */
    public fun Modifier.transformedHeight(
        heightProvider:
            (measuredHeight: Int, scrollProgress: TransformingLazyColumnItemScrollProgress) -> Int
    ): Modifier

    /**
     * Preserves the appearance of some content within an item, by preventing implicit access to the
     * [TransformingLazyColumnItemScope]. Explicit use of [LocalTransformingLazyColumnItemScope] can
     * still apply transformations to the item.
     *
     * @sample androidx.wear.compose.foundation.samples.TransformingLazyColumnImplicitSample
     */
    @Composable
    public fun TransformExclusion(content: @Composable TransformingLazyColumnItemScope.() -> Unit) {
        CompositionLocalProvider(LocalTransformingLazyColumnItemScope provides null) { content() }
    }

    /**
     * This modifier animates item appearance (fade in), disappearance (fade out) and placement
     * changes (such as an item reordering).
     *
     * You should also provide a unique key via [TransformingLazyColumnScope.item]/
     * [TransformingLazyColumnScope.items] for this modifier to enable animations.
     *
     * @sample androidx.wear.compose.foundation.samples.TransformingLazyColumnAnimateItemSample
     * @param fadeInSpec an animation spec to use for animating the item appearance. When null is
     *   provided the item will appear without animations.
     * @param placementSpec an animation spec that will be used to animate the item placement. Aside
     *   from item reordering all other position changes caused by events like arrangement or
     *   alignment changes will also be animated. When null is provided no animations will happen.
     * @param fadeOutSpec an animation spec to use for animating the item disappearance. When null
     *   is provided the item will be disappearance without animations.
     */
    public fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
        placementSpec: FiniteAnimationSpec<IntOffset>? =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            ),
        fadeOutSpec: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    ): Modifier
}

/** Receiver scope which is used by [TransformingLazyColumn]. */
@TransformingLazyColumnScopeMarker
public sealed interface TransformingLazyColumnScope {
    /**
     * Adds [count] items.
     *
     * @param count The number of items to add to the [TransformingLazyColumn].
     * @param key A factory of stable and unique keys representing the item. Using the same key for
     *   multiple items in the [TransformingLazyColumn] is not allowed.
     * @param contentType A factory of the content types for the item. The item compositions of the
     *   same type could be reused more efficiently. Note that null is a valid type and items of
     *   such type will be considered compatible.
     * @param content The content displayed by a single item.
     */
    public fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        content: @Composable TransformingLazyColumnItemScope.(index: Int) -> Unit
    )

    /**
     * Adds a single item.
     *
     * @param key A stable and unique key representing the item. Using the same key for multiple
     *   items in the [TransformingLazyColumn] is not allowed. Type of the key should be saveable
     *   via Bundle on Android. If null is passed the position in the [TransformingLazyColumn] will
     *   represent the key. When you specify the key the scroll position will be maintained based on
     *   the key, which means if you add/remove items before the current visible item the item with
     *   the given key will be kept as the first visible one.
     * @param contentType The type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content The content of the item.
     */
    public fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable TransformingLazyColumnItemScope.() -> Unit
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the [TransformingLazyColumn] is not allowed. Type of the key should be
 *   saveable via Bundle on Android. If null is passed the position in the [TransformingLazyColumn]
 *   will represent the key. When you specify the key the scroll position will be maintained based
 *   on the key, which means if you add/remove items before the current visible item the item with
 *   the given key will be kept as the first visible one.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item.
 */
public inline fun <T> TransformingLazyColumnScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable TransformingLazyColumnItemScope.(item: T) -> Unit
): Unit =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) {
        itemContent(items[it])
    }

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the [TransformingLazyColumn] is not allowed. Type of the key should be
 *   saveable via Bundle on Android. If null is passed the position in the list will represent the
 *   key. When you specify the key the scroll position will be maintained based on the key, which
 *   means if you add/remove items before the current visible item the item with the given key will
 *   be kept as the first visible one.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
public inline fun <T> TransformingLazyColumnScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent:
        @Composable
        TransformingLazyColumnItemScope.(index: Int, item: T) -> Unit
): Unit =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        contentType = { index -> contentType(index, items[index]) }
    ) {
        itemContent(it, items[it])
    }

internal class TransformingLazyColumnItemScopeImpl(
    val index: Int,
    val state: TransformingLazyColumnState,
    val reduceMotionEnabled: Boolean
) : TransformingLazyColumnItemScope {

    private val _scrollProgress: TransformingLazyColumnItemScrollProgress
        get() =
            state.layoutInfo.visibleItems.fastFirstOrNull { it.index == index }?.scrollProgress
                ?: TransformingLazyColumnItemScrollProgress.Unspecified

    override val DrawScope.scrollProgress: TransformingLazyColumnItemScrollProgress
        get() = _scrollProgress

    override val GraphicsLayerScope.scrollProgress: TransformingLazyColumnItemScrollProgress
        get() = _scrollProgress

    override fun Modifier.transformedHeight(
        heightProvider: (Int, TransformingLazyColumnItemScrollProgress) -> Int
    ): Modifier =
        if (reduceMotionEnabled) this
        else this then TransformingLazyColumnCompositeParentDataModifier(heightProvider)

    override fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>?,
        placementSpec: FiniteAnimationSpec<IntOffset>?,
        fadeOutSpec: FiniteAnimationSpec<Float>?,
    ): Modifier =
        if (reduceMotionEnabled) {
            this
        } else if (fadeInSpec == null && placementSpec == null && fadeOutSpec == null) {
            this
        } else {
            this then LazyLayoutAnimateItemElement(fadeInSpec, placementSpec, fadeOutSpec)
        }
}

internal class TransformingLazyColumnCompositeParentDataModifier(
    val heightProvider: (Int, TransformingLazyColumnItemScrollProgress) -> Int,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any {
        if (parentData is LazyLayoutAnimationSpecsNode) {
            return TransformingLazyColumnParentData(heightProvider, parentData)
        }
        if (parentData is TransformingLazyColumnParentData) {
            return parentData.copy(heightProvider = heightProvider)
        }
        return TransformingLazyColumnParentData(heightProvider)
    }
}

internal data class TransformingLazyColumnParentData(
    val heightProvider: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)? = null,
    val animationSpecs: LazyLayoutAnimationSpecsNode? = null,
)

internal class TransformingLazyColumnScopeImpl(
    val content: TransformingLazyColumnScope.() -> Unit
) : LazyLayoutIntervalContent<TransformingLazyColumnInterval>(), TransformingLazyColumnScope {
    override val intervals: MutableIntervalList<TransformingLazyColumnInterval> =
        MutableIntervalList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        content: @Composable TransformingLazyColumnItemScope.(Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            TransformingLazyColumnInterval(
                key,
                type = contentType,
                item = content,
            )
        )
    }

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable TransformingLazyColumnItemScope.() -> Unit
    ) {
        intervals.addInterval(
            1,
            TransformingLazyColumnInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() },
            )
        )
    }
}

internal class TransformingLazyColumnInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable TransformingLazyColumnItemScope.(index: Int) -> Unit,
) : LazyLayoutIntervalContent.Interval
