/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints

/**
 * Receiver scope being used by the item content parameter of LazyColumn/Row.
 */
@Stable
interface LazyItemScope {
    /**
     * Have the content fill the [Constraints.maxWidth] and [Constraints.maxHeight] of the parent
     * measurement constraints by setting the [minimum width][Constraints.minWidth] to be equal to the
     * [maximum width][Constraints.maxWidth] and the [minimum height][Constraints.minHeight] to be
     * equal to the [maximum height][Constraints.maxHeight].
     */
    fun Modifier.fillParentMaxSize(): Modifier

    /**
     * Have the content fill the [Constraints.maxWidth] of the parent measurement constraints
     * by setting the [minimum width][Constraints.minWidth] to be equal to the
     * [maximum width][Constraints.maxWidth].
     *
     * It intentionally shadows the modifier with the same signature from
     * [androidx.compose.foundation.layout] as the original one can't work inside the scrolling
     * content as the content is measured with [Constraints.Infinity] as the constraints for the
     * main axis.
     */
    fun Modifier.fillParentMaxWidth(): Modifier

    /**
     * Have the content fill the [Constraints.maxHeight] of the incoming measurement constraints
     * by setting the [minimum height][Constraints.minHeight] to be equal to the
     * [maximum height][Constraints.maxHeight].
     */
    fun Modifier.fillParentMaxHeight(): Modifier

    @Deprecated(
        "Modifier.fillMaxSize() can't work inside the scrolling layout like " +
                "LazyColumn/Row as the items are measured with Constraints.Infinity as the " +
                "constraints for the main axis. Please use instead Modifier.fillParentMaxSize() " +
                "available inside the scope",
        ReplaceWith("fillParentMaxSize()")
    )
    fun Modifier.fillMaxSize(): Modifier = fillParentMaxSize()

    @Deprecated(
        "Modifier.fillMaxWidth() can't work inside the scrolling layout like " +
                "LazyRow as the items are measured with Constraints.Infinity as the " +
                "constraints for the main axis. Please use instead Modifier.fillParentMaxWidth() " +
                "available inside the scope",
        ReplaceWith("fillParentMaxWidth()")
    )
    fun Modifier.fillMaxWidth(): Modifier = fillParentMaxWidth()

    @Deprecated(
        "Modifier.fillMaxHeight() can't work inside the scrolling layout like " +
                "LazyColumn as the items are measured with Constraints.Infinity as the " +
                "constraints for the main axis. Please use instead" +
                "Modifier.fillParentMaxHeight() available inside the scope",
        ReplaceWith("fillParentMaxHeight()")
    )
    fun Modifier.fillMaxHeight(): Modifier = fillParentMaxHeight()
}
