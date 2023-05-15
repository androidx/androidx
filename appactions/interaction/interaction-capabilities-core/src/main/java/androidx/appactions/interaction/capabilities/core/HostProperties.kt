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

package androidx.appactions.interaction.capabilities.core

import android.util.SizeF
import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * HostProperties contains information about the connected assistant's environment which can be
 * used to customize behaviour for the different assistant contexts.
 *
 * @property maxHostSizeDp the dimensions of the host area where the app content will be displayed.
 */
class HostProperties internal constructor(val maxHostSizeDp: SizeF) {
    override fun toString() =
        "HostProperties(maxHostSizeDp=$maxHostSizeDp)"

    override fun equals(other: Any?): Boolean {
        return other is HostProperties && maxHostSizeDp == other.maxHostSizeDp
    }

    override fun hashCode() = Objects.hash(maxHostSizeDp)

    /**
     * Builder class for [HostProperties].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder {
        private var maxHostSizeDp: SizeF? = null

        /** Sets the dimensions of the host area where the app content will be displayed in dp. */
        fun setMaxHostSizeDp(maxHostSizeDp: SizeF) = apply {
            this.maxHostSizeDp = maxHostSizeDp
        }

        /**
         * Builds and returns the HostProperties instance.
         */
        fun build() = HostProperties(
            requireNotNull(maxHostSizeDp, { "maxHostSizeDp must be set." }),
        )
    }
}
