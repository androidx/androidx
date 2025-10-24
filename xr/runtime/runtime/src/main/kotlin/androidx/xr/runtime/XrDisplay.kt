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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** Capability APIs related to the display and rendering on XR devices. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class XrDisplay {

    /**
     * A device capability that determines how virtual content is added to the real world (i.e. the
     * environment).
     */
    public class BlendMode private constructor(private val value: Int) {

        public companion object {
            /** Blending is not supported. */
            public val NOT_APPLICABLE: BlendMode = BlendMode(0)
            /**
             * Virtual content is added to the real world by adding the pixel values for each of
             * Red, Green, and Blue components. Alpha is ignored. Black pixels will appear
             * transparent.
             */
            public val ADDITIVE: BlendMode = BlendMode(1)
            /**
             * Virtual content is added to the real world by alpha blending the pixel values based
             * on the Alpha component.
             */
            public val ALPHA_BLEND: BlendMode = BlendMode(2)
        }

        public override fun toString(): String =
            when (this) {
                NOT_APPLICABLE -> "NOT_APPLICABLE"
                ADDITIVE -> "ADDITIVE"
                ALPHA_BLEND -> "ALPHA_BLEND"
                else -> "UNKNOWN"
            }
    }

    public companion object {
        /**
         * Returns the preferred blend mode for this session.
         *
         * @param session the [Session] to query the blend mode for.
         * @return The [BlendMode] that is preferred by [session] for rendering.
         *   [BlendMode.NOT_APPLICABLE] will be returned if there are no supported blend modes
         *   available.
         * @throws IllegalStateException if the [session] has been destroyed.
         */
        @JvmStatic
        public fun getPreferredBlendMode(session: Session): BlendMode {
            return if (session.runtimes.isEmpty()) {
                BlendMode.NOT_APPLICABLE
            } else {
                session.runtimes.firstNotNullOf { it.getPreferredBlendMode() }
            }
        }
    }
}
