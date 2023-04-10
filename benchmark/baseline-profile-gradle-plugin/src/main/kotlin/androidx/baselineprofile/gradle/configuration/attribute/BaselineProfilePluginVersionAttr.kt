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

package androidx.baselineprofile.gradle.configuration.attribute

import androidx.baselineprofile.gradle.configuration.attribute.BaselineProfilePluginVersionAttr.Companion.ATTRIBUTE
import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

/**
 * Type for the attribute holding the baseline profile plugin version.
 *
 * There should only be one build type attribute associated to each
 * [org.gradle.api.artifacts.Configuration] object. The key should be [ATTRIBUTE].
 */
internal interface BaselineProfilePluginVersionAttr : Named {
    companion object {
        @JvmField
        val ATTRIBUTE: Attribute<BaselineProfilePluginVersionAttr> =
            Attribute.of(BaselineProfilePluginVersionAttr::class.java)
    }
}
