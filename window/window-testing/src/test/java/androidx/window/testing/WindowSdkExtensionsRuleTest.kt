/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.testing

import androidx.window.WindowSdkExtensions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Test class to verify [WindowSdkExtensionsRule] behaviors. */
class WindowSdkExtensionsRuleTest {

    @JvmField @Rule val rule = WindowSdkExtensionsRule()

    /** Verifies the [WindowSdkExtensionsRule] behavior. */
    @Test
    fun testWindowSdkExtensionsRule() {
        assertEquals(
            "The WindowSdkExtensions.extensionVersion is 0 in unit test",
            0,
            WindowSdkExtensions.getInstance().extensionVersion
        )

        rule.overrideExtensionVersion(3)

        assertEquals(3, WindowSdkExtensions.getInstance().extensionVersion)
    }
}
