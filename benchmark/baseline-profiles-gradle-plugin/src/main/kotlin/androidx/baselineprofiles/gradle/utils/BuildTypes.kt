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

import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

internal inline fun <reified T : BuildType> createNonObfuscatedBuildTypes(
    project: Project,
    extension: CommonExtension<*, T, *, *>,
    crossinline filterBlock: (T) -> (Boolean),
    crossinline configureBlock: T.() -> (Unit),
    extendedBuildTypeToOriginalBuildTypeMapping: MutableMap<String, String>
) {
    extension.buildTypes.filter { buildType ->
            if (buildType !is T) {
                throw GradleException(
                    "Build type `${buildType.name}` is not of type ${T::class}"
                )
            }
            filterBlock(buildType)
        }.forEach { buildType ->

            val newBuildTypeName = camelCase(BUILD_TYPE_BASELINE_PROFILE_PREFIX, buildType.name)

            // Check in case the build type was created manually (to allow full customization)
            if (extension.buildTypes.findByName(newBuildTypeName) != null) {
                project.logger.info(
                    "Build type $newBuildTypeName won't be created because already exists."
                )
            } else {
                // If the new build type doesn't exist, create it simply extending the configured
                // one (by default release).
                extension.buildTypes.create(newBuildTypeName).apply {
                    initWith(buildType)
                    matchingFallbacks += listOf(buildType.name)
                    configureBlock(this as T)
                }
            }
            // Mapping the build type to the newly created
            extendedBuildTypeToOriginalBuildTypeMapping[newBuildTypeName] = buildType.name
        }
}

internal inline fun <reified T : BuildType> createBuildTypeIfNotExists(
    project: Project,
    extension: CommonExtension<*, T, *, *>,
    buildTypeName: String,
    crossinline configureBlock: T.() -> (Unit),
) {
    // Check in case the build type was created manually (to allow full customization)
    if (extension.buildTypes.findByName(buildTypeName) != null) {
        project.logger.info(
            "Build type $buildTypeName won't be created because already exists."
        )
        return
    }
    // If the new build type doesn't exist, create it simply extending the configured
    // one (by default release).
    extension.buildTypes.create(buildTypeName).apply {
        configureBlock(this)
    }
}
