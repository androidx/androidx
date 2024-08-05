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

package androidx.core.telecom

import java.util.Objects

/**
 * CallControlResult is a return value that represents the result of one of the following
 * [CallControlScope] methods:
 * - [CallControlScope.setActive]
 * - [CallControlScope.answer]
 * - [CallControlScope.disconnect]
 * - [CallControlScope.setInactive]
 * - [CallControlScope.requestEndpointChange]
 *
 * Each of the above listed methods has the ability to fail and if it does, this will be represented
 * by a [CallControlResult.Error] (e.g. Telecom was not able to change the call route via
 * requestEndpointChange). Otherwise, [CallControlResult.Success] is returned to represent the
 * operation succeeded (e.g Telecom was able to set the call active).
 *
 * Example usage:
 * ```
 * launch {
 *     when(val result = setActive()) {
 *       is CallControlResult.Success -> {
 *           Log.d(TAG, "onSetActive - ${result}")
 *           // move call to active state locally
 *       }
 *       is CallControlResult.Failure -> {
 *           Log.w(TAG, "onSetActive - ${result}")
 *          // surface error to user if required
 *       }
 * }
 * ````
 */
public sealed class CallControlResult {
    /**
     * The associated [CallControlScope] method was successful. For example, if
     * [CallControlScope.setActive] was requested, Telecom was able to change the call state.
     */
    public class Success : CallControlResult() {
        override fun toString(): String {
            return "CallControlResult(Success)"
        }

        override fun equals(other: Any?): Boolean {
            return other is Success
        }

        override fun hashCode(): Int {
            return Objects.hash()
        }
    }

    /**
     * The associated [CallControlScope] method failed. For example, if [CallControlScope.setActive]
     * was requested, Telecom failed to transition the call to active. There are numerous reasons
     * why the operation failed; please see the [errorCode] for details.
     */
    public class Error(@CallException.Companion.CallErrorCode public val errorCode: Int) :
        CallControlResult() {
        override fun toString(): String {
            return "CallControlResult(Error[errorCode=($errorCode)])"
        }

        override fun equals(other: Any?): Boolean {
            return other is Error && errorCode == other.errorCode
        }

        override fun hashCode(): Int {
            return Objects.hash(errorCode)
        }
    }
}
