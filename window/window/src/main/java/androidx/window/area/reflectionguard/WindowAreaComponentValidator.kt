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

package androidx.window.area.reflectionguard

import androidx.window.extensions.area.ExtensionWindowAreaPresentation
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.reflection.ReflectionUtils.validateImplementation

/**
 * Utility class to validate [WindowAreaComponent] implementation.
 */
internal object WindowAreaComponentValidator {

    internal fun isWindowAreaComponentValid(windowAreaComponent: Class<*>, apiLevel: Int): Boolean {
        return when {
            apiLevel <= 1 -> false
            apiLevel == 2 -> validateImplementation(
                windowAreaComponent, WindowAreaComponentApi2Requirements::class.java
            )
            else -> validateImplementation(
                windowAreaComponent, WindowAreaComponentApi3Requirements::class.java
            )
        }
    }

    internal fun isExtensionWindowAreaStatusValid(
        extensionWindowAreaStatus: Class<*>,
        apiLevel: Int
    ): Boolean {
        return when {
            apiLevel <= 1 -> false
            else -> validateImplementation(
                extensionWindowAreaStatus, ExtensionWindowAreaStatusRequirements::class.java
            )
        }
    }

    internal fun isExtensionWindowAreaPresentationValid(
        extensionWindowAreaPresentation: Class<*>,
        apiLevel: Int
    ): Boolean {
        return when {
            apiLevel <= 2 -> false
            else -> validateImplementation(
                extensionWindowAreaPresentation, ExtensionWindowAreaPresentation::class.java
            )
        }
    }
}
