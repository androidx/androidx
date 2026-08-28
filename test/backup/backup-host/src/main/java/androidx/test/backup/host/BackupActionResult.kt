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

/**
 * Represents the result of a delegated device payload execution.
 *
 * Note: This sealed class is open to future additions. Consumers should use a non-exhaustive 'when'
 * pattern-matching approach (incorporating an explicit 'else' branch) to ensure binary
 * compatibility when new subclasses are introduced.
 */
public sealed class BackupActionResult {
    /** Returns true if this is a Success result. */
    public val isSuccess: Boolean
        get() = this is Success

    /**
     * A private subtype to prevent exhaustive when expressions on external clients, forcing
     * consumers to define an 'else' branch.
     */
    @Suppress("unused") private object Unknown : BackupActionResult()

    /**
     * Indicates that the device payload executed and verified successfully.
     *
     * @property data A read-only map of key-value string pairs representing serialized diagnostic
     *   results, returned system status, or database/preferences validation records captured
     *   on-device by the executing action payload (e.g. key-values checked or row-counts matched).
     */
    public class Success
    @JvmOverloads
    constructor(public val data: Map<String, String> = emptyMap()) : BackupActionResult() {
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
    constructor(public val errorMessage: String, public val stackTrace: String? = null) :
        BackupActionResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Failure) return false
            return errorMessage == other.errorMessage && stackTrace == other.stackTrace
        }

        override fun hashCode(): Int {
            var result = errorMessage.hashCode()
            result = 31 * result + (stackTrace?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Failure(errorMessage=$errorMessage, stackTrace=$stackTrace)"
        }
    }
}
