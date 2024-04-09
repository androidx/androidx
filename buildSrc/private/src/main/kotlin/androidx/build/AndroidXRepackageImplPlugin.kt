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

package androidx.build

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create

/**
 * Plugin responsible for repackaging libraries. The plugin repackages what is set in the
 * [RelocationExtension] by the user and reconfigures the JAR task to output the repackaged classes
 * JAR.
 */
@Suppress("unused")
class AndroidXRepackageImplPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val relocationExtension =
            project.extensions.create<RelocationExtension>(EXTENSION_NAME)
        project.plugins.configureEach { plugin ->
            when (plugin) {
                is JavaLibraryPlugin -> project.configureJavaLibrary(relocationExtension)
            }
        }
    }

    private fun Project.configureJavaLibrary(relocationExtension: RelocationExtension) {
        createConfigurations()

        val javaExtension = extensions.findByType(JavaPluginExtension::class.java)
        val libraryShadowJar = tasks.register(
            "shadowLibraryJar", ShadowJar::class.java
        ) { task ->
            task.transformers.add(
                BundleInsideHelper.DontIncludeResourceTransformer().apply {
                    dropResourcesWithSuffix = ".proto"
                }
            )
            task.from(javaExtension?.sourceSets?.findByName("main")?.output)
        }

        afterEvaluate {
            val relocateFrom = relocationExtension.sourcePackage
            val relocateTo = relocationExtension.targetPackage
            val artifactIdForPublish = relocationExtension.artifactId
            libraryShadowJar.configure { it.relocate(relocateFrom, relocateTo) }

            artifactIdForPublish?.let {
                libraryShadowJar.configure {
                    it.configurations = listOf(
                        project.configurations.getByName("repackageClasspath")
                    )
                }
            }
            addArchiveToVariants(libraryShadowJar)
        }
    }

    private fun Project.createConfigurations() {
        val repackage = configurations.create("repackage") { config ->
            config.isCanBeConsumed = false
            config.isCanBeResolved = false
        }

        configurations.create("repackageClasspath") { config ->
            config.isCanBeConsumed = false
            config.isCanBeResolved = true
            config.extendsFrom(repackage)
        }

        tasks.named("jar", Jar::class.java) {
            // We cannot have two tasks with the same output as the ListTaskOutputsTask will fail.
            // As we want the repackaged jar as the published artifact, we change the
            // name of classifier of the JAR task
            it.archiveClassifier.set("before-shadow")
        }

        forceJarUsageForAndroid()
    }

    /**
     * This forces the use of repackaged JARs as opposed to the java-classes-directory
     * for Android. Without this, AGP uses the artifacts in java-classes-directory, which do not
     * have the classes repackaged to the target package.
     *
     * We attempted to extract the contents of the repackaged library JAR into classes/java/main,
     * but the AGP transform depends on JavaCompile. We cannot make JavaCompile depend on the task
     * that creates the shadowed library as that would result in a circular dependency.
     */
    private fun Project.forceJarUsageForAndroid() =
        configurations.getByName("runtimeElements").outgoing.apply {
            variants.getByName("classes").attributes {
                it.attribute(ARTIFACT_TYPE_ATTRIBUTE, "doNotUse")
            }
        }

    private fun Project.addArchiveToVariants(task: TaskProvider<ShadowJar>) =
        listOf("apiElements", "runtimeElements")
            .forEach { config ->
                configurations.getByName(config).apply {
                    outgoing.artifacts.clear()
                    outgoing.artifact(task)
                }
            }

    companion object {
        const val EXTENSION_NAME = "repackage"
    }
}

abstract class RelocationExtension : ExtensionAware {
    /* The package name and any import statements for a class that are to be relocated. */
    var sourcePackage: String? = null

    /* The package name and any import statements for a class to which they should be relocated. */
    var targetPackage: String? = null

    /* Optional artifact id if the user wants to publish the dependency in the shadowed config. */
    var artifactId: String? = null
}
