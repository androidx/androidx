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

package androidx.compose.foundation.layout

import androidx.collection.IntIntPair
import androidx.compose.foundation.layout.ContextualFlowRowOverflow.Companion.Clip
import androidx.compose.foundation.layout.ContextualFlowRowOverflow.Companion.Visible
import androidx.compose.foundation.layout.FlowColumnOverflow.Companion.Clip
import androidx.compose.foundation.layout.FlowColumnOverflow.Companion.Visible
import androidx.compose.foundation.layout.FlowColumnOverflow.Companion.expandIndicator
import androidx.compose.foundation.layout.FlowColumnOverflow.Companion.expandOrCollapseIndicator
import androidx.compose.foundation.layout.FlowRowOverflow.Companion.Clip
import androidx.compose.foundation.layout.FlowRowOverflow.Companion.expandIndicator
import androidx.compose.foundation.layout.FlowRowOverflow.Companion.expandOrCollapseIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Overflow Handling Options
 *
 * This enumeration defines the available options for handling content that exceeds the boundaries
 * of its container for [FlowRow].
 *
 * Options:
 * - [Visible]: The overflowing content remains visible outside its container. This can lead to
 *   overlapping with other elements. Use this option when it's crucial to display all content,
 *   regardless of the container's size.
 * - [Clip]: The overflowing content is clipped and not visible beyond the boundary of its
 *   container. Ideal for maintaining a clean and uncluttered UI, where overlapping elements are
 *   undesirable.
 * - [expandIndicator]: Behaves similar to [Clip], however shows an indicator or button indicating
 *   that more items can be loaded.
 * - [expandOrCollapseIndicator]: Extends the [expandIndicator] functionality by adding a 'Collapse'
 *   option. After expanding the content, users can choose to collapse it back to the summary view.
 */
@ExperimentalLayoutApi
class FlowRowOverflow
private constructor(
    type: OverflowType,
    minLinesToShowCollapse: Int = 0,
    minCrossAxisSizeToShowCollapse: Int = 0,
    seeMoreGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null,
    collapseGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null
) :
    FlowLayoutOverflow(
        type,
        minLinesToShowCollapse,
        minCrossAxisSizeToShowCollapse,
        seeMoreGetter,
        collapseGetter
    ) {

    companion object {
        /** Display all content, even if there is not enough space in the specified bounds. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Visible = FlowRowOverflow(OverflowType.Visible)

        /** Clip the overflowing content to fix its container. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Clip = FlowRowOverflow(OverflowType.Clip)

        /**
         * Registers an "expand indicator" composable for handling overflow in a [FlowRow].
         *
         * This function allows the creation of a custom composable that can be displayed when there
         * are more items in the [FlowRow] than can be displayed in the available space. The
         * "expandable indicator" composable can be tailored to show a summary, a button, or any
         * other composable element that indicates the presence of additional items.
         *
         * @sample androidx.compose.foundation.layout.samples.SimpleFlowRowMaxLinesWithSeeMore
         * @param content composable that visually indicates more items can be loaded.
         */
        @ExperimentalLayoutApi
        fun expandIndicator(content: @Composable FlowRowOverflowScope.() -> Unit): FlowRowOverflow {
            val seeMoreGetter = { state: FlowLayoutOverflowState ->
                @Composable {
                    val scope = FlowRowOverflowScopeImpl(state)
                    scope.content()
                }
            }
            return FlowRowOverflow(OverflowType.ExpandIndicator, seeMoreGetter = seeMoreGetter)
        }

        /**
         * Registers an "expand or collapse indicator" composable for handling overflow in a
         * [FlowRow].
         *
         * This function is designed to switch between two states: a "Expandable" state when there
         * are more items to show, and a "Collapsable" state when all items are being displayed and
         * can be collapsed.
         *
         * Prior to layout, the function evaluates both composables indicators to determine their
         * maximum intrinsic size. Depending on the space available and the number of items, either
         * the [expandIndicator] or the [collapseIndicator] is rendered.
         *
         * @sample androidx.compose.foundation.layout.samples.SimpleFlowRowMaxLinesDynamicSeeMore
         * @param minRowsToShowCollapse Specifies the minimum number of rows that should be visible
         *   before showing the collapse option. This parameter is useful when the number of rows is
         *   too small to be reduced further.
         * @param minHeightToShowCollapse Specifies the minimum height at which the collapse option
         *   should be displayed. This parameter is useful for preventing the collapse option from
         *   appearing when the height is too narrow.
         * @param expandIndicator composable that visually indicates more items can be loaded.
         * @param collapseIndicator composable that visually indicates less items can be loaded.
         */
        @ExperimentalLayoutApi
        @Composable
        fun expandOrCollapseIndicator(
            expandIndicator: @Composable FlowRowOverflowScope.() -> Unit,
            collapseIndicator: @Composable FlowRowOverflowScope.() -> Unit,
            minRowsToShowCollapse: Int = 1,
            minHeightToShowCollapse: Dp = 0.dp,
        ): FlowRowOverflow {
            val minHeightToShowCollapsePx =
                with(LocalDensity.current) { minHeightToShowCollapse.roundToPx() }
            return remember(
                minRowsToShowCollapse,
                minHeightToShowCollapsePx,
                expandIndicator,
                collapseIndicator
            ) {
                val seeMoreGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = FlowRowOverflowScopeImpl(state)
                        scope.expandIndicator()
                    }
                }

                val collapseGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = FlowRowOverflowScopeImpl(state)
                        scope.collapseIndicator()
                    }
                }

                FlowRowOverflow(
                    OverflowType.ExpandOrCollapseIndicator,
                    minLinesToShowCollapse = minRowsToShowCollapse,
                    minCrossAxisSizeToShowCollapse = minHeightToShowCollapsePx,
                    seeMoreGetter = seeMoreGetter,
                    collapseGetter = collapseGetter
                )
            }
        }
    }
}

/**
 * Overflow Handling Options
 *
 * This enumeration defines the available options for handling content that exceeds the boundaries
 * of its container for [FlowColumn] and [ContextualFlowColumn].
 *
 * Options:
 * - [Visible]: The overflowing content remains visible outside its container. This can lead to
 *   overlapping with other elements. Use this option when it's crucial to display all content,
 *   regardless of the container's size.
 * - [Clip]: The overflowing content is clipped and not visible beyond the boundary of its
 *   container. Ideal for maintaining a clean and uncluttered UI, where overlapping elements are
 *   undesirable.
 * - [expandIndicator]: Behaves similar to [Clip], however shows an indicator or button indicating
 *   that more items can be loaded.
 * - [expandOrCollapseIndicator]: Extends the [expandIndicator] functionality by adding a 'Collapse'
 *   option. After expanding the content, users can choose to collapse it back to the summary view.
 */
@ExperimentalLayoutApi
class FlowColumnOverflow
private constructor(
    type: OverflowType,
    minLinesToShowCollapse: Int = 0,
    minCrossAxisSizeToShowCollapse: Int = 0,
    seeMoreGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null,
    collapseGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null
) :
    FlowLayoutOverflow(
        type,
        minLinesToShowCollapse,
        minCrossAxisSizeToShowCollapse,
        seeMoreGetter,
        collapseGetter
    ) {
    companion object {
        /** Display all content, even if there is not enough space in the specified bounds. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Visible = FlowColumnOverflow(FlowLayoutOverflow.OverflowType.Visible)

        /** Clip the overflowing content to fix its container. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Clip = FlowColumnOverflow(FlowLayoutOverflow.OverflowType.Clip)

        /**
         * Registers an "expand indicator" composable for handling overflow in a [FlowColumn].
         *
         * This function allows the creation of a custom composable that can be displayed when there
         * are more items in the [FlowColumn] than can be displayed in the available space. The
         * "expandable indicator" composable can be tailored to show a summary, a button, or any
         * other composable element that indicates the presence of additional items.
         *
         * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumnMaxLinesWithSeeMore
         * @param content composable that visually indicates more items can be loaded.
         */
        @ExperimentalLayoutApi
        fun expandIndicator(
            content: @Composable FlowColumnOverflowScope.() -> Unit
        ): FlowColumnOverflow {
            val seeMoreGetter = { state: FlowLayoutOverflowState ->
                @Composable {
                    val scope = FlowColumnOverflowScopeImpl(state)
                    scope.content()
                }
            }
            return FlowColumnOverflow(OverflowType.ExpandIndicator, seeMoreGetter = seeMoreGetter)
        }

        /**
         * Registers an "expand or collapse indicator" composable for handling overflow in a
         * [FlowColumn].
         *
         * This function is designed to switch between two states: a "Expandable" state when there
         * are more items to show, and a "Collapsable" state when all items are being displayed and
         * can be collapsed.
         *
         * Prior to layout, the function evaluates both composables indicators to determine their
         * maximum intrinsic size. Depending on the space available and the number of items, either
         * the [expandIndicator] or the [collapseIndicator] is rendered.
         *
         * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumnMaxLinesDynamicSeeMore
         * @param minColumnsToShowCollapse Specifies the minimum number of columns that should be
         *   visible before showing the collapse option. This parameter is useful when the number of
         *   columns is too small to be reduced further.
         * @param minWidthToShowCollapse Specifies the minimum width at which the collapse option
         *   should be displayed. This parameter is useful for preventing the collapse option from
         *   appearing when the width is too narrow.
         * @param expandIndicator composable that visually indicates more items can be loaded.
         * @param collapseIndicator composable that visually indicates less items can be loaded.
         */
        @ExperimentalLayoutApi
        @Composable
        fun expandOrCollapseIndicator(
            expandIndicator: @Composable FlowColumnOverflowScope.() -> Unit,
            collapseIndicator: @Composable FlowColumnOverflowScope.() -> Unit,
            minColumnsToShowCollapse: Int = 1,
            minWidthToShowCollapse: Dp = 0.dp,
        ): FlowColumnOverflow {
            val minWidthToShowCollapsePx =
                with(LocalDensity.current) { minWidthToShowCollapse.roundToPx() }
            return remember(
                minColumnsToShowCollapse,
                minWidthToShowCollapsePx,
                expandIndicator,
                collapseIndicator
            ) {
                val seeMoreGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = FlowColumnOverflowScopeImpl(state)
                        scope.expandIndicator()
                    }
                }

                val collapseGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = FlowColumnOverflowScopeImpl(state)
                        scope.collapseIndicator()
                    }
                }

                FlowColumnOverflow(
                    OverflowType.ExpandOrCollapseIndicator,
                    minLinesToShowCollapse = minColumnsToShowCollapse,
                    minCrossAxisSizeToShowCollapse = minWidthToShowCollapsePx,
                    seeMoreGetter = seeMoreGetter,
                    collapseGetter = collapseGetter
                )
            }
        }
    }
}

/**
 * Overflow Handling Options
 *
 * This enumeration defines the available options for handling content that exceeds the boundaries
 * of its container for [ContextualFlowRow].
 *
 * Options:
 * - [Visible]: The overflowing content remains visible outside its container. This can lead to
 *   overlapping with other elements. Use this option when it's crucial to display all content,
 *   regardless of the container's size.
 * - [Clip]: The overflowing content is clipped and not visible beyond the boundary of its
 *   container. Ideal for maintaining a clean and uncluttered UI, where overlapping elements are
 *   undesirable.
 * - [expandIndicator]: Behaves similar to [Clip], however shows an indicator or button indicating
 *   that more items can be loaded.
 * - [expandOrCollapseIndicator]: Extends the [expandIndicator] functionality by adding a 'Collapse'
 *   option. After expanding the content, users can choose to collapse it back to the summary view.
 */
@ExperimentalLayoutApi
class ContextualFlowRowOverflow
private constructor(
    type: OverflowType,
    minLinesToShowCollapse: Int = 0,
    minCrossAxisSizeToShowCollapse: Int = 0,
    seeMoreGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null,
    collapseGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null
) :
    FlowLayoutOverflow(
        type,
        minLinesToShowCollapse,
        minCrossAxisSizeToShowCollapse,
        seeMoreGetter,
        collapseGetter
    ) {

    companion object {
        /** Display all content, even if there is not enough space in the specified bounds. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Visible = ContextualFlowRowOverflow(FlowLayoutOverflow.OverflowType.Visible)

        /** Clip the overflowing content to fix its container. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Clip = ContextualFlowRowOverflow(FlowLayoutOverflow.OverflowType.Clip)

        /**
         * Registers an "expand indicator" composable for handling overflow in a
         * [ContextualFlowRow].
         *
         * This function allows the creation of a custom composable that can be displayed when there
         * are more items in the [ContextualFlowRow] than can be displayed in the available space.
         * The "expandable indicator" composable can be tailored to show a summary, a button, or any
         * other composable element that indicates the presence of additional items.
         *
         * @param content composable that visually indicates more items can be loaded.
         */
        @ExperimentalLayoutApi
        fun expandIndicator(
            content: @Composable ContextualFlowRowOverflowScope.() -> Unit
        ): ContextualFlowRowOverflow {
            val seeMoreGetter = { state: FlowLayoutOverflowState ->
                @Composable {
                    val scope = ContextualFlowRowOverflowScopeImpl(state)
                    scope.content()
                }
            }
            return ContextualFlowRowOverflow(
                OverflowType.ExpandIndicator,
                seeMoreGetter = seeMoreGetter
            )
        }

        /**
         * Registers an "expand or collapse indicator" composable for handling overflow in a
         * [ContextualFlowRow].
         *
         * This function is designed to switch between two states: a "Expandable" state when there
         * are more items to show, and a "Collapsable" state when all items are being displayed and
         * can be collapsed.
         *
         * Prior to layout, the function evaluates both composables indicators to determine their
         * maximum intrinsic size. Depending on the space available and the number of items, either
         * the [expandIndicator] or the [collapseIndicator] is rendered.
         *
         * @sample androidx.compose.foundation.layout.samples.ContextualFlowRowMaxLineDynamicSeeMore
         * @param minRowsToShowCollapse Specifies the minimum number of rows that should be visible
         *   before showing the collapse option. This parameter is useful when the number of rows is
         *   too small to be reduced further.
         * @param minHeightToShowCollapse Specifies the minimum height at which the collapse option
         *   should be displayed. This parameter is useful for preventing the collapse option from
         *   appearing when the height is too narrow.
         * @param expandIndicator composable that visually indicates more items can be loaded.
         * @param collapseIndicator composable that visually indicates less items can be loaded.
         */
        @ExperimentalLayoutApi
        @Composable
        fun expandOrCollapseIndicator(
            expandIndicator: @Composable ContextualFlowRowOverflowScope.() -> Unit,
            collapseIndicator: @Composable ContextualFlowRowOverflowScope.() -> Unit,
            minRowsToShowCollapse: Int = 1,
            minHeightToShowCollapse: Dp = 0.dp,
        ): ContextualFlowRowOverflow {
            val minHeightToShowCollapsePx =
                with(LocalDensity.current) { minHeightToShowCollapse.roundToPx() }
            return remember(
                minRowsToShowCollapse,
                minHeightToShowCollapsePx,
                expandIndicator,
                collapseIndicator
            ) {
                val seeMoreGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = ContextualFlowRowOverflowScopeImpl(state)
                        scope.expandIndicator()
                    }
                }

                val collapseGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = ContextualFlowRowOverflowScopeImpl(state)
                        scope.collapseIndicator()
                    }
                }

                ContextualFlowRowOverflow(
                    OverflowType.ExpandOrCollapseIndicator,
                    minLinesToShowCollapse = minRowsToShowCollapse,
                    minCrossAxisSizeToShowCollapse = minHeightToShowCollapsePx,
                    seeMoreGetter = seeMoreGetter,
                    collapseGetter = collapseGetter
                )
            }
        }
    }
}

