/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.baselineprofiles.gradle.utils

import org.gradle.api.attributes.Attribute
import org.gradle.configurationcache.extensions.capitalized

internal fun camelCase(vararg strings: String): String {
    if (strings.isEmpty()) return ""
    return StringBuilder()
        .apply {
            var shouldCapitalize = false
            for (str in strings.filter { it.isNotBlank() }) {
                append(if (shouldCapitalize) str.capitalized() else str)
                shouldCapitalize = true
            }
        }.toString()
}

// Prefix for the build type baseline profiles
internal const val BUILD_TYPE_BASELINE_PROFILE_PREFIX = "nonObfuscated"

// Configuration consumed by this plugin that carries the baseline profile HRF file.
internal const val CONFIGURATION_NAME_BASELINE_PROFILES = "baselineprofiles"

// Custom category attribute to match the baseline profile configuration
internal const val ATTRIBUTE_CATEGORY_BASELINE_PROFILE = "baselineprofile"

internal val ATTRIBUTE_FLAVOR =
    Attribute.of("androidx.baselineprofiles.gradle.attributes.Flavor", String::class.java)
internal val ATTRIBUTE_BUILD_TYPE =
    Attribute.of("androidx.baselineprofiles.gradle.attributes.BuildType", String::class.java)