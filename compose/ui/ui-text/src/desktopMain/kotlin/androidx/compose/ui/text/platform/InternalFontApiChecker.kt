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

package androidx.compose.ui.text.platform

import kotlinx.atomicfu.atomic

internal object InternalFontApiChecker {

    private var hasCheckedAccess by atomic(false)
    private var isSunFontAccessible by atomic(false)

    /**
     * Check whether the `sun.font` API is accessible. The result is cached
     * after the initial lookup; this API can safely be called multiple times.
     */
    fun isSunFontApiAccessible(): Boolean {
        if (hasCheckedAccess) return isSunFontAccessible

        val canAccess = canAccessSunFontApi()
        isSunFontAccessible = canAccess
        hasCheckedAccess = true
        return canAccess
    }

    private fun canAccessSunFontApi(): Boolean {
        try {
            val unnamedModule = ClassLoader.getSystemClassLoader().unnamedModule
            val desktopModule = ModuleLayer.boot().modules().single { it.name == "java.desktop" }

            // Check the necessary open directives are available, so we can access standard sun.font APIs
            if (!unnamedModule.canRead(desktopModule)) return false
            if (!desktopModule.isOpen("sun.font", unnamedModule)) return false

            // Try to obtain an instance of sun.font.FontManager (will fail if the open directive is missing)
            val fontManagerClass = Class.forName("sun.font.FontManagerFactory")
            fontManagerClass.getDeclaredMethod("getInstance").invoke(null)

            return true
        } catch (ignored: Throwable) {
            return false
        }
    }

    /**
     * Check whether the application is currently running on the JetBrains
     * Runtime.
     */
    fun isRunningOnJetBrainsRuntime() =
        System.getProperty("java.vendor")
            .equals("JetBrains s.r.o.", ignoreCase = true)
}
