/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.shapes

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.LayoutDirection

/**
 * A shape describing the rectangle with rounded corners.
 *
 * This shape will automatically mirror the corner sizes in [LayoutDirection.Rtl].
 *
 * @param topStart a size of the top start corner
 * @param topEnd a size of the top end corner
 * @param bottomEnd a size of the bottom end corner
 * @param bottomStart a size of the bottom start corner
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteRoundedCornerShape(
    topStart: RemoteCornerSize,
    topEnd: RemoteCornerSize,
    bottomEnd: RemoteCornerSize,
    bottomStart: RemoteCornerSize,
) :
    RemoteCornerBasedShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    ) {
    override fun createOutline(
        topStart: RemoteFloat,
        topEnd: RemoteFloat,
        bottomEnd: RemoteFloat,
        bottomStart: RemoteFloat,
    ): RemoteOutline {
        return RemoteOutline.Rounded(topStart, topEnd, bottomEnd, bottomStart)
    }
}

public val RemoteCircleShape: RemoteRoundedCornerShape = RemoteRoundedCornerShape(50)

public val RemoteRectangleShape: RemoteRoundedCornerShape = RemoteRoundedCornerShape(0.rf)

/**
 * Creates [RemoteRoundedCornerShape] with the same size applied for all four corners.
 *
 * @param corner [RemoteCornerSize] to apply.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(corner: RemoteCornerSize): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(corner, corner, corner, corner)

/**
 * Creates [RemoteRoundedCornerShape] with the same size applied for all four corners.
 *
 * @param size Size in [RemoteDp] to apply.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(size: RemoteDp): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(RemoteCornerSize(size))

/**
 * Creates [RemoteRoundedCornerShape] with the same size applied for all four corners.
 *
 * @param size Size in pixels to apply.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(size: RemoteFloat): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(RemoteCornerSize(size))

/**
 * Creates [RemoteRoundedCornerShape] with the same size applied for all four corners.
 *
 * @param percent Size in percents to apply.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(percent: Int): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(RemoteCornerSize(percent))

/** Creates [RemoteRoundedCornerShape] with sizes defined in [RemoteDp]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(
    topStart: RemoteDp = 0.rdp,
    topEnd: RemoteDp = 0.rdp,
    bottomEnd: RemoteDp = 0.rdp,
    bottomStart: RemoteDp = 0.rdp,
): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(
        topStart = RemoteCornerSize(topStart),
        topEnd = RemoteCornerSize(topEnd),
        bottomEnd = RemoteCornerSize(bottomEnd),
        bottomStart = RemoteCornerSize(bottomStart),
    )

/** Creates [RemoteRoundedCornerShape] with sizes defined in pixels. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(
    topStart: RemoteFloat = 0.rf,
    topEnd: RemoteFloat = 0.rf,
    bottomEnd: RemoteFloat = 0.rf,
    bottomStart: RemoteFloat = 0.rf,
): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(
        topStart = RemoteCornerSize(topStart),
        topEnd = RemoteCornerSize(topEnd),
        bottomEnd = RemoteCornerSize(bottomEnd),
        bottomStart = RemoteCornerSize(bottomStart),
    )

/**
 * Creates [RemoteRoundedCornerShape] with sizes defined in percents of the shape's smaller side.
 *
 * @param topStartPercent The top start corner radius as a percentage of the smaller side, with a
 *   range of 0 - 100.
 * @param topEndPercent The top end corner radius as a percentage of the smaller side, with a range
 *   of 0 - 100.
 * @param bottomEndPercent The bottom end corner radius as a percentage of the smaller side, with a
 *   range of 0 - 100.
 * @param bottomStartPercent The bottom start corner radius as a percentage of the smaller side,
 *   with a range of 0 - 100.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteRoundedCornerShape(
    @IntRange(from = 0, to = 100) topStartPercent: Int = 0,
    @IntRange(from = 0, to = 100) topEndPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomEndPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomStartPercent: Int = 0,
): RemoteRoundedCornerShape =
    RemoteRoundedCornerShape(
        topStart = RemoteCornerSize(topStartPercent),
        topEnd = RemoteCornerSize(topEndPercent),
        bottomEnd = RemoteCornerSize(bottomEndPercent),
        bottomStart = RemoteCornerSize(bottomStartPercent),
    )
