/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Arguments passed from the host orchestrator to a [BackupDeviceAction].
 *
 * The payload is a flat map of string keys to string values. [BackupActionInputKeys] documents
 * every key the actions bundled with this library understand, and [BackupActionValues] documents
 * the values those keys accept. Keys an action does not recognize are ignored, so one payload can
 * drive both phases of a flow. Custom [BackupDeviceAction] implementations may define keys of their
 * own.
 *
 * @property payload key-value arguments for the action, keyed by [BackupActionInputKeys]
 */
public class BackupDeviceActionArgs
@JvmOverloads
constructor(public val payload: Map<String, String> = emptyMap()) {
    /**
     * Returns the argument stored under [key], or `null` when the host did not supply it.
     *
     * Any key may be read, so a custom [BackupDeviceAction] can query keys of its own alongside the
     * [BackupActionInputKeys] constants.
     */
    public operator fun get(@BackupActionInputKey key: String): String? = payload[key]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupDeviceActionArgs) return false
        return payload == other.payload
    }

    override fun hashCode(): Int = payload.hashCode()

    override fun toString(): String = "BackupDeviceActionArgs(payload=$payload)"
}

/**
 * Results returned from a [BackupDeviceAction] back to the host orchestrator.
 *
 * [BackupActionOutputKeys] documents every key the actions bundled with this library produce.
 * Actions built with [success] and [failure] always report [BackupActionOutputKeys.STATUS], and
 * report [BackupActionOutputKeys.ERROR] only alongside a failure. Additional keys may be returned
 * to carry data back to the host, which forwards the whole payload to the caller.
 *
 * @property payload key-value results produced by the action, keyed by [BackupActionOutputKeys]
 */
public class BackupDeviceActionResult
@JvmOverloads
constructor(public val payload: Map<String, String> = emptyMap()) {
    /**
     * Returns the result stored under [key], or `null` when the action did not produce it.
     *
     * Any key may be read, so a custom [BackupDeviceAction] can return keys of its own alongside
     * the [BackupActionOutputKeys] constants.
     */
    public operator fun get(@BackupActionOutputKey key: String): String? = payload[key]

    /**
     * The reported [BackupActionOutputKeys.STATUS], or `null` for an action that does not report
     * one.
     */
    @get:BackupActionStatus
    public val status: String?
        get() = payload[BackupActionOutputKeys.STATUS]

    /**
     * Whether the action completed its work.
     *
     * Resolved in two steps:
     * - A reported [BackupActionOutputKeys.STATUS] decides on its own. Only
     *   [BackupActionValues.STATUS_FAILURE], compared without regard to case, means failure;
     *   `status` is a generic key that custom actions also use to publish their own vocabulary, so
     *   an unrecognized value is reported as successful rather than being guessed at.
     * - With no `status` at all, a non-empty [BackupActionOutputKeys.ERROR] means failure. An
     *   action that reports only an error would otherwise be read as passing.
     *
     * Use [BackupDeviceActionResult.failure] to report a failure that callers and the host will
     * honor.
     */
    public val isSuccess: Boolean
        get() {
            val status = status
            if (status != null) {
                return !status.equals(BackupActionValues.STATUS_FAILURE, ignoreCase = true)
            }
            return payload[BackupActionOutputKeys.ERROR].isNullOrEmpty()
        }

    /**
     * The reported [BackupActionOutputKeys.ERROR], or `null` when the action did not fail.
     *
     * Reported only for a result that [isSuccess] rejects. A successful action may legitimately
     * carry a [BackupActionOutputKeys.ERROR] entry of its own — an empty string, or a diagnostic
     * unrelated to the outcome — and surfacing that as a failure message would be misleading. This
     * mirrors the host, which reads the key under exactly the same conditions.
     *
     * @see isSuccess
     */
    public val errorMessage: String?
        get() = if (isSuccess) null else payload[BackupActionOutputKeys.ERROR]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupDeviceActionResult) return false
        return payload == other.payload
    }

    override fun hashCode(): Int = payload.hashCode()

    override fun toString(): String = "BackupDeviceActionResult(payload=$payload)"

    public companion object {
        /**
         * Returns a result reporting [BackupActionValues.STATUS_SUCCESS].
         *
         * @param payload additional data to return to the host; a [BackupActionOutputKeys.STATUS]
         *   entry it contains is overwritten
         */
        @JvmStatic
        @JvmOverloads
        public fun success(payload: Map<String, String> = emptyMap()): BackupDeviceActionResult =
            BackupDeviceActionResult(
                payload + (BackupActionOutputKeys.STATUS to BackupActionValues.STATUS_SUCCESS)
            )

        /**
         * Returns a result reporting [BackupActionValues.STATUS_FAILURE] together with
         * [errorMessage].
         *
         * @param errorMessage human-readable description of the failure, surfaced to the host as
         *   [BackupActionOutputKeys.ERROR]
         * @param payload additional data to return to the host; [BackupActionOutputKeys.STATUS] and
         *   [BackupActionOutputKeys.ERROR] entries it contains are overwritten
         */
        @JvmStatic
        @JvmOverloads
        public fun failure(
            errorMessage: String,
            payload: Map<String, String> = emptyMap(),
        ): BackupDeviceActionResult =
            BackupDeviceActionResult(
                payload +
                    mapOf(
                        BackupActionOutputKeys.STATUS to BackupActionValues.STATUS_FAILURE,
                        BackupActionOutputKeys.ERROR to errorMessage,
                    )
            )
    }
}

