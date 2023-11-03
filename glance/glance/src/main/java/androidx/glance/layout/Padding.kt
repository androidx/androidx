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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end] and
 * [bottom]. The start and end edges will be determined by layout direction of the current locale.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * If any value is not defined, it will be [0.dp] or whatever value was defined by an earlier
 * modifier.
 */
fun GlanceModifier.padding(
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
fun GlanceModifier.padding(
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
fun GlanceModifier.padding(
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
fun GlanceModifier.padding(
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
fun GlanceModifier.padding(all: Dp): GlanceModifier {
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
fun GlanceModifier.padding(@DimenRes all: Int): GlanceModifier {
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
fun GlanceModifier.absolutePadding(
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
fun GlanceModifier.absolutePadding(
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun GlanceModifier.collectPadding(): PaddingModifier? =
    foldIn<PaddingModifier?>(null) { acc, modifier ->
        if (modifier is PaddingModifier) {
            (acc ?: PaddingModifier()) + modifier
        } else {
            acc
        }
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun GlanceModifier.collectPaddingInDp(resources: Resources) =
    collectPadding()?.toDp(resources)

private fun List<Int>.toDp(resources: Resources) =
    fold(0.dp) { acc, res ->
        acc + (resources.getDimension(res) / resources.displayMetrics.density).dp
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaddingModifier(
    val left: PaddingDimension = PaddingDimension(),
    val start: PaddingDimension = PaddingDimension(),
    val top: PaddingDimension = PaddingDimension(),
    val right: PaddingDimension = PaddingDimension(),
    val end: PaddingDimension = PaddingDimension(),
    val bottom: PaddingDimension = PaddingDimension(),
) : GlanceModifier.Element {

    operator fun plus(other: PaddingModifier) =
        PaddingModifier(
            left = left + other.left,
            start = start + other.start,
            top = top + other.top,
            right = right + other.right,
            end = end + other.end,
            bottom = bottom + other.bottom,
        )

    fun toDp(resources: Resources): PaddingInDp =
        PaddingInDp(
            left = left.dp + left.resourceIds.toDp(resources),
            start = start.dp + start.resourceIds.toDp(resources),
            top = top.dp + top.resourceIds.toDp(resources),
            right = right.dp + right.resourceIds.toDp(resources),
            end = end.dp + end.resourceIds.toDp(resources),
            bottom = bottom.dp + bottom.resourceIds.toDp(resources),
        )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaddingDimension(
    val dp: Dp = 0.dp,
    val resourceIds: List<Int> = emptyList(),
) {
    constructor(@DimenRes resource: Int) : this(resourceIds = listOf(resource))

    operator fun plus(other: PaddingDimension) =
        PaddingDimension(
            dp = dp + other.dp,
            resourceIds = resourceIds + other.resourceIds,
        )

    companion object {
        val Zero = PaddingDimension()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaddingInDp(
    val left: Dp = 0.dp,
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
) {
    /** Transfer [start] / [end] to [left] / [right] depending on [isRtl]. */
    fun toAbsolute(isRtl: Boolean) =
        PaddingInDp(
            left = left + if (isRtl) end else start,
            top = top,
            right = right + if (isRtl) start else end,
            bottom = bottom,
        )

    /** Transfer [left] / [right] to [start] / [end] depending on [isRtl]. */
    fun toRelative(isRtl: Boolean) =
        PaddingInDp(
            start = start + if (isRtl) right else left,
            top = top,
            end = end + if (isRtl) left else right,
            bottom = bottom
        )
}
