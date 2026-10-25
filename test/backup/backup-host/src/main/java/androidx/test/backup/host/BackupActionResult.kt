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

package androidx.test.backup.host

import java.io.IOException
import java.util.Collections

/**
 * Represents the result of a delegated device payload execution.
 *
 * Note: This sealed class is open to future additions. Consumers should use a non-exhaustive 'when'
 * pattern-matching approach (incorporating an explicit 'else' branch) to ensure binary
 * compatibility when new subclasses are introduced.
 */
public sealed class BackupActionResult {
    /**
     * Data that the action returned, such as values it read on the device. Empty if the action
     * returned none or didn't run to completion. The data of a [Failure] leaves out the `status`
     * and `error` of the action, which [Failure.errorMessage] reports. A result keeps a copy of the
     * data that it's created with.
     */
    public abstract val data: Map<String, String>

    /** Returns true if this is a Success result. */
    public val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns the [data] of a [Success], or throws an [IOException] for a [Failure], as
     * [BackupRestoreController.runBackupRestoreFlow] does when one of its actions fails.
     *
     * The exception message has the [Failure.errorMessage] and the [Failure.stackTrace] that the
     * device sent. To read a [Failure] itself, such as its [data], check the type of this result
     * instead.
     *
     * @throws IOException if this is a [Failure]
     */
    @Throws(IOException::class)
    public fun getOrThrow(): Map<String, String> {
        if (this is Failure) {
            throw IOException(
                if (stackTrace.isNullOrBlank()) errorMessage else "$errorMessage\n$stackTrace"
            )
        }
        return data
    }

    /**
     * A private subtype to prevent exhaustive when expressions on external clients, forcing
     * consumers to define an 'else' branch.
     */
    @Suppress("unused")
    private object Unknown : BackupActionResult() {
        override val data: Map<String, String>
            get() = emptyMap()
    }

    /** Indicates that the device payload executed and verified successfully. */
    public class Success @JvmOverloads constructor(data: Map<String, String> = emptyMap()) :
        BackupActionResult() {
        override val data: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(data))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return data == other.data
        }

        override fun hashCode(): Int {
            return data.hashCode()
        }

        override fun toString(): String {
            return "Success(data=$data)"
        }
    }

    /**
     * Indicates that the device payload failed during execution or verification.
     *
     * @property errorMessage The high-level error message or exception summary.
     * @property stackTrace The optional serialized stack trace of any caught device-side
     *   exceptions.
     */
    public class Failure
    @JvmOverloads
    constructor(
        public val errorMessage: String,
        public val stackTrace: String? = null,
        data: Map<String, String> = emptyMap(),
    ) : BackupActionResult() {
        override val data: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(data))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Failure) return false
            return errorMessage == other.errorMessage &&
                stackTrace == other.stackTrace &&
                data == other.data
        }

        override fun hashCode(): Int {
            var result = errorMessage.hashCode()
            result = 31 * result + (stackTrace?.hashCode() ?: 0)
            result = 31 * result + data.hashCode()
            return result
        }

        override fun toString(): String {
            return "Failure(errorMessage=$errorMessage, stackTrace=$stackTrace, data=$data)"
        }
    }
}
