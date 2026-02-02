/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.build.gradle.internal.fixtures

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

class FakeProviderFactory(
    private val originalFactory: ProviderFactory,
    private val gradleProperties: Map<String, Any>
) : ProviderFactory by originalFactory {

    override fun gradleProperty(propertyName: String): Provider<String> {
        if (gradleProperties.containsKey(propertyName)) {
            return originalFactory.provider { gradleProperties.getValue(propertyName).toString() }
        } else {
            return originalFactory.provider { null }
        }
    }

    companion object {
        @JvmStatic val factory: ProviderFactory by lazy { ProjectFactory.project.providers }
    }
}
