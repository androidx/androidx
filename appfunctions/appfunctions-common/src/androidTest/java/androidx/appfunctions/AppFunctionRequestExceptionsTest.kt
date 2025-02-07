/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionRequestExceptionsTest {
    @Test
    fun testErrorCategory_RequestError() {
        assertThat(AppFunctionDeniedException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(AppFunctionDeniedException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionInvalidArgumentException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_INVALID_ARGUMENT)
        assertThat(AppFunctionInvalidArgumentException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionDisabledException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_DISABLED)
        assertThat(AppFunctionDisabledException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionFunctionNotFoundException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_FUNCTION_NOT_FOUND)
        assertThat(AppFunctionFunctionNotFoundException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionElementNotFoundException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_RESOURCE_NOT_FOUND)
        assertThat(AppFunctionElementNotFoundException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionLimitExceededException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_LIMIT_EXCEEDED)
        assertThat(AppFunctionLimitExceededException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionElementAlreadyExistsException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_RESOURCE_ALREADY_EXISTS)
        assertThat(AppFunctionElementAlreadyExistsException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
    }
}
