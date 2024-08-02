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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** This class defines exceptions that can be thrown when using [androidx.core.telecom] APIs. */
public class CallException(@CallErrorCode public val code: Int = ERROR_UNKNOWN) :
    RuntimeException() {

    override fun toString(): String {
        return "CallException(code=[$code])"
    }

    override fun equals(other: Any?): Boolean {
        return other is CallException && code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            ERROR_UNKNOWN,
            ERROR_CANNOT_HOLD_CURRENT_ACTIVE_CALL,
            ERROR_CALL_IS_NOT_BEING_TRACKED,
            ERROR_CALL_CANNOT_BE_SET_TO_ACTIVE,
            ERROR_CALL_NOT_PERMITTED_AT_PRESENT_TIME,
            ERROR_OPERATION_TIMED_OUT,
            ERROR_CALL_DOES_NOT_SUPPORT_HOLD,
            ERROR_BLUETOOTH_DEVICE_IS_NULL
        )
        public annotation class CallErrorCode

        /** The operation has failed due to an unknown or unspecified error. */
        public const val ERROR_UNKNOWN: Int = 1

        /**
         * The operation has failed due to Telecom failing to hold the current active call for the
         * call attempting to become the new active call. The client should end the current active
         * call and re-try the failed operation.
         */
        public const val ERROR_CANNOT_HOLD_CURRENT_ACTIVE_CALL: Int = 2

        /**
         * The operation has failed because Telecom has already removed the call from the server
         * side and destroyed all the objects associated with it. The client should re-add the call.
         */
        public const val ERROR_CALL_IS_NOT_BEING_TRACKED: Int = 3

        /**
         * The operation has failed because Telecom cannot set the requested call as the current
         * active call. The client should end the current active call and re-try the operation.
         */
        public const val ERROR_CALL_CANNOT_BE_SET_TO_ACTIVE: Int = 4

        /**
         * The operation has failed because there is either no PhoneAccount registered with Telecom
         * for the given operation, or the limit of calls has been reached. The client should end
         * the current active call and re-try the failed operation.
         */
        public const val ERROR_CALL_NOT_PERMITTED_AT_PRESENT_TIME: Int = 5

        /** The operation has failed because the operation failed to complete before the timeout */
        public const val ERROR_OPERATION_TIMED_OUT: Int = 6

        /**
         * The [CallControlScope.setInactive] or [CallsManager.addCall#onSetInactive] failed because
         * the [CallAttributesCompat.SUPPORTS_SET_INACTIVE] was not set. Please re-add the call with
         * the [CallAttributesCompat.SUPPORTS_SET_INACTIVE] if the call should be able to hold.
         */
        public const val ERROR_CALL_DOES_NOT_SUPPORT_HOLD: Int = 7

        /**
         * Telecom was not able to switch the audio route to Bluetooth because the Bluetooth device
         * is null. The user should reconnect the Bluetooth device and retry the audio route switch.
         */
        public const val ERROR_BLUETOOTH_DEVICE_IS_NULL: Int = 8

        internal fun fromTelecomCode(code: Int): Int {
            when (code) {
                1 -> return ERROR_UNKNOWN
                2 -> return ERROR_CANNOT_HOLD_CURRENT_ACTIVE_CALL
                3 -> return ERROR_CALL_IS_NOT_BEING_TRACKED
                4 -> return ERROR_CALL_CANNOT_BE_SET_TO_ACTIVE
                5 -> return ERROR_CALL_NOT_PERMITTED_AT_PRESENT_TIME
                6 -> return ERROR_OPERATION_TIMED_OUT
            }
            return ERROR_UNKNOWN
        }
    }
}
