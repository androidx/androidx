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

/**
 * An immutable pair of the [LowLightBoostEffect.status] and [LowLightBoostEffect.error] values, so
 * that both are posted, committed and read together.
 *
 * A non-null [error] is only allowed with [LowLightBoostEffect.STATUS_OFF]: on failure, frames are
 * passed through without enhancement, like when Low Light Boost is off.
 *
 * @property status One of the [LowLightBoostEffect.Status] constants.
 * @property error The [LowLightBoostException] that prevents Low Light Boost, or `null` if there is
 *   none.
 */
internal class LowLightBoostStateSnapshot(
    @get:LowLightBoostEffect.Status val status: Int,
    val error: LowLightBoostException? = null,
) {
    init {
        require(error == null || status == LowLightBoostEffect.STATUS_OFF) {
            "A non-null error requires STATUS_OFF (status=${statusToString(status)}, " +
                "error=$error)."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LowLightBoostStateSnapshot) return false
        // [error] is compared with equals(), i.e. by identity for exceptions that don't override
        // it, so a new failure is a new state even if its exception has the same message.
        return status == other.status && error == other.error
    }

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "LowLightBoostStateSnapshot(status=${statusToString(status)}, error=$error)"

    companion object {
        // Snapshots without an error are immutable, so they are shared.

        /** [LowLightBoostEffect.STATUS_OFF] without an error. */
        val OFF = LowLightBoostStateSnapshot(LowLightBoostEffect.STATUS_OFF)

        /** [LowLightBoostEffect.STATUS_INACTIVE]. */
        val INACTIVE = LowLightBoostStateSnapshot(LowLightBoostEffect.STATUS_INACTIVE)

        /** [LowLightBoostEffect.STATUS_ACTIVE]. */
        val ACTIVE = LowLightBoostStateSnapshot(LowLightBoostEffect.STATUS_ACTIVE)

        /** [LowLightBoostEffect.STATUS_PENDING_INSTALLATION]. */
        val PENDING_INSTALLATION =
            LowLightBoostStateSnapshot(LowLightBoostEffect.STATUS_PENDING_INSTALLATION)

        /** Creates a failed snapshot: [LowLightBoostEffect.STATUS_OFF] with [error]. */
        fun error(error: LowLightBoostException): LowLightBoostStateSnapshot =
            LowLightBoostStateSnapshot(LowLightBoostEffect.STATUS_OFF, error)

        /** Returns a human-readable name of a [LowLightBoostEffect.Status] constant for logging. */
        fun statusToString(@LowLightBoostEffect.Status status: Int): String =
            when (status) {
                LowLightBoostEffect.STATUS_OFF -> "OFF"
                LowLightBoostEffect.STATUS_INACTIVE -> "INACTIVE"
                LowLightBoostEffect.STATUS_ACTIVE -> "ACTIVE"
                LowLightBoostEffect.STATUS_PENDING_INSTALLATION -> "PENDING_INSTALLATION"
                else -> "UNKNOWN($status)"
            }
    }
}
