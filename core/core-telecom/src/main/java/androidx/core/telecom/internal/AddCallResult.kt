/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.internal

import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * AddCallResult should be used to represent the platform request to add a call via
 * [androidx.core.telecom.CallsManager.addCall]. Generally, the platform can either successfully add
 * the call at the time or fail with or without an exception code.
 */
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal sealed class AddCallResult {

    /**
     * [androidx.core.telecom.CallsManager.addCall] used a [CallSession] to manage the call meaning
     * the client is using sdk 34+.
     */
    class SuccessCallSession : AddCallResult() {
        override fun toString(): String {
            return "AddCallResult(SuccessCallSession)"
        }

        override fun equals(other: Any?): Boolean {
            return other is SuccessCallSession
        }

        override fun hashCode(): Int {
            return Objects.hash()
        }
    }

    /**
     * [androidx.core.telecom.CallsManager.addCall] used a [CallSessionLegacy] to manage the call
     * meaning the client is using sdk 33- (backwards compat layer aka ConnectionService).
     */
    class SuccessCallSessionLegacy(val callSessionLegacy: CallSessionLegacy) : AddCallResult() {
        override fun toString(): String {
            return "AddCallResult(SuccessCallSessionLegacy)"
        }

        override fun equals(other: Any?): Boolean {
            return other is SuccessCallSessionLegacy && callSessionLegacy == other.callSessionLegacy
        }

        override fun hashCode(): Int {
            return Objects.hash(callSessionLegacy)
        }
    }

    /**
     * The error code the platform provided as to why the new call could not be added. For Sdk 34+
     * (Android UpsideDownCase), the platform will return a non ERROR_UNKNOWN exception code. For
     * 33-, the platform will not provide an error code and ERROR_UNKNOWN will be used.
     */
    class Error(val errorCode: Int) : AddCallResult() {
        override fun toString(): String {
            return "AddCallResult(Error[errorCode=($errorCode)])"
        }

        override fun equals(other: Any?): Boolean {
            return other is Error && errorCode == other.errorCode
        }

        override fun hashCode(): Int {
            return Objects.hash(errorCode)
        }
    }
}
