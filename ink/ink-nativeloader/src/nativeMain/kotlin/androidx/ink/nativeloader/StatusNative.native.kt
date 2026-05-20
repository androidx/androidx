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

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.cinterop.StatusNative_statusCodeFailedPrecondition
import androidx.ink.nativeloader.cinterop.StatusNative_statusCodeInvalidArgument
import androidx.ink.nativeloader.cinterop.StatusNative_statusCodeNotFound
import androidx.ink.nativeloader.cinterop.StatusNative_statusCodeOk
import androidx.ink.nativeloader.cinterop.StatusNative_statusCodeOutOfRange
import androidx.ink.nativeloader.cinterop.StatusNative_statusCodeUnimplemented
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromFailedPreconditionForTesting
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromInvalidArgumentForTesting
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromNotFoundForTesting
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromOkStatusForTesting
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromOutOfRangeForTesting
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromUnimplementedForTesting
import androidx.ink.nativeloader.cinterop.StatusNative_throwExceptionFromUnknownStatusCodeForTesting
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val throwForNonOkStatusCallback:
    CPointer<CFunction<(COpaquePointer?, Int, CPointer<ByteVar>?) -> Unit>> =
    staticCFunction({ _, statusCode, statusString ->
        NativeExceptionHandling.throwForNonOkStatus(statusCode, statusString?.toKString() ?: "")
    })

@OptIn(ExperimentalForeignApi::class)
actual internal object StatusNative {
    actual fun statusCodeOk(): Int = StatusNative_statusCodeOk()

    actual fun statusCodeFailedPrecondition(): Int = StatusNative_statusCodeFailedPrecondition()

    actual fun statusCodeInvalidArgument(): Int = StatusNative_statusCodeInvalidArgument()

    actual fun statusCodeNotFound(): Int = StatusNative_statusCodeNotFound()

    actual fun statusCodeOutOfRange(): Int = StatusNative_statusCodeOutOfRange()

    actual fun statusCodeUnimplemented(): Int = StatusNative_statusCodeUnimplemented()

    actual fun throwExceptionFromOkStatusForTesting(): Unit =
        StatusNative_throwExceptionFromOkStatusForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
        )

    actual fun throwExceptionFromFailedPreconditionForTesting(message: String) =
        StatusNative_throwExceptionFromFailedPreconditionForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
            message,
        )

    actual fun throwExceptionFromInvalidArgumentForTesting(message: String) =
        StatusNative_throwExceptionFromInvalidArgumentForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
            message,
        )

    actual fun throwExceptionFromNotFoundForTesting(message: String) =
        StatusNative_throwExceptionFromNotFoundForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
            message,
        )

    actual fun throwExceptionFromOutOfRangeForTesting(message: String) =
        StatusNative_throwExceptionFromOutOfRangeForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
            message,
        )

    actual fun throwExceptionFromUnimplementedForTesting(message: String) =
        StatusNative_throwExceptionFromUnimplementedForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
            message,
        )

    actual fun throwExceptionFromUnknownStatusCodeForTesting(statusCode: Int, message: String) =
        StatusNative_throwExceptionFromUnknownStatusCodeForTesting(
            jni_env_pass_through = null,
            throwForNonOkStatusCallback,
            statusCode,
            message,
        )
}
