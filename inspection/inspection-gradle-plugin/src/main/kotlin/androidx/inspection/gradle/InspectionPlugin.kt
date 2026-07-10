/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies

/**
 * A plugin which, when present, ensures that intermediate inspector resources are generated at
 * build time
 */
class InspectionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        var foundLibraryPlugin = false
        var foundReleaseVariant = false
        val extension = project.extensions.create<InspectionExtension>(EXTENSION_NAME, project)

        val publishInspector =
            project.configurations.create("publishInspector") {
                it.isCanBeConsumed = true
                it.isCanBeResolved = false
                it.setupInspectorAttribute()
            }

        project.configurations.create(EXPORT_INSPECTOR_DEPENDENCIES) {
            // to allow including these dependencies in an SBOM
            it.description = "Re-publishes dependencies of the inspector"
            it.isCanBeConsumed = true
            it.isCanBeResolved = true
            it.extendsFrom(project.configurations.getByName("implementation"))
            it.setupReleaseAttribute()
        }

        project.pluginManager.withPlugin("com.android.library") {
            foundLibraryPlugin = true
            val libExtension = project.extensions.getByType(LibraryExtension::class.java)
            val componentsExtension =
                project.extensions.findByType(LibraryAndroidComponentsExtension::class.java)
                    ?: throw GradleException("android plugin must be used")
            componentsExtension.onVariants { variant: Variant ->
                if (variant.name == "release") {
                    foundReleaseVariant = true
                    val unzip = project.registerUnzipTask(variant)
                    val shadowJar =
                        project.registerShadowDependenciesTask(variant, extension, unzip)
                    val bundleTask =
                        project.registerBundleInspectorTask(
                            variant,
                            libExtension,
                            componentsExtension,
                            extension.name,
                            shadowJar,
                        )
                    publishInspector.outgoing.variants {
                        val configVariant = it.create("inspectorJar")
                        configVariant.artifact(bundleTask)
                    }
                }
            }
        }

        project.apply(plugin = "com.google.protobuf")

        project.dependencies { add("implementation", project.getLibraryByName("protobufLite")) }

        project.afterEvaluate {
            if (!foundLibraryPlugin) {
                throw StopExecutionException(
                    """
                    A required plugin, com.android.library, was not found.
                                            The androidx.inspection plugin currently only supports android library
                                            modules, so ensure that com.android.library is applied in the project
                                            build.gradle file.
                    """
                        .trimIndent()
                )
            }
            if (!foundReleaseVariant) {
                throw StopExecutionException(
                    "The androidx.inspection plugin requires " + "release build variant."
                )
            }
        }
    }
}

private fun Project.getLibraryByName(name: String): MinimalExternalModuleDependency {
    val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).find("libs").get()
    val library = libs.findLibrary(name)
    return if (library.isPresent) {
        library.get().get()
    } else {
        throw GradleException("Could not find a library for `$name`")
    }
}

/**
 * Use this function in [libraryProject] to include inspector that will be compiled into
 * inspector.jar and packaged in the library's aar.
 *
 * @param libraryProject project that is inspected and which aar will host inspector.jar . E.g.
 *   work-runtime
 * @param inspectorProject project of the inspector that will be compiled into the inspector.jar.
 *   E.g. :work:work-inspection
 */
fun Variant.packageInspector(libraryProject: Project, inspectorProject: Project) {
    val consumeInspector = libraryProject.createConsumeInspectionConfiguration()

    libraryProject.dependencies.add(consumeInspector.name, inspectorProject)
    val consumeInspectorFiles = consumeInspector.incoming.files

    libraryProject.registerGenerateProguardDetectionFileTask(this)
    val repackageWithInspectorJarTaskProvider =
        libraryProject.tasks.register(
            this.taskName("repackageAarWithInspectorJarFor"),
            AddInspectorJarToAarTask::class.java,
        ) { task ->
            task.inspectorJar.from(consumeInspectorFiles)
        }
    artifacts
        .use(repackageWithInspectorJarTaskProvider)
        .wiredWithFiles(AddInspectorJarToAarTask::inputAar, AddInspectorJarToAarTask::outputAar)
        .toTransform(SingleArtifact.AAR)

    libraryProject.configurations.create(IMPORT_INSPECTOR_DEPENDENCIES) {
        it.setupReleaseAttribute()
    }
    libraryProject.dependencies.add(
        IMPORT_INSPECTOR_DEPENDENCIES,
        libraryProject.dependencies.project(
            mapOf("path" to inspectorProject.path, "configuration" to EXPORT_INSPECTOR_DEPENDENCIES)
        ),
    )

    // When adding package inspector to a new project, add the artifactId here
    // to ensure inspector.jar is packaged in the correct location
    val artifactId =
        when (libraryProject.name) {
            "ui" -> "ui-android"
            "work-runtime" -> "work-runtime"
            else ->
                throw GradleException(
                    "Project ${libraryProject.name} does not have artifactId defined " +
                        "for packaging the inspector.jar file"
                )
        }
    libraryProject.createVerifyInspectorJarPresentTask(artifactId)
}

fun Project.createConsumeInspectionConfiguration(): Configuration =
    configurations.create("consumeInspector") {
        it.setupInspectorAttribute()
        it.isCanBeConsumed = false
    }

private fun Configuration.setupInspectorAttribute() {
    attributes { it.attribute(Attribute.of("inspector", String::class.java), "inspectorJar") }
}

private fun Configuration.setupReleaseAttribute() {
    attributes {
        it.attribute(
            Attribute.of("com.android.build.api.attributes.BuildTypeAttr", String::class.java),
            "release",
        )
        it.attribute(
            Attribute.of("artifactType", String::class.java),
            ArtifactTypeDefinition.JAR_TYPE,
        )
    }
}

const val EXTENSION_NAME = "inspection"
const val EXPORT_INSPECTOR_DEPENDENCIES = "exportInspectorImplementation"
const val IMPORT_INSPECTOR_DEPENDENCIES = "importInspectorImplementation"

open class InspectionExtension(@Suppress("UNUSED_PARAMETER") project: Project) {
    /** Name of built inspector artifact, if not provided it is equal to project's name. */
    var name: String? = null

    /**
     * Modules to exclude, e.g.
     * - "org.jetbrains.kotlin:*"
     * - "org.jetbrains:annotations"
     */
    val excludedModules: SetProperty<String> =
        project.objects
            .setProperty(String::class.java)
            .convention(
                setOf(
                    "org.jetbrains.kotlin:kotlin-stdlib*",
                    "org.jetbrains:annotations",
                    "org.intellij.lang:annotations",
                )
            )

    /**
     * Modules to force-keep, even if excludedModules matches them, e.g.
     * - "org.jetbrains.kotlin:kotlin-reflect"
     */
    val allowedModules: SetProperty<String> =
        project.objects.setProperty(String::class.java).convention(emptySet())
}
