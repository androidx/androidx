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

package androidx.ink.nativeloader

import kotlin.jvm.JvmStatic

/** Callback object for raising an exception from a non-OK absl::Status in native code. */
@UsedByNative
internal object NativeExceptionHandling {
    @UsedByNative
    @JvmStatic
    fun throwForNonOkStatus(statusCode: Int, statusMessage: String) {
        when (statusCode) {
            StatusCode.OK ->
                throw IllegalStateException(
                    "Attempting to throw an exception from status, but status is OK."
                )
            StatusCode.FAILED_PRECONDITION -> throw IllegalStateException(statusMessage)
            StatusCode.INVALID_ARGUMENT -> throw IllegalArgumentException(statusMessage)
            StatusCode.NOT_FOUND -> throw NoSuchElementException(statusMessage)
            StatusCode.OUT_OF_RANGE -> throw IndexOutOfBoundsException(statusMessage)
            StatusCode.UNIMPLEMENTED -> throw UnsupportedOperationException(statusMessage)
            else -> throw RuntimeException(statusMessage)
        }
    }
}

private object StatusCode {
    val OK = StatusNative.statusCodeOk()
    val FAILED_PRECONDITION = StatusNative.statusCodeFailedPrecondition()
    val INVALID_ARGUMENT = StatusNative.statusCodeInvalidArgument()
    val NOT_FOUND = StatusNative.statusCodeNotFound()
    val OUT_OF_RANGE = StatusNative.statusCodeOutOfRange()
    val UNIMPLEMENTED = StatusNative.statusCodeUnimplemented()
}

expect internal object StatusNative {
    fun statusCodeOk(): Int

    fun statusCodeFailedPrecondition(): Int

    fun statusCodeInvalidArgument(): Int

    fun statusCodeNotFound(): Int

    fun statusCodeOutOfRange(): Int

    fun statusCodeUnimplemented(): Int

    fun throwExceptionFromOkStatusForTesting()

    fun throwExceptionFromFailedPreconditionForTesting(message: String)

    fun throwExceptionFromInvalidArgumentForTesting(message: String)

    fun throwExceptionFromNotFoundForTesting(message: String)

    fun throwExceptionFromOutOfRangeForTesting(message: String)

    fun throwExceptionFromUnimplementedForTesting(message: String)

    fun throwExceptionFromUnknownStatusCodeForTesting(statusCode: Int, message: String)
}
