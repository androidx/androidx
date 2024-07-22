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

import androidx.annotation.IntRange
import androidx.window.WindowSdkExtensions
import androidx.window.WindowSdkExtensionsDecorator
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * [TestRule] for overriding [WindowSdkExtensions] properties in unit tests.
 *
 * The [TestRule] is designed to only be used in unit tests. Users should use the actual
 * [WindowSdkExtensions] properties for instrumentation tests. Overriding the device's extensions
 * version to a higher version may lead to unexpected test failures or even app crash.
 */
class WindowSdkExtensionsRule : TestRule {

    private val fakeWindowSdkExtensions = FakeWindowSdkExtensions()

    override fun apply(
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        base: Statement,
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        description: Description
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                WindowSdkExtensions.overrideDecorator(
                    object : WindowSdkExtensionsDecorator {
                        override fun decorate(
                            windowSdkExtensions: WindowSdkExtensions
                        ): WindowSdkExtensions = fakeWindowSdkExtensions
                    }
                )
                try {
                    base.evaluate()
                } finally {
                    WindowSdkExtensions.reset()
                }
            }
        }
    }

    /**
     * Overrides the [WindowSdkExtensions.extensionVersion] for testing.
     *
     * @param version The extension version to override.
     */
    fun overrideExtensionVersion(@IntRange(from = 0) version: Int) {
        fakeWindowSdkExtensions.overrideExtensionVersion(version)
    }
}
