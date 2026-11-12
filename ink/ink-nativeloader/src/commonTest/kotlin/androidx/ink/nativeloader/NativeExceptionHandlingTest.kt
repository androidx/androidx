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

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class NativeExceptionHandlingTest {

    @Test
    fun throwForNonOkStatus_throwsIllegalStateExceptionForOkStatus() {
        // Here the exception is because this callback should not be called with an OK status.
        val ex =
            assertFailsWith<IllegalStateException> {
                StatusNative.throwExceptionFromOkStatusForTesting()
            }
        assertThat(ex)
            .hasMessageThat()
            .isEqualTo("Attempting to throw an exception from status, but status is OK.")
    }

    @Test
    fun throwForNonOkStatus_throwsIllegalStateExceptionForFailedPrecondition() {
        val ex =
            assertFailsWith<IllegalStateException> {
                StatusNative.throwExceptionFromFailedPreconditionForTesting("Failed precondition")
            }
        assertThat(ex).hasMessageThat().startsWith("FAILED_PRECONDITION: Failed precondition")
        assertThat(ex).hasMessageThat().contains("=== Source Location Trace: ===")
        assertThat(ex).hasMessageThat().contains("status_native.cc")
    }

    @Test
    fun throwForNonOkStatus_throwsIllegalArgumentExceptionForInvalidArgument() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                StatusNative.throwExceptionFromInvalidArgumentForTesting("Invalid argument")
            }
        assertThat(ex).hasMessageThat().startsWith("INVALID_ARGUMENT: Invalid argument")
        assertThat(ex).hasMessageThat().contains("=== Source Location Trace: ===")
        assertThat(ex).hasMessageThat().contains("status_native.cc")
    }

    @Test
    fun throwForNonOkStatus_throwsNoSuchElementExceptionForNotFound() {
        val ex =
            assertFailsWith<NoSuchElementException> {
                StatusNative.throwExceptionFromNotFoundForTesting("Not found")
            }
        assertThat(ex).hasMessageThat().startsWith("NOT_FOUND: Not found")
        assertThat(ex).hasMessageThat().contains("=== Source Location Trace: ===")
        assertThat(ex).hasMessageThat().contains("status_native.cc")
    }

    @Test
    fun throwForNonOkStatus_throwsIndexOutOfBoundsExceptionForOutOfRange() {
        val ex =
            assertFailsWith<IndexOutOfBoundsException> {
                StatusNative.throwExceptionFromOutOfRangeForTesting("Out of range")
            }
        assertThat(ex).hasMessageThat().startsWith("OUT_OF_RANGE: Out of range")
        assertThat(ex).hasMessageThat().contains("=== Source Location Trace: ===")
        assertThat(ex).hasMessageThat().contains("status_native.cc")
    }

    @Test
    fun throwForNonOkStatus_throwsUnsupportedOperationExceptionForUnimplemented() {
        val ex =
            assertFailsWith<UnsupportedOperationException> {
                StatusNative.throwExceptionFromUnimplementedForTesting("Unimplemented")
            }
        assertThat(ex).hasMessageThat().startsWith("UNIMPLEMENTED: Unimplemented")
        assertThat(ex).hasMessageThat().contains("=== Source Location Trace: ===")
        assertThat(ex).hasMessageThat().contains("status_native.cc")
    }

    @Test
    fun throwForNonOkStatus_throwsRuntimeExceptionForUnknownStatusCode() {
        val ex =
            assertFailsWith<RuntimeException> {
                // This is DEADLINE_EXCEEDED, which we don't have handling for, so it just uses
                // RuntimeException.
                StatusNative.throwExceptionFromUnknownStatusCodeForTesting(4, "Unknown status code")
            }
        assertThat(ex).hasMessageThat().startsWith("DEADLINE_EXCEEDED: Unknown status code")
        assertThat(ex).hasMessageThat().contains("=== Source Location Trace: ===")
        assertThat(ex).hasMessageThat().contains("status_native.cc")
    }
}
