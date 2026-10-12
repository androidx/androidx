/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Represents the runtime state of the `LowLightBoostEffect`.
 *
 * This state tells whether Low Light Boost is disabled, enabled, brightening a dark scene, waiting
 * for the Google Play Services module, or can't be applied (see [ERROR]).
 *
 * @property type The current runtime state type of the effect. One of [OFF], [INACTIVE], [ACTIVE],
 *   [PENDING_INSTALLATION], [ERROR].
 * @property error The [Exception] associated with [ERROR] (for example, a Google Play Services
 *   `ApiException` carrying a status code, or an [IllegalArgumentException] for unsupported
 *   resolutions), or `null` if not in an error state.
 */
// TODO: b/448524973 - Remove @RestrictTo when making this API public in next alpha.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LowLightBoostEffectState
internal constructor(@get:Type public val type: Int, public val error: Exception? = null) {
    init {
        require((type == ERROR) == (error != null)) {
            "Error exception must be non-null if and only if type is ERROR " +
                "(type=${typeToString(type)}, error=$error)."
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(OFF, INACTIVE, ACTIVE, PENDING_INSTALLATION, ERROR)
    public annotation class Type

    public companion object {
        /**
         * The Low Light Boost effect is disabled or has not yet been initialized.
         *
         * Frames are passed through without enhancement.
         */
        public const val OFF: Int = -1

        /**
         * The effect is enabled, but the scene is bright enough that no noticeable boost is
         * applied.
         */
        public const val INACTIVE: Int = 0

        /**
         * The effect is enabled, and the scene is dark enough that Low Light Boost brightens it.
         */
        public const val ACTIVE: Int = 1

        /**
         * The required Play Services module is currently being downloaded and installed in the
         * background.
         *
         * The effect is bypassed, and frames are passed through without enhancement until
         * installation completes.
         */
        public const val PENDING_INSTALLATION: Int = 2

        /**
         * Low Light Boost can't be applied, e.g. because the device, the camera or the stream
         * resolution isn't supported, the Google Play Services module couldn't be installed, or a
         * session couldn't be created.
         *
         * Frames are passed through without enhancement. See [error] for the cause. Some errors are
         * transient: the effect may recover automatically, e.g. after Google Play Services
         * restarts, in which case the state changes to [INACTIVE] or [ACTIVE]. Disabling and then
         * enabling Low Light Boost starts a new attempt. Some errors, e.g. an unsupported device,
         * camera or stream resolution, persist for the current binding.
         */
        public const val ERROR: Int = 3

        // States without an error are immutable, so they are shared.
        private val STATE_OFF = LowLightBoostEffectState(OFF)
        private val STATE_INACTIVE = LowLightBoostEffectState(INACTIVE)
        private val STATE_ACTIVE = LowLightBoostEffectState(ACTIVE)
        private val STATE_PENDING_INSTALLATION = LowLightBoostEffectState(PENDING_INSTALLATION)

        /** Returns the [OFF] state. */
        internal fun createOff(): LowLightBoostEffectState = STATE_OFF

        /** Returns the [INACTIVE] state. */
        internal fun createInactive(): LowLightBoostEffectState = STATE_INACTIVE

        /** Returns the [ACTIVE] state. */
        internal fun createActive(): LowLightBoostEffectState = STATE_ACTIVE

        /** Returns the [PENDING_INSTALLATION] state. */
        internal fun createPendingInstallation(): LowLightBoostEffectState =
            STATE_PENDING_INSTALLATION

        /** Creates an [ERROR] state with the given [error]. */
        internal fun createError(error: Exception): LowLightBoostEffectState =
            LowLightBoostEffectState(ERROR, error)

        /** Returns a human-readable string representation of a [Type] constant for logging. */
        internal fun typeToString(@Type type: Int): String =
            when (type) {
                OFF -> "OFF"
                INACTIVE -> "INACTIVE"
                ACTIVE -> "ACTIVE"
                PENDING_INSTALLATION -> "PENDING_INSTALLATION"
                ERROR -> "ERROR"
                else -> "UNKNOWN($type)"
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LowLightBoostEffectState) return false
        // [error] is compared with equals(), i.e. by identity for exceptions that don't override
        // it, so two such ERROR states are equal only if they carry the same exception instance.
        return type == other.type && error == other.error
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "LowLightBoostEffectState(type=${typeToString(type)}, error=$error)"
}
