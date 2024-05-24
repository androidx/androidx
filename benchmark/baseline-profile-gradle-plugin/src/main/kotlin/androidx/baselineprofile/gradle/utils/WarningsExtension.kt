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

package androidx.baselineprofile.gradle.utils

import org.gradle.api.plugins.ExtensionContainer

/** Extensions to enable/disable each warnings separately. */
abstract class WarningsExtension : Warnings() {

    companion object {
        private const val EXTENSION_NAME = "warnings"

        internal fun register(extensions: ExtensionContainer): Warnings {
            return extensions.findByType(Warnings::class.java)
                ?: extensions.create(EXTENSION_NAME, Warnings::class.java)
        }
    }
}

open class Warnings {

    /** Controls all the warnings printed by the baseline profile gradle plugin. */
    fun setAll(value: Boolean) {
        this.maxAgpVersion = value
        this.disabledVariants = value
        this.multipleBuildTypesWithAgp80 = value
        this.noBaselineProfileRulesGenerated = value
        this.noStartupProfileRulesGenerated = value
    }

    /**
     * Controls the warning for when the Android Gradle Plugin version is higher than the max tested
     * one.
     */
    var maxAgpVersion = true

    /** Controls the warning for when a benchmark or baseline profile variant has been disabled. */
    var disabledVariants = true

    /**
     * Controls the warning printed when invoking `generateBaselineProfile` with AGP 8.0, that does
     * not support running instrumentation tests for multiple build types at once.
     */
    var multipleBuildTypesWithAgp80 = true

    /**
     * Controls the warning printed when no baseline profile are generated after running the
     * generate baseline profile command.
     */
    var noBaselineProfileRulesGenerated = true

    /**
     * Controls the warning printed when no startup profile are generated after running the generate
     * baseline profile command.
     */
    var noStartupProfileRulesGenerated = true

    /**
     * Controls the warning printed when a variant has no baseline profile dependency set, either
     * globally or a specific one.
     */
    var variantHasNoBaselineProfileDependency = true
}
