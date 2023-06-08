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
package androidx.window.area

import androidx.window.SafeWindowExtensionsProvider
import androidx.window.area.reflectionguard.WindowAreaComponentValidator.isExtensionWindowAreaPresentationValid
import androidx.window.area.reflectionguard.WindowAreaComponentValidator.isExtensionWindowAreaStatusValid
import androidx.window.area.reflectionguard.WindowAreaComponentValidator.isWindowAreaComponentValid
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.ReflectionUtils.validateReflection
import androidx.window.reflection.WindowExtensionsConstants

/**
 * Reflection Guard for [WindowAreaComponent].
 * This will go through the [WindowAreaComponent]'s method by reflection and
 * check each method's name and signature to see if the interface is what we required.
 */
internal class SafeWindowAreaComponentProvider(private val loader: ClassLoader) {

    private val windowExtensions = SafeWindowExtensionsProvider(loader).windowExtensions

    val windowAreaComponent: WindowAreaComponent?
        get() {
            return try {
                if (
                    windowExtensions != null &&
                    isWindowAreaProviderValid(windowExtensions) &&
                    isWindowAreaComponentValid(
                        windowAreaComponentClass, ExtensionsUtil.safeVendorApiLevel
                    ) &&
                    isExtensionWindowAreaStatusValid(
                        extensionWindowAreaStatusClass, ExtensionsUtil.safeVendorApiLevel
                    ) &&
                    isValidExtensionWindowPresentation()
                ) windowExtensions.windowAreaComponent else null
            } catch (e: Exception) {
                null
            }
        }

    private fun isWindowAreaProviderValid(windowExtensions: Any): Boolean {
        return validateReflection(
            "WindowExtensions#getWindowAreaComponent is not valid"
        ) {
            val getWindowAreaComponentMethod =
                windowExtensions::class.java.getMethod("getWindowAreaComponent")
            getWindowAreaComponentMethod.isPublic &&
                getWindowAreaComponentMethod.doesReturn(windowAreaComponentClass)
        }
    }

    private fun isValidExtensionWindowPresentation(): Boolean {
        // Not required for API Level 2 or below
        return ExtensionsUtil.safeVendorApiLevel <= 2 ||
            isExtensionWindowAreaPresentationValid(
                extensionWindowAreaPresentationClass, ExtensionsUtil.safeVendorApiLevel
            )
    }

    private val windowAreaComponentClass: Class<*>
        get() {
            return loader.loadClass(WindowExtensionsConstants.WINDOW_AREA_COMPONENT_CLASS)
        }

    private val extensionWindowAreaStatusClass: Class<*>
        get() {
            return loader.loadClass(WindowExtensionsConstants.EXTENSION_WINDOW_AREA_STATUS_CLASS)
        }

    private val extensionWindowAreaPresentationClass: Class<*>
        get() {
            return loader.loadClass(
                WindowExtensionsConstants.EXTENSION_WINDOW_AREA_PRESENTATION_CLASS
            )
        }
}