/**
 * Overflow Handling Options
 *
 * This enumeration defines the available options for handling content that exceeds the boundaries
 * of its container for [ContextualFlowColumn].
 *
 * Options:
 * - [Visible]: The overflowing content remains visible outside its container. This can lead to
 *   overlapping with other elements. Use this option when it's crucial to display all content,
 *   regardless of the container's size.
 * - [Clip]: The overflowing content is clipped and not visible beyond the boundary of its
 *   container. Ideal for maintaining a clean and uncluttered UI, where overlapping elements are
 *   undesirable.
 * - [expandIndicator]: Behaves similar to [Clip], however shows an indicator or button indicating
 *   that more items can be loaded.
 * - [expandOrCollapseIndicator]: Extends the [expandIndicator] functionality by adding a 'Collapse'
 *   option. After expanding the content, users can choose to collapse it back to the summary view.
 */
@ExperimentalLayoutApi
class ContextualFlowColumnOverflow
private constructor(
    type: OverflowType,
    minLinesToShowCollapse: Int = 0,
    minCrossAxisSizeToShowCollapse: Int = 0,
    seeMoreGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null,
    collapseGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null
) :
    FlowLayoutOverflow(
        type,
        minLinesToShowCollapse,
        minCrossAxisSizeToShowCollapse,
        seeMoreGetter,
        collapseGetter
    ) {

    companion object {
        /** Display all content, even if there is not enough space in the specified bounds. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Visible = ContextualFlowColumnOverflow(FlowLayoutOverflow.OverflowType.Visible)

        /** Clip the overflowing content to fix its container. */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @ExperimentalLayoutApi
        @get:ExperimentalLayoutApi
        val Clip = ContextualFlowColumnOverflow(FlowLayoutOverflow.OverflowType.Clip)

        /**
         * Registers an "expand indicator" composable for handling overflow in a
         * [ContextualFlowColumn].
         *
         * This function allows the creation of a custom composable that can be displayed when there
         * are more items in the [ContextualFlowColumn] than can be displayed in the available
         * space. The "expandable indicator" composable can be tailored to show a summary, a button,
         * or any other composable element that indicates the presence of additional items.
         *
         * @param content composable that visually indicates more items can be loaded.
         */
        @ExperimentalLayoutApi
        fun expandIndicator(
            content: @Composable ContextualFlowColumnOverflowScope.() -> Unit
        ): ContextualFlowColumnOverflow {
            val seeMoreGetter = { state: FlowLayoutOverflowState ->
                @Composable {
                    val scope = ContextualFlowColumnOverflowScopeImpl(state)
                    scope.content()
                }
            }
            return ContextualFlowColumnOverflow(
                OverflowType.ExpandIndicator,
                seeMoreGetter = seeMoreGetter
            )
        }

        /**
         * Registers an "expand or collapse indicator" composable for handling overflow in a
         * [ContextualFlowColumn].
         *
         * This function is designed to switch between two states: a "Expandable" state when there
         * are more items to show, and a "Collapsable" state when all items are being displayed and
         * can be collapsed.
         *
         * Prior to layout, the function evaluates both composables indicators to determine their
         * maximum intrinsic size. Depending on the space available and the number of items, either
         * the [expandIndicator] or the [collapseIndicator] is rendered.
         *
         * @sample androidx.compose.foundation.layout.samples.ContextualFlowColMaxLineDynamicSeeMore
         * @param minColumnsToShowCollapse Specifies the minimum number of columns that should be
         *   visible before showing the collapse option. This parameter is useful when the number of
         *   columns is too small to be reduced further.
         * @param minWidthToShowCollapse Specifies the minimum width at which the collapse option
         *   should be displayed. This parameter is useful for preventing the collapse option from
         *   appearing when the width is too narrow.
         * @param expandIndicator composable that visually indicates more items can be loaded.
         * @param collapseIndicator composable that visually indicates less items can be loaded.
         */
        @ExperimentalLayoutApi
        @Composable
        fun expandOrCollapseIndicator(
            expandIndicator: @Composable ContextualFlowColumnOverflowScope.() -> Unit,
            collapseIndicator: @Composable ContextualFlowColumnOverflowScope.() -> Unit,
            minColumnsToShowCollapse: Int = 1,
            minWidthToShowCollapse: Dp = 0.dp,
        ): ContextualFlowColumnOverflow {
            val minWidthToShowCollapsePx =
                with(LocalDensity.current) { minWidthToShowCollapse.roundToPx() }
            return remember(
                minColumnsToShowCollapse,
                minWidthToShowCollapsePx,
                expandIndicator,
                collapseIndicator
            ) {
                val seeMoreGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = ContextualFlowColumnOverflowScopeImpl(state)
                        scope.expandIndicator()
                    }
                }

                val collapseGetter = { state: FlowLayoutOverflowState ->
                    @Composable {
                        val scope = ContextualFlowColumnOverflowScopeImpl(state)
                        scope.collapseIndicator()
                    }
                }

                ContextualFlowColumnOverflow(
                    OverflowType.ExpandOrCollapseIndicator,
                    minLinesToShowCollapse = minColumnsToShowCollapse,
                    minCrossAxisSizeToShowCollapse = minWidthToShowCollapsePx,
                    seeMoreGetter = seeMoreGetter,
                    collapseGetter = collapseGetter
                )
            }
        }
    }
}

