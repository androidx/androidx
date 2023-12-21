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

/**
 * This class defines exceptions that can be thrown when using [androidx.core.telecom] APIs.
 */
class CallException(
    @CallErrorCode val code: Int = ERROR_UNKNOWN_CODE,
    message: String? = codeToMessage(code)
) : RuntimeException(message) {

    override fun toString(): String {
        return "CallException( code=[$code], message=[$message])"
    }

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(ERROR_UNKNOWN_CODE, ERROR_CALLBACKS_CODE)
        annotation class CallErrorCode

        /**
         * The operation has failed due to an unknown or unspecified error.
         */
        const val ERROR_UNKNOWN_CODE = 1

        /**
         * This error code is thrown whenever a call is added via [CallsManager.addCall] and the
         * [CallControlScope.setCallback]s is not the first API called in the session block or at
         * all. In order to avoid this exception, ensure your [CallControlScope] is calling
         * [CallControlScope.setCallback]s.
         */
        const val ERROR_CALLBACKS_CODE = 2

        internal const val ERROR_CALLBACKS_MSG: String = "Error, when using the " +
            "[CallControlScope], you must first set the " +
            "[androidx.core.telecom.CallControlCallback]s via [CallControlScope]#[setCallback]"

        internal const val ERROR_BUILD_VERSION: String = "Core-Telecom only supports builds from" +
            " Oreo (Android 8) and above.  In order to utilize Core-Telecom, your device must" +
            " be updated."

        internal fun codeToMessage(@CallErrorCode code: Int): String {
            when (code) {
                ERROR_CALLBACKS_CODE -> return ERROR_CALLBACKS_MSG
            }
            return "An Unknown Error has occurred while using the Core-Telecom APIs"
        }
    }
}