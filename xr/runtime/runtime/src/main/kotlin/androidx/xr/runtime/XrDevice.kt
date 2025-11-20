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

/** Provides hardware capabilities of the device. */
public class XrDevice private constructor(private val session: Session) {

    /** A device capability that determines how virtual content is added to the real world. */
    public class DisplayBlendMode private constructor(private val value: Int) {

        public companion object {
            /** Blending is not supported. */
            @JvmField public val NO_DISPLAY: DisplayBlendMode = DisplayBlendMode(0)
            /**
             * Virtual content is added to the real world by adding the pixel values for each of
             * Red, Green, and Blue components. Alpha is ignored. Black pixels will appear
             * transparent.
             */
            @JvmField public val ADDITIVE: DisplayBlendMode = DisplayBlendMode(1)
            /**
             * Virtual content is added to the real world by alpha blending the pixel values based
             * on the Alpha component.
             */
            @JvmField public val ALPHA_BLEND: DisplayBlendMode = DisplayBlendMode(2)
        }

        public override fun toString(): String =
            when (this) {
                NO_DISPLAY -> "NOT_APPLICABLE"
                ADDITIVE -> "ADDITIVE"
                ALPHA_BLEND -> "ALPHA_BLEND"
                else -> "UNKNOWN"
            }
    }

    public companion object {

        /**
         * Get the current [XrDevice] for the provided [Session].
         *
         * @param session the [Session] connected to the device.
         */
        @JvmStatic public fun getCurrentDevice(session: Session): XrDevice = XrDevice(session)
    }

    /**
     * Returns the preferred display blend mode for this session.
     *
     * @return The [DisplayBlendMode] that is preferred by the [Session] for rendering.
     *   [DisplayBlendMode.NO_DISPLAY] will be returned if there are no supported blend modes
     *   available.
     * @throws IllegalStateException if the [Session] has been destroyed.
     */
    public fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        return if (session.runtimes.isEmpty()) {
            DisplayBlendMode.NO_DISPLAY
        } else {
            session.runtimes.firstNotNullOf { it.getPreferredDisplayBlendMode() }
        }
    }
}