/**
 * Overflow Handling Options
 *
 * This enumeration defines the available options for handling content that exceeds the boundaries
 * of its container.
 *
 * Please check out the children classes on ways to initialize a FlowLayout overflow
 *
 * @see [FlowRowOverflow]
 * @see [FlowColumnOverflow]
 * @see [ContextualFlowRowOverflow]
 * @see [ContextualFlowColumnOverflow]
 */
@ExperimentalLayoutApi
sealed class FlowLayoutOverflow(
    internal val type: OverflowType,
    private val minLinesToShowCollapse: Int = 0,
    private val minCrossAxisSizeToShowCollapse: Int = 0,
    private val seeMoreGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null,
    private val collapseGetter: ((state: FlowLayoutOverflowState) -> @Composable () -> Unit)? = null
) {
    internal fun createOverflowState() =
        FlowLayoutOverflowState(type, minLinesToShowCollapse, minCrossAxisSizeToShowCollapse)

    internal fun addOverflowComposables(
        state: FlowLayoutOverflowState,
        list: MutableList<@Composable () -> Unit>
    ) {
        val expandIndicator = seeMoreGetter?.let { getter -> getter(state) }
        val collapseIndicator = collapseGetter?.let { getter -> getter(state) }

        when (type) {
            OverflowType.ExpandIndicator -> expandIndicator?.let { list.add(expandIndicator) }
            OverflowType.ExpandOrCollapseIndicator -> {
                expandIndicator?.let { list.add(expandIndicator) }
                collapseIndicator?.let { list.add(collapseIndicator) }
            }
            else -> {}
        }
    }

    internal enum class OverflowType {
        Visible,
        Clip,
        ExpandIndicator,
        ExpandOrCollapseIndicator,
    }
}

