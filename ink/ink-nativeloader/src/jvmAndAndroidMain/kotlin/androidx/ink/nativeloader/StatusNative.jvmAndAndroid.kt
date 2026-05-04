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

@UsedByNative
actual internal object StatusNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun statusCodeOk(): Int

    @UsedByNative actual external fun statusCodeFailedPrecondition(): Int

    @UsedByNative actual external fun statusCodeInvalidArgument(): Int

    @UsedByNative actual external fun statusCodeNotFound(): Int

    @UsedByNative actual external fun statusCodeOutOfRange(): Int

    @UsedByNative actual external fun statusCodeUnimplemented(): Int

    @UsedByNative actual external fun throwExceptionFromOkStatusForTesting()

    @UsedByNative
    actual external fun throwExceptionFromFailedPreconditionForTesting(message: String)

    @UsedByNative actual external fun throwExceptionFromInvalidArgumentForTesting(message: String)

    @UsedByNative actual external fun throwExceptionFromNotFoundForTesting(message: String)

    @UsedByNative actual external fun throwExceptionFromOutOfRangeForTesting(message: String)

    @UsedByNative actual external fun throwExceptionFromUnimplementedForTesting(message: String)

    @UsedByNative
    actual external fun throwExceptionFromUnknownStatusCodeForTesting(
        statusCode: Int,
        message: String,
    )
}
