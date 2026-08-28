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

package androidx.test.backup.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BackupRestoreExtensionTest {

    @Suppress("UNUSED_PARAMETER")
    private fun dummyMethod(device: BackupRestoreController, other: String) {}

    @Test
    fun testSupportsParameter() {
        val extension = BackupRestoreExtension()
        val mockParameterContext = mock(ParameterContext::class.java)
        val mockExtensionContext = mock(ExtensionContext::class.java)

        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "dummyMethod",
                    BackupRestoreController::class.java,
                    String::class.java,
                )
        val parameters = method.parameters
        val deviceParameter = parameters[0]
        val otherParameter = parameters[1]

        // Test when parameter is BackupRestoreController
        `when`(mockParameterContext.parameter).thenReturn(deviceParameter)
        assertTrue(extension.supportsParameter(mockParameterContext, mockExtensionContext))

        // Test when parameter is something else
        `when`(mockParameterContext.parameter).thenReturn(otherParameter)
        assertFalse(extension.supportsParameter(mockParameterContext, mockExtensionContext))
    }
}
