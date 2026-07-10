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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp

/**
 * Describes a padding to be applied along the edges inside a box. Use the various
 * [RemotePaddingValues] constructors for convenient ways to build instances.
 */
public class RemotePaddingValues(
    public val leftPadding: RemoteDp = 0.rdp,
    public val topPadding: RemoteDp = 0.rdp,
    public val rightPadding: RemoteDp = 0.rdp,
    public val bottomPadding: RemoteDp = 0.rdp,
) {

    /** Creates a padding of [all] RemoteDp along all 4 edges. */
    public constructor(
        all: RemoteDp
    ) : this(leftPadding = all, topPadding = all, rightPadding = all, bottomPadding = all)

    /**
     * Creates a padding of [horizontal] RemoteDp along the left and right edges, and of [vertical]
     * RemoteDp along the top and bottom edges.
     */
    public constructor(
        horizontal: RemoteDp = 0.rdp,
        vertical: RemoteDp = 0.rdp,
    ) : this(
        leftPadding = horizontal,
        topPadding = vertical,
        rightPadding = horizontal,
        bottomPadding = vertical,
    )

    init {
        leftPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Left padding must be non negative" }
        }
        topPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Top padding must be non negative" }
        }
        rightPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Right padding must be non negative" }
        }
        bottomPadding.value.constantValueOrNull?.let {
            require(it >= 0f) { "Bottom padding must be non negative" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RemotePaddingValues) return false

        // If any of the values is an expression, the paddings are not considered equals.
        if (
            !leftPadding.value.hasConstantValue ||
                !topPadding.value.hasConstantValue ||
                !rightPadding.value.hasConstantValue ||
                !bottomPadding.value.hasConstantValue ||
                !other.leftPadding.value.hasConstantValue ||
                !other.topPadding.value.hasConstantValue ||
                !other.rightPadding.value.hasConstantValue ||
                !other.bottomPadding.value.hasConstantValue
        ) {
            return false
        }
        return leftPadding.value.constantValueOrNull ==
            other.leftPadding.value.constantValueOrNull &&
            topPadding.value.constantValueOrNull == other.topPadding.value.constantValueOrNull &&
            rightPadding.value.constantValueOrNull ==
                other.rightPadding.value.constantValueOrNull &&
            bottomPadding.value.constantValueOrNull == other.bottomPadding.value.constantValueOrNull
    }

    override fun hashCode(): Int =
        ((leftPadding.hashCode() * 31 + topPadding.hashCode()) * 31 + rightPadding.hashCode()) *
            31 + bottomPadding.hashCode()

    override fun toString(): String =
        "RemotePaddingValues(left=$leftPadding, top=$topPadding, right=$rightPadding, bottom=$bottomPadding)"
}
