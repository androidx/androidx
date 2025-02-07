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
import org.junit.AssumptionViolatedException
import org.junit.Test

class AppFunctionExceptionTest {
    @Test
    fun testTransformToPlatformExtensionsClass() {
        assumeAppFunctionExtensionLibraryAvailable()
        val extras = Bundle().apply { putString("testKey", "testValue") }
        val exception = AppFunctionDeniedException("testMessage", extras)

        val platformException = exception.toPlatformExtensionsClass()

        assertThat(platformException.errorCode).isEqualTo(AppFunctionException.ERROR_DENIED)
        assertThat(platformException.errorMessage).isEqualTo("testMessage")
        assertThat(platformException.extras.getString("testKey")).isEqualTo("testValue")
    }

    @Test
    fun testCreateFromPlatformExtensionsClass_knownClasses() {
        assumeAppFunctionExtensionLibraryAvailable()

        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_APP_UNKNOWN_ERROR,
            AppFunctionAppUnknownException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_PERMISSION_REQUIRED,
            AppFunctionPermissionRequiredException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_NOT_SUPPORTED,
            AppFunctionNotSupportedException::class.java
        )

        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_DENIED,
            AppFunctionDeniedException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_INVALID_ARGUMENT,
            AppFunctionInvalidArgumentException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_DISABLED,
            AppFunctionDisabledException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_FUNCTION_NOT_FOUND,
            AppFunctionFunctionNotFoundException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_LIMIT_EXCEEDED,
            AppFunctionLimitExceededException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_RESOURCE_ALREADY_EXISTS,
            AppFunctionElementAlreadyExistsException::class.java
        )

        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_SYSTEM_ERROR,
            AppFunctionSystemUnknownException::class.java
        )
        testCreateFromPlatformExtensionsClass(
            AppFunctionException.ERROR_CANCELLED,
            AppFunctionCancelledException::class.java
        )
    }

    @Test
    fun testCreateFromPlatformExtensionsClass_unknownErrorCode() {
        assumeAppFunctionExtensionLibraryAvailable()

        testCreateFromPlatformExtensionsClass(123456, AppFunctionUnknownException::class.java)
    }

    private fun <E : AppFunctionException> testCreateFromPlatformExtensionsClass(
        errorCode: Int,
        exceptionClass: Class<E>
    ) {
        assumeAppFunctionExtensionLibraryAvailable()
        val extras = Bundle().apply { putString("testKey", "testValue") }
        val platformException =
            com.android.extensions.appfunctions.AppFunctionException(
                errorCode,
                "testMessage",
                extras
            )

        val exception = AppFunctionException.fromPlatformExtensionsClass(platformException)

        assertThat(exception).isInstanceOf(exceptionClass)
        assertThat(exception.internalErrorCode).isEqualTo(errorCode)
        assertThat(exception.errorMessage).isEqualTo("testMessage")
        assertThat(exception.extras.getString("testKey")).isEqualTo("testValue")
    }

    private fun assumeAppFunctionExtensionLibraryAvailable(): Boolean {
        try {
            Class.forName("com.android.extensions.appfunctions.AppFunctionManager")
            return true
        } catch (e: ClassNotFoundException) {
            throw AssumptionViolatedException("Unable to find AppFunction extension library", e)
        }
    }
}
