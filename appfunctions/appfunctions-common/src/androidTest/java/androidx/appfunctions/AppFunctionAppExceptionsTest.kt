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

class AppFunctionAppExceptionsTest {
    @Test
    fun testErrorCategory_AppError() {
        assertThat(AppFunctionAppUnknownException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_APP_UNKNOWN_ERROR)
        assertThat(AppFunctionAppUnknownException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)

        assertThat(AppFunctionPermissionRequiredException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_PERMISSION_REQUIRED)
        assertThat(AppFunctionPermissionRequiredException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)

        assertThat(AppFunctionNotSupportedException().internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_NOT_SUPPORTED)
        assertThat(AppFunctionNotSupportedException().errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)
    }
}