internal fun lazyInt(
    errorMessage: String = "Lazy item is not yet initialized",
    initializer: () -> Int
): Lazy<Int> = LazyImpl(initializer, errorMessage)

private class LazyImpl(val initializer: () -> Int, val errorMessage: String) : Lazy<Int> {
    private var _value: Int = UNINITIALIZED_VALUE
    override val value: Int
        get() {
            if (_value == UNINITIALIZED_VALUE) {
                _value = initializer()
            }
            if (_value == UNINITIALIZED_VALUE) {
                throw IllegalStateException(errorMessage)
            }
            return _value
        }

    override fun isInitialized(): Boolean = _value != UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else errorMessage

    companion object {
        internal const val UNINITIALIZED_VALUE: Int = -1
    }
}

/** Overflow State for managing overflow state within FlowLayouts. */
@OptIn(ExperimentalLayoutApi::class)
internal data class FlowLayoutOverflowState
internal constructor(
    internal val type: FlowLayoutOverflow.OverflowType,
    internal val minLinesToShowCollapse: Int,
    internal val minCrossAxisSizeToShowCollapse: Int
) {
    internal val shownItemCount: Int
        get() {
            if (itemShown == -1) {
                throw IllegalStateException(shownItemLazyErrorMessage)
            }
            return itemShown
        }

    internal val shownItemLazyErrorMessage =
        "Accessing shownItemCount before it is set. " +
            "Are you calling this in the Composition phase, " +
            "rather than in the draw phase? " +
            "Consider our samples on how to use it during the draw phase " +
            "or consider using ContextualFlowRow/ContextualFlowColumn " +
            "which initializes this method in the composition phase."

    internal var itemShown: Int = -1
    internal var itemCount = 0
    private var seeMoreMeasurable: Measurable? = null
    private var seeMorePlaceable: Placeable? = null
    private var collapseMeasurable: Measurable? = null
    private var collapsePlaceable: Placeable? = null
    private var seeMoreSize: IntIntPair? = null
    private var collapseSize: IntIntPair? = null
    // for contextual flow row
    private var getOverflowMeasurable:
        ((isExpandable: Boolean, noOfItemsShown: Int) -> Measurable?)? =
        null

    internal fun ellipsisSize(
        hasNext: Boolean,
        lineIndex: Int,
        totalCrossAxisSize: Int
    ): IntIntPair? {
        return when (type) {
            FlowLayoutOverflow.OverflowType.Visible -> null
            FlowLayoutOverflow.OverflowType.Clip -> null
            FlowLayoutOverflow.OverflowType.ExpandIndicator ->
                if (hasNext) {
                    seeMoreSize
                } else {
                    null
                }
            FlowLayoutOverflow.OverflowType.ExpandOrCollapseIndicator -> {
                if (hasNext) {
                    seeMoreSize
                } else if (
                    lineIndex + 1 >= minLinesToShowCollapse &&
                        totalCrossAxisSize >= minCrossAxisSizeToShowCollapse
                ) {
                    collapseSize
                } else {
                    null
                }
            }
        }
    }

    internal fun ellipsisInfo(
        hasNext: Boolean,
        lineIndex: Int,
        totalCrossAxisSize: Int
    ): FlowLayoutBuildingBlocks.WrapEllipsisInfo? {
        return when (type) {
            FlowLayoutOverflow.OverflowType.Visible -> null
            FlowLayoutOverflow.OverflowType.Clip -> null
            FlowLayoutOverflow.OverflowType.ExpandIndicator,
            FlowLayoutOverflow.OverflowType.ExpandOrCollapseIndicator -> {
                var measurable: Measurable? = null
                var placeable: Placeable? = null
                var ellipsisSize: IntIntPair?
                if (hasNext) {
                    measurable =
                        getOverflowMeasurable?.invoke(/* isExpandable */ true, shownItemCount)
                            ?: seeMoreMeasurable
                    ellipsisSize = seeMoreSize
                    if (getOverflowMeasurable == null) {
                        placeable = seeMorePlaceable
                    }
                } else {
                    if (
                        lineIndex >= (minLinesToShowCollapse - 1) &&
                            totalCrossAxisSize >= (minCrossAxisSizeToShowCollapse)
                    ) {
                        measurable =
                            getOverflowMeasurable?.invoke(/* isExpandable */ false, shownItemCount)
                                ?: collapseMeasurable
                    }
                    ellipsisSize = collapseSize
                    if (getOverflowMeasurable == null) {
                        placeable = collapsePlaceable
                    }
                }
                measurable ?: return null
                FlowLayoutBuildingBlocks.WrapEllipsisInfo(measurable, placeable, ellipsisSize!!)
            }
        }
    }

    internal fun setOverflowMeasurables(
        seeMoreMeasurable: IntrinsicMeasurable?,
        collapseMeasurable: IntrinsicMeasurable?,
        isHorizontal: Boolean,
        constraints: Constraints,
    ) {
        val orientation =
            if (isHorizontal) LayoutOrientation.Horizontal else LayoutOrientation.Vertical
        val orientationIndependentConstraints =
            OrientationIndependentConstraints(constraints, orientation)
        seeMoreMeasurable?.let { item ->
            val mainAxisSize =
                item.mainAxisMin(isHorizontal, orientationIndependentConstraints.crossAxisMax)
            val crossAxisSize = item.crossAxisMin(isHorizontal, mainAxisSize)
            this.seeMoreSize = IntIntPair(mainAxisSize, crossAxisSize)
            this.seeMoreMeasurable = item as? Measurable
            this.seeMorePlaceable = null
        }
        collapseMeasurable?.let { item ->
            val mainAxisSize =
                item.mainAxisMin(isHorizontal, orientationIndependentConstraints.crossAxisMax)
            val crossAxisSize = item.crossAxisMin(isHorizontal, mainAxisSize)
            this.collapseSize = IntIntPair(mainAxisSize, crossAxisSize)
            this.collapseMeasurable = item as? Measurable
            this.collapsePlaceable = null
        }
    }

    internal fun setOverflowMeasurables(
        measurePolicy: FlowLineMeasurePolicy,
        seeMoreMeasurable: Measurable?,
        collapseMeasurable: Measurable?,
        constraints: Constraints,
    ) {
        val isHorizontal = measurePolicy.isHorizontal
        val orientation =
            if (isHorizontal) LayoutOrientation.Horizontal else LayoutOrientation.Vertical
        val orientationIndependentConstraints =
            OrientationIndependentConstraints(constraints, orientation)
                .copy(mainAxisMin = 0, crossAxisMin = 0)
        val finalConstraints = orientationIndependentConstraints.toBoxConstraints(orientation)
        seeMoreMeasurable?.let { item ->
            item.measureAndCache(measurePolicy, finalConstraints) { placeable ->
                var mainAxisSize = 0
                var crossAxisSize = 0
                placeable?.let {
                    with(measurePolicy) {
                        mainAxisSize = placeable.mainAxisSize()
                        crossAxisSize = placeable.crossAxisSize()
                    }
                }
                seeMoreSize = IntIntPair(mainAxisSize, crossAxisSize)
                seeMorePlaceable = placeable
            }
            this.seeMoreMeasurable = item
        }
        collapseMeasurable?.let { item ->
            item.measureAndCache(measurePolicy, finalConstraints) { placeable ->
                var mainAxisSize = 0
                var crossAxisSize = 0
                placeable?.let {
                    with(measurePolicy) {
                        mainAxisSize = placeable.mainAxisSize()
                        crossAxisSize = placeable.crossAxisSize()
                    }
                }
                this.collapseSize = IntIntPair(mainAxisSize, crossAxisSize)
                collapsePlaceable = placeable
            }
            this.collapseMeasurable = item
        }
    }

    internal fun setOverflowMeasurables(
        measurePolicy: FlowLineMeasurePolicy,
        constraints: Constraints,
        getOverflowMeasurable: ((isExpandable: Boolean, numberOfItemsShown: Int) -> Measurable?)
    ) {
        this.itemShown = 0
        this.getOverflowMeasurable = getOverflowMeasurable
        val seeMoreMeasurable =
            getOverflowMeasurable(/* isExpandable */ true, /* numberOfItemsShown */ 0)
        val collapseMeasurable =
            getOverflowMeasurable(/* isExpandable */ false, /* numberOfItemsShown */ 0)
        setOverflowMeasurables(measurePolicy, seeMoreMeasurable, collapseMeasurable, constraints)
    }
}
