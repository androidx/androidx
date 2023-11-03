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

package androidx.baselineprofile.gradle.utils

import org.gradle.api.Project

internal class Dependencies(private val project: Project) {

    private val defaultConfigurationsToCopy = listOf(
        "implementation",
        "api",
        "kapt",
        "annotationProcessor",
        "compile",
        "compileOnly"
    )

    fun copy(
        fromPrefix: String,
        toPrefix: String,
        configurationsToCopy: List<String> = defaultConfigurationsToCopy
    ) {
        configurationsToCopy.forEach { configurationName ->

            val fromVariantConfigurationName = camelCase(fromPrefix, configurationName)
            val fromVariantDependencies = project
                .configurations
                .findByName(fromVariantConfigurationName)
                ?.dependencies
                ?: return@forEach

            val toVariantConfigurationName = camelCase(toPrefix, configurationName)
            val toVariantDependencies = project
                .configurations
                .findByName(toVariantConfigurationName)
                ?.dependencies
                ?: return@forEach

            toVariantDependencies.addAll(fromVariantDependencies)
        }
    }
}
