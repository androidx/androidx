/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Immutable

/**
 * Describes a padding to be applied along the edges inside a box. Use the various
 * [RemotePaddingValues] constructors for convenient ways to build instances.
 *
 * @param startPadding Padding at the start edge.
 * @param topPadding Padding at the top edge.
 * @param endPadding Padding at the end edge.
 * @param bottomPadding Padding at the bottom edge.
 */
@Immutable
public class RemotePaddingValues(
    public val startPadding: RemoteDp = 0.rdp,
    public val topPadding: RemoteDp = 0.rdp,
    public val endPadding: RemoteDp = 0.rdp,
    public val bottomPadding: RemoteDp = 0.rdp,
) {

    /** Creates a padding of [all] RemoteDp along all 4 edges. */
    public constructor(
        all: RemoteDp
    ) : this(startPadding = all, topPadding = all, endPadding = all, bottomPadding = all)

    /**
     * Creates a padding of [horizontal] RemoteDp along the start and end edges, and of [vertical]
     * RemoteDp along the top and bottom edges.
     */
    public constructor(
        horizontal: RemoteDp = 0.rdp,
        vertical: RemoteDp = 0.rdp,
    ) : this(
        startPadding = horizontal,
        topPadding = vertical,
        endPadding = horizontal,
        bottomPadding = vertical,
    )

    init {
        startPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Start padding must be non negative" }
        }
        topPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Top padding must be non negative" }
        }
        endPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "End padding must be non negative" }
        }
        bottomPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Bottom padding must be non negative" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RemotePaddingValues) return false

        // If any of the values is an expression, the paddings are not considered equals.
        if (
            !startPadding.value.hasConstantValue ||
                !topPadding.value.hasConstantValue ||
                !endPadding.value.hasConstantValue ||
                !bottomPadding.value.hasConstantValue ||
                !other.startPadding.value.hasConstantValue ||
                !other.topPadding.value.hasConstantValue ||
                !other.endPadding.value.hasConstantValue ||
                !other.bottomPadding.value.hasConstantValue
        ) {
            return false
        }
        return startPadding.value.constantValueOrNull ==
            other.startPadding.value.constantValueOrNull &&
            topPadding.value.constantValueOrNull == other.topPadding.value.constantValueOrNull &&
            endPadding.value.constantValueOrNull == other.endPadding.value.constantValueOrNull &&
            bottomPadding.value.constantValueOrNull == other.bottomPadding.value.constantValueOrNull
    }

    override fun hashCode(): Int =
        ((startPadding.hashCode() * 31 + topPadding.hashCode()) * 31 + endPadding.hashCode()) * 31 +
            bottomPadding.hashCode()

    override fun toString(): String =
        "RemotePaddingValues(start=$startPadding, top=$topPadding, end=$endPadding, bottom=$bottomPadding)"
}