/**
 * Denotes that the annotated [Int] is a backup and restore lifecycle phase.
 *
 * Unlike [BackupActionStatus] this set is closed: the host dispatches on exactly these two phases.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Retention(AnnotationRetention.SOURCE)
@IntDef(BackupDeviceAction.PHASE_POPULATE, BackupDeviceAction.PHASE_VERIFY)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
)
public annotation class BackupActionPhase {
    public companion object {
        /** Seeds data inside the application sandbox before a backup. */
        public const val POPULATE: Int = BackupDeviceAction.PHASE_POPULATE

        /** Verifies restored data inside the application sandbox after a restore. */
        public const val VERIFY: Int = BackupDeviceAction.PHASE_VERIFY
    }
}

/**
 * Runs an action inside the application sandbox on the target device.
 *
 * Implementations must provide a public no-arg constructor to enable dynamic instantiation.
 *
 * An action reads its configuration from [BackupDeviceActionArgs], whose documented keys are listed
 * in [BackupActionInputKeys], and reports its outcome through [BackupDeviceActionResult], whose
 * documented keys are listed in [BackupActionOutputKeys]. Build the outcome with
 * [BackupDeviceActionResult.success] or [BackupDeviceActionResult.failure] so that the host can
 * tell the two apart.
 */
public interface BackupDeviceAction {
    public companion object {
        /** Seeds data inside the application sandbox before a backup. */
        public const val PHASE_POPULATE: Int = 1

        /** Verifies restored data inside the application sandbox after a restore. */
        public const val PHASE_VERIFY: Int = 2
    }

    /** Lifecycle phase when this action runs. */
    @get:BackupActionPhase public val phase: Int

    /**
     * Runs the action payload.
     *
     * Implementations should report a recoverable problem, such as a missing argument or a failed
     * assertion, by returning [BackupDeviceActionResult.failure] rather than by throwing. The host
     * treats a thrown exception as a crash of the action and reports it with a stack trace.
     *
     * @param context application context on the target device
     * @param args input arguments passed from the host orchestrator, keyed by
     *   [BackupActionInputKeys]
     * @return result returned to the host, keyed by [BackupActionOutputKeys]
     */
    public fun execute(context: Context, args: BackupDeviceActionArgs): BackupDeviceActionResult
}
