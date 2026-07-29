/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionManagerApiTest {

    @Test
    fun testApplyMissingRuntimeMetadataExceptionFix_withMatchingRuntimeException_returnsIllegalArgumentException() {
        val functionId = "com.example.pkg/func"
        val error =
            RuntimeException(
                "Some prefix. Expected 1 GenericDocument for runtimeMetadata, found 0. Some suffix."
            )

        val result =
            AppFunctionManagerApi.applyMissingRuntimeMetadataExceptionFix(functionId, error)

        assertThat(result).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.message).isEqualTo("Runtime metadata for $functionId is not yet created.")
    }

    @Test
    fun testApplyMissingRuntimeMetadataExceptionFix_withNonMatchingRuntimeException_returnsOriginalException() {
        val functionId = "com.example.pkg/func"
        val error = RuntimeException("Some other error")

        val result =
            AppFunctionManagerApi.applyMissingRuntimeMetadataExceptionFix(functionId, error)

        assertThat(result).isSameInstanceAs(error)
    }

    @Test
    fun testApplyMissingRuntimeMetadataExceptionFix_withIllegalArgumentException_returnsOriginalException() {
        val functionId = "com.example.pkg/func"
        val error =
            IllegalArgumentException("Expected 1 GenericDocument for runtimeMetadata, found 0")

        val result =
            AppFunctionManagerApi.applyMissingRuntimeMetadataExceptionFix(functionId, error)

        assertThat(result).isSameInstanceAs(error)
    }

    @Test
    fun testApplyMissingRuntimeMetadataExceptionFix_withNonRuntimeException_returnsOriginalException() {
        val functionId = "com.example.pkg/func"
        val error = Exception("Expected 1 GenericDocument for runtimeMetadata, found 0")

        val result =
            AppFunctionManagerApi.applyMissingRuntimeMetadataExceptionFix(functionId, error)

        assertThat(result).isSameInstanceAs(error)
    }
}
