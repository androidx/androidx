/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.borders

import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.text.TextDirection

internal class MixedBorderRadius(
    override val topLeft: Radius,
    override val topRight: Radius,
    override val bottomLeft: Radius,
    override val bottomRight: Radius,
    override val topStart: Radius,
    override val topEnd: Radius,
    override val bottomStart: Radius,
    override val bottomEnd: Radius
) : BorderRadiusGeometry() {

    override fun unaryMinus(): MixedBorderRadius {
        return MixedBorderRadius(
            -topLeft,
            -topRight,
            -bottomLeft,
            -bottomRight,
            -topStart,
            -topEnd,
            -bottomStart,
            -bottomEnd
        )
    }

    /** Scales each corner of the [MixedBorderRadius] by the given factor. */
    override fun times(other: Float): MixedBorderRadius {
        return MixedBorderRadius(
            topLeft * other,
            topRight * other,
            bottomLeft * other,
            bottomRight * other,
            topStart * other,
            topEnd * other,
            bottomStart * other,
            bottomEnd * other
        )
    }

    override fun div(other: Float): MixedBorderRadius {
        return MixedBorderRadius(
            topLeft / other,
            topRight / other,
            bottomLeft / other,
            bottomRight / other,
            topStart / other,
            topEnd / other,
            bottomStart / other,
            bottomEnd / other
        )
    }

    override fun truncDiv(other: Float): MixedBorderRadius {
        return MixedBorderRadius(
            topLeft.truncDiv(other),
            topRight.truncDiv(other),
            bottomLeft.truncDiv(other),
            bottomRight.truncDiv(other),
            topStart.truncDiv(other),
            topEnd.truncDiv(other),
            bottomStart.truncDiv(other),
            bottomEnd.truncDiv(other)
        )
    }

    override fun rem(other: Float): MixedBorderRadius {
        return MixedBorderRadius(
            topLeft % other,
            topRight % other,
            bottomLeft % other,
            bottomRight % other,
            topStart % other,
            topEnd % other,
            bottomStart % other,
            bottomEnd % other
        )
    }

    override fun resolve(direction: TextDirection?): BorderRadius {
        assert(direction != null)
        when (direction!!) {
            TextDirection.Rtl ->
                return BorderRadius(
                    topLeft = topLeft + topEnd,
                    topRight = topRight + topStart,
                    bottomLeft = bottomLeft + bottomEnd,
                    bottomRight = bottomRight + bottomStart
                )
            TextDirection.Ltr ->
                return BorderRadius(
                    topLeft = topLeft + topStart,
                    topRight = topRight + topEnd,
                    bottomLeft = bottomLeft + bottomStart,
                    bottomRight = bottomRight + bottomEnd
                )
        }
    }
}
