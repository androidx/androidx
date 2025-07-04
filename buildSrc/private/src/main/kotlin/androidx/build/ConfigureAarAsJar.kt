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

package androidx.build

import com.android.build.api.attributes.BuildTypeAttr
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

/**
 * Creates `[configurationName]AarAsJar` config for JVM tests that need Android library classes on
 * the classpath.
 */
internal fun configureAarAsJarForConfiguration(project: Project, configurationName: String) {
    val releaseVariant =
        project.objects.named(BuildTypeAttr::class.java, Release.DEFAULT_PUBLISH_CONFIG)
    val javaApiUsage = project.objects.named(Usage::class.java, Usage.JAVA_API)
    val androidJvmEnv =
        project.objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.ANDROID)

    val aarAsJarConfig =
        project.configurations.register("${configurationName}AarAsJar") {
            it.isTransitive = false
            it.isCanBeConsumed = false
            it.isCanBeResolved = true

            it.attributes.apply {
                attribute(BuildTypeAttr.ATTRIBUTE, releaseVariant)
                attribute(Usage.USAGE_ATTRIBUTE, javaApiUsage)
                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, androidJvmEnv)
                attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "android-classes-jar")
            }
        }

    project.configurations.named(configurationName) { config ->
        config.dependencies.add(
            project.dependencies.create(aarAsJarConfig.get().incoming.artifactView {}.files)
        )
    }
}
