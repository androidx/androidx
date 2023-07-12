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

import androidx.window.reflection.ReflectionUtils
import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.WindowExtensionsConstants

internal class SafeWindowExtensionsProvider(private val loader: ClassLoader) {
    internal val windowExtensionsClass: Class<*>
        get() {
            return loader.loadClass(WindowExtensionsConstants.WINDOW_EXTENSIONS_CLASS)
        }

    internal fun isWindowExtensionsValid(): Boolean {
        return isWindowExtensionsPresent() &&
            ReflectionUtils.validateReflection(
                "WindowExtensionsProvider#getWindowExtensions is not valid"
            ) {
                val providerClass = windowExtensionsProviderClass
                val getWindowExtensionsMethod = providerClass
                    .getDeclaredMethod("getWindowExtensions")
                val windowExtensionsClass = windowExtensionsClass
                getWindowExtensionsMethod.doesReturn(windowExtensionsClass) &&
                    getWindowExtensionsMethod.isPublic
            }
    }

    private fun isWindowExtensionsPresent(): Boolean {
        return ReflectionUtils.checkIsPresent {
            loader.loadClass(WindowExtensionsConstants.WINDOW_EXTENSIONS_PROVIDER_CLASS)
        }
    }
    private val windowExtensionsProviderClass: Class<*>
        get() {
            return loader.loadClass(WindowExtensionsConstants.WINDOW_EXTENSIONS_PROVIDER_CLASS)
        }
}