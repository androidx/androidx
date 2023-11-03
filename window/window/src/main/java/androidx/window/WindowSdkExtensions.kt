/*
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
import androidx.window.core.ExtensionsUtil

/**
 * This class provides information about the extension SDK versions for window features present on
 * this device. Use [extensionVersion] to get the version of the extension. The extension version
 * advances as the platform evolves and new APIs are added, so is suitable to use for determining
 * API availability at runtime.
 *
 * Window Manager Jetpack APIs that require window SDK extensions support are denoted with
 * [RequiresWindowSdkExtension]. The caller must check whether the device's extension version is
 * greater than or equal to the minimum level reported in [RequiresWindowSdkExtension].
 *
 * @sample androidx.window.samples.checkWindowSdkExtensionsVersion
 */
abstract class WindowSdkExtensions internal constructor() {

    /**
     * Reports the device's extension version
     *
     * When Window SDK Extensions is not present on the device, the extension version will be 0.
     */
    @get: IntRange(from = 0)
    open val extensionVersion: Int = ExtensionsUtil.safeVendorApiLevel

    /**
     * Checks the [extensionVersion] and throws [UnsupportedOperationException] if the minimum
     * [version] is not satisfied.
     *
     * @param version The minimum required extension version of the targeting API.
     * @throws UnsupportedOperationException if the minimum [version] is not satisfied.
     */
    internal fun requireExtensionVersion(@IntRange(from = 1) version: Int) {
        if (extensionVersion < version) {
            throw UnsupportedOperationException("This API requires extension version " +
                "$version, but the device is on $extensionVersion")
        }
    }

    companion object {
        /** Returns a [WindowSdkExtensions] instance. */
        @JvmStatic
        fun getInstance(): WindowSdkExtensions {
            return decorator.decorate(object : WindowSdkExtensions() {})
        }

        private var decorator: WindowSdkExtensionsDecorator = EmptyDecoratorWindowSdk

        internal fun overrideDecorator(overridingDecorator: WindowSdkExtensionsDecorator) {
            decorator = overridingDecorator
        }

        internal fun reset() {
            decorator = EmptyDecoratorWindowSdk
        }
    }
}

internal interface WindowSdkExtensionsDecorator {
    /** Returns a [WindowSdkExtensions] instance. */
    fun decorate(windowSdkExtensions: WindowSdkExtensions): WindowSdkExtensions
}

private object EmptyDecoratorWindowSdk : WindowSdkExtensionsDecorator {
    override fun decorate(windowSdkExtensions: WindowSdkExtensions): WindowSdkExtensions =
        windowSdkExtensions
}
