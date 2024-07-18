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

import com.android.build.api.dsl.AndroidSourceDirectorySet
import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.BuildType
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.android.build.gradle.internal.api.DefaultAndroidSourceFile
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

internal inline fun <reified T : BuildType> createExtendedBuildTypes(
    project: Project,
    extensionBuildTypes: NamedDomainObjectContainer<out T>,
    newBuildTypePrefix: String,
    crossinline filterBlock: (T) -> (Boolean),
    crossinline configureBlock: T.() -> (Unit),
    extendedBuildTypeToOriginalBuildTypeMapping: MutableMap<String, String> = mutableMapOf()
) {
    extensionBuildTypes.filter { buildType ->
        if (buildType !is T) {
            throw GradleException(
                "Build type `${buildType.name}` is not of type ${T::class}"
            )
        }
        filterBlock(buildType)
    }.forEach { buildType ->

        val newBuildTypeName = camelCase(newBuildTypePrefix, buildType.name)

        // Check in case the build type was created manually (to allow full customization)
        if (extensionBuildTypes.findByName(newBuildTypeName) != null) {
            project.logger.info(
                "Build type $newBuildTypeName won't be created because already exists."
            )
        } else {
            // If the new build type doesn't exist, create it simply extending the configured
            // one (by default release).
            extensionBuildTypes.create(newBuildTypeName).apply {
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
    extensionBuildTypes: NamedDomainObjectContainer<out T>,
    buildTypeName: String,
    crossinline configureBlock: T.() -> (Unit),
) {
    // Check in case the build type was created manually (to allow full customization)
    if (extensionBuildTypes.findByName(buildTypeName) != null) {
        project.logger.info(
            "Build type $buildTypeName won't be created because already exists."
        )
        return
    }
    // If the new build type doesn't exist, create it simply extending the configured
    // one (by default release).
    extensionBuildTypes.create(buildTypeName).apply {
        configureBlock(this)
    }
}

internal fun copyBuildTypeSources(
    extensionSourceSets: NamedDomainObjectContainer<out AndroidSourceSet>,
    fromToMapping: Map<String, String>
) {
    extensionSourceSets
        .filter { it.name in fromToMapping.keys }
        .forEach { toSourceSets ->

            val fromBuildType = fromToMapping[toSourceSets.name]!!
            val fromSourceSets = extensionSourceSets.getByName(fromBuildType)

            // Copies each specified source set
            for (dirSet in listOf<(AndroidSourceSet) -> (AndroidSourceDirectorySet)>(
                { it.aidl },
                { it.assets },
                { it.java },
                { it.jniLibs },
                { it.kotlin },
                { it.mlModels },
                { it.renderscript },
                { it.res },
                { it.resources },
                { it.shaders },
            )) {
                val fromSet = dirSet(fromSourceSets) as DefaultAndroidSourceDirectorySet
                val toSet = dirSet(toSourceSets) as DefaultAndroidSourceDirectorySet
                toSet.srcDirs(*fromSet.srcDirs.toTypedArray())
            }

            // Copies the manifest file
            val manifestFile = (fromSourceSets.manifest as DefaultAndroidSourceFile).srcFile
            toSourceSets.manifest.srcFile(manifestFile)
        }
}
