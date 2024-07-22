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

package androidx.window.embedding

import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * The parameter container to create an overlay [ActivityStack].
 *
 * If there's an shown overlay [ActivityStack] associated with the [tag], the existing
 * [ActivityStack] will be:
 * - Dismissed if the overlay [ActivityStack] is in different task from the launched one
 * - Updated with [OverlayAttributes] if the overlay [ActivityStack] is in the same task.
 *
 * See [android.os.Bundle.setOverlayCreateParams] for how to create an overlay [ActivityStack].
 *
 * @constructor creates a parameter container to launch an overlay [ActivityStack].
 * @property tag The unique identifier of the overlay [ActivityStack], which will be generated
 *   automatically if not specified.
 * @property overlayAttributes The attributes of the overlay [ActivityStack], which defaults to the
 *   default value of [OverlayAttributes].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class OverlayCreateParams
@JvmOverloads
constructor(
    val tag: String = generateOverlayTag(),
    val overlayAttributes: OverlayAttributes = OverlayAttributes.Builder().build(),
) {
    override fun toString(): String =
        "${OverlayCreateParams::class.simpleName}:{ " +
            ", tag=$tag" +
            ", attrs=$overlayAttributes" +
            "}"

    /** The [OverlayCreateParams] builder. */
    class Builder {
        private var tag: String? = null
        private var launchAttrs: OverlayAttributes? = null

        /**
         * Sets the overlay [ActivityStack]'s unique identifier. The builder will generate one
         * automatically if not specified.
         *
         * @param tag The unique identifier of the overlay [ActivityStack] to create.
         * @return The [OverlayCreateParams] builder.
         */
        fun setTag(tag: String): Builder = apply { this.tag = tag }

        /**
         * Sets the overlay [ActivityStack]'s attributes, which defaults to the default value of
         * [OverlayAttributes.Builder].
         *
         * @param attrs The [OverlayAttributes].
         * @return The [OverlayCreateParams] builder.
         */
        fun setOverlayAttributes(attrs: OverlayAttributes): Builder = apply { launchAttrs = attrs }

        /** Builds the [OverlayCreateParams] */
        fun build(): OverlayCreateParams =
            OverlayCreateParams(
                tag ?: generateOverlayTag(),
                launchAttrs ?: OverlayAttributes.Builder().build()
            )
    }

    companion object {

        /** A helper function to generate a random unique identifier. */
        @JvmStatic
        fun generateOverlayTag(): String = UUID.randomUUID().toString().substring(IntRange(0, 32))
    }
}
