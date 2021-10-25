/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.glance.layout

import android.content.res.Resources
import androidx.annotation.DimenRes
import androidx.annotation.RestrictTo
import androidx.glance.GlanceModifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end] and
 * [bottom]. The start and end edges will be determined by layout direction of the current locale.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * If any value is not defined, it will be [0.dp] or whatever value was defined by an earlier
 * modifier.
 */
public fun GlanceModifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): GlanceModifier = this.then(
    PaddingModifier(
        start = start.toPadding(),
        top = top.toPadding(),
        end = end.toPadding(),
        bottom = bottom.toPadding(),
    )
)

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end] and
 * [bottom]. The start and end edges will be determined by layout direction of the current locale.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * If any value is not defined, it will be [0.dp] or whatever value was defined by an earlier
 * modifier.
 */
public fun GlanceModifier.padding(
    @DimenRes start: Int = 0,
    @DimenRes top: Int = 0,
    @DimenRes end: Int = 0,
    @DimenRes bottom: Int = 0
): GlanceModifier = this.then(
    PaddingModifier(
        start = start.toPadding(),
        top = top.toPadding(),
        end = end.toPadding(),
        bottom = bottom.toPadding(),
    )
)

/**
 * Apply [horizontal] dp space along the left and right edges of the content, and [vertical] dp
 * space along the top and bottom edges.
 *
 * If any value is not defined, it will be [0.dp] or whatever value was defined by an earlier
 * modifier.
 */
public fun GlanceModifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
): GlanceModifier = this.then(
    PaddingModifier(
        start = horizontal.toPadding(),
        top = vertical.toPadding(),
        end = horizontal.toPadding(),
        bottom = vertical.toPadding(),
    )
)

/**
 * Apply [horizontal] dp space along the left and right edges of the content, and [vertical] dp
 * space along the top and bottom edges.
 *
 * If any value is not defined, it will be [0.dp] or whatever value was defined by an earlier
 * modifier.
 */
public fun GlanceModifier.padding(
    @DimenRes horizontal: Int = 0,
    @DimenRes vertical: Int = 0
): GlanceModifier = this.then(
    PaddingModifier(
        start = horizontal.toPadding(),
        top = vertical.toPadding(),
        end = horizontal.toPadding(),
        bottom = vertical.toPadding(),
    )
)

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right and bottom.
 */
public fun GlanceModifier.padding(all: Dp): GlanceModifier {
    val allDp = all.toPadding()
    return this.then(
        PaddingModifier(
            start = allDp,
            top = allDp,
            end = allDp,
            bottom = allDp,
        )
    )
}

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right and bottom.
 */
public fun GlanceModifier.padding(@DimenRes all: Int): GlanceModifier {
    val allDp = all.toPadding()
    return this.then(
        PaddingModifier(
            start = allDp,
            top = allDp,
            end = allDp,
            bottom = allDp,
        )
    )
}

/**
 *  Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom], ignoring the current locale's layout direction.
 */
public fun GlanceModifier.absolutePadding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
): GlanceModifier = this.then(
    PaddingModifier(
        left = left.toPadding(),
        top = top.toPadding(),
        right = right.toPadding(),
        bottom = bottom.toPadding(),
    )
)

/**
 *  Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom], ignoring the current locale's layout direction.
 */
public fun GlanceModifier.absolutePadding(
    @DimenRes left: Int = 0,
    @DimenRes top: Int = 0,
    @DimenRes right: Int = 0,
    @DimenRes bottom: Int = 0
): GlanceModifier = this.then(
    PaddingModifier(
        left = left.toPadding(),
        top = top.toPadding(),
        right = right.toPadding(),
        bottom = bottom.toPadding(),
    )
)

private fun Dp.toPadding() =
    PaddingDimension(dp = this)

private fun Int.toPadding() =
    if (this == 0) PaddingDimension() else PaddingDimension(this)

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceModifier.collectPadding(): PaddingModifier? =
    foldIn<PaddingModifier?>(null) { acc, modifier ->
        if (modifier is PaddingModifier) {
            (acc ?: PaddingModifier()) + modifier
        } else {
            acc
        }
    }

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun GlanceModifier.collectPaddingInDp(resources: Resources) =
    collectPadding()?.let { padding ->
        PaddingInDp(
            left = padding.left.dp + padding.left.resources.toDp(resources),
            start = padding.start.dp + padding.start.resources.toDp(resources),
            top = padding.top.dp + padding.top.resources.toDp(resources),
            right = padding.right.dp + padding.right.resources.toDp(resources),
            end = padding.end.dp + padding.end.resources.toDp(resources),
            bottom = padding.bottom.dp + padding.bottom.resources.toDp(resources),
        )
    }

private fun List<Int>.toDp(resources: Resources) =
    fold(0.dp) { acc, res ->
        acc + (resources.getDimension(res) / resources.displayMetrics.density).dp
    }

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class PaddingModifier(
    public val left: PaddingDimension = PaddingDimension(),
    public val start: PaddingDimension = PaddingDimension(),
    public val top: PaddingDimension = PaddingDimension(),
    public val right: PaddingDimension = PaddingDimension(),
    public val end: PaddingDimension = PaddingDimension(),
    public val bottom: PaddingDimension = PaddingDimension(),
) : GlanceModifier.Element {

    public operator fun plus(other: PaddingModifier) =
        PaddingModifier(
            left = left + other.left,
            start = start + other.start,
            top = top + other.top,
            right = right + other.right,
            end = end + other.end,
            bottom = bottom + other.bottom,
        )
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class PaddingDimension(
    public val dp: Dp = 0.dp,
    public val resources: List<Int> = emptyList(),
) {
    constructor(@DimenRes resource: Int) : this(resources = listOf(resource))

    public operator fun plus(other: PaddingDimension) =
        PaddingDimension(
            dp = dp + other.dp,
            resources = resources + other.resources,
        )

    companion object {
        val Zero = PaddingDimension()
    }
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class PaddingInDp(
    public val left: Dp = 0.dp,
    public val start: Dp = 0.dp,
    public val top: Dp = 0.dp,
    public val right: Dp = 0.dp,
    public val end: Dp = 0.dp,
    public val bottom: Dp = 0.dp,
) {
    /** Transfer [start] / [end] to [left] / [right] depending on [isRtl]. */
    public fun toAbsolute(isRtl: Boolean) =
        PaddingInDp(
            left = left + if (isRtl) end else start,
            top = top,
            right = right + if (isRtl) start else end,
            bottom = bottom,
        )

    /** Transfer [left] / [right] to [start] / [end] depending on [isRtl]. */
    public fun toRelative(isRtl: Boolean) =
        PaddingInDp(
            start = start + if (isRtl) right else left,
            top = top,
            end = end + if (isRtl) left else right,
            bottom = bottom
        )
}
