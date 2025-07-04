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

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionRequestExceptionsTest {
    @Test
    fun testErrorCategory_RequestError() {
        assertThat(AppFunctionDeniedException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(AppFunctionDeniedException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionInvalidArgumentException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_INVALID_ARGUMENT)
        assertThat(AppFunctionInvalidArgumentException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionDisabledException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_DISABLED)
        assertThat(AppFunctionDisabledException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionFunctionNotFoundException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_FUNCTION_NOT_FOUND)
        assertThat(AppFunctionFunctionNotFoundException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionElementNotFoundException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_RESOURCE_NOT_FOUND)
        assertThat(AppFunctionElementNotFoundException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionLimitExceededException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_LIMIT_EXCEEDED)
        assertThat(AppFunctionLimitExceededException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)

        assertThat(AppFunctionElementAlreadyExistsException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_RESOURCE_ALREADY_EXISTS)
        assertThat(AppFunctionElementAlreadyExistsException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_REQUEST_ERROR)
    }
}
