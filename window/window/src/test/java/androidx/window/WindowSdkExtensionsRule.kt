/**
*
* Copyright 2023 The Android Open Source Project
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

package androidx.window

import androidx.annotation.IntRange
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test rule for overriding [WindowSdkExtensions] properties.
 *
 * This should mainly be used for validating the behavior with a simplified version of
 * [WindowSdkExtensions] in unit tests.
 * For on-device Android tests, it's highly suggested to respect
 * the device's [WindowSdkExtensions.extensionVersion]. Overriding the real device's is error-prone,
 * and may lead to unexpected behavior.
 */
class WindowSdkExtensionsRule : TestRule {

    private val mStubWindowSdkExtensions = StubWindowSdkExtensions()

    override fun apply(
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        base: Statement,
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        description: Description
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                WindowSdkExtensions.overrideDecorator(object : WindowSdkExtensionsDecorator {
                    override fun decorate(windowSdkExtensions: WindowSdkExtensions):
                        WindowSdkExtensions = mStubWindowSdkExtensions
                })
                try {
                    base.evaluate()
                } finally {
                    WindowSdkExtensions.reset()
                }
            }
        }
    }

    /**
     * Overrides [WindowSdkExtensions.extensionVersion] for testing.
     *
     * @param version The extension version to override
     */
    fun overrideExtensionVersion(@IntRange(from = 0) version: Int) {
        mStubWindowSdkExtensions.overrideExtensionVersion(version)
    }
}
