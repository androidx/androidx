/*
 * Copyright 2020 The Android Open Source Project
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
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named

/** Allow java and Android libraries to bundle other projects inside the project jar/aar. */
object BundleInsideHelper {
    val CONFIGURATION_NAME = "bundleInside"
    val REPACKAGE_TASK_NAME = "repackageBundledJars"

    /**
     * Creates a configuration for the users to use that will be used to bundle these dependency
     * jars inside of libs/ directory inside of the aar.
     *
     * ```
     * dependencies {
     *   bundleInside(project(":foo"))
     * }
     * ```
     *
     * Used project are expected
     *
     * @param relocations a list of package relocations to apply
     * @param dropResourcesWithSuffix used to drop Java resources if they match this suffix, null
     *   means no filtering
     * @receiver the project that should bundle jars specified by this configuration
     * @see forInsideAar(String, String)
     */
    @JvmStatic
    fun Project.forInsideAar(relocations: List<Relocation>?, dropResourcesWithSuffix: String?) {
        val bundle = createBundleConfiguration()
        val repackage = configureRepackageTaskForType(relocations, bundle, dropResourcesWithSuffix)
        // Add to AGP's configuration so this jar get packaged inside of the aar.
        dependencies.add("implementation", files(repackage.flatMap { it.archiveFile }))
    }
    /**
     * Creates 3 configurations for the users to use that will be used bundle these dependency jars
     * inside of libs/ directory inside of the aar.
     *
     * ```
     * dependencies {
     *   bundleInside(project(":foo"))
     * }
     * ```
     *
     * Used project are expected
     *
     * @param from specifies from which package the rename should happen
     * @param to specifies to which package to put the renamed classes
     * @param dropResourcesWithSuffix used to drop Java resources if they match this suffix, null
     *   means no filtering
     * @receiver the project that should bundle jars specified by these configurations
     */
    @JvmStatic
    fun Project.forInsideAar(from: String, to: String, dropResourcesWithSuffix: String?) {
        forInsideAar(listOf(Relocation(from, to)), dropResourcesWithSuffix)
    }

    /**
     * Creates a configuration for users to use that will bundle the dependency jars
     * inside of this lint check's jar. This is required because lintPublish does not currently
     * support dependencies, so instead we need to bundle any dependencies with the lint jar
     * manually. (b/182319899)
     *
     * ```
     * dependencies {
     *     if (rootProject.hasProperty("android.injected.invoked.from.ide")) {
     *         compileOnly(LINT_API_LATEST)
     *     } else {
     *         compileOnly(LINT_API_MIN)
     *     }
     *     compileOnly(KOTLIN_STDLIB)
     *     // Include this library inside the resulting lint jar
     *     bundleInside(project(":foo-lint-utils"))
     * }
     * ```
     *
     * @receiver the project that should bundle jars specified by these configurations
     */
    @JvmStatic
    fun Project.forInsideLintJar() {
        val bundle = createBundleConfiguration()
        val compileOnly = configurations.getByName("compileOnly")
        val testImplementation = configurations.getByName("testImplementation")

        compileOnly.extendsFrom(bundle)
        testImplementation.extendsFrom(bundle)

        val extractTask = tasks.register("extractBundleJars", ExtractJarTask::class.java) { task ->
            task.description = "Extracts all JARs from the bundle configuration."
            task.jarFiles.setFrom(bundle.incoming.artifactView { }.files)
            task.outputDir.set(layout.buildDirectory.dir("extractedJars"))
        }
        tasks.named("jar", Jar::class.java).configure {
            it.from(extractTask.flatMap { it.outputDir })
        }
    }

    data class Relocation(val from: String, val to: String)

    private fun Project.configureRepackageTaskForType(
        relocations: List<Relocation>?,
        configuration: Configuration,
        dropResourcesWithSuffix: String?
    ): TaskProvider<ShadowJar> {
        return tasks.register(REPACKAGE_TASK_NAME, ShadowJar::class.java) { task ->
            task.apply {
                configurations = listOf(configuration)
                if (relocations != null) {
                    for (relocation in relocations) {
                        relocate(relocation.from, relocation.to)
                    }
                }
                val dontIncludeResourceTransformer = DontIncludeResourceTransformer()
                dontIncludeResourceTransformer.dropResourcesWithSuffix = dropResourcesWithSuffix
                transformers.add(dontIncludeResourceTransformer)
                archiveBaseName.set("repackaged")
                archiveVersion.set("")
                destinationDirectory.set(layout.buildDirectory.dir("repackaged"))
            }
        }
    }

    private fun Project.createBundleConfiguration(): Configuration {
        val bundle = configurations.create(CONFIGURATION_NAME) {
            it.attributes {
                   it.attribute(
                         Usage.USAGE_ATTRIBUTE,
                        objects.named<Usage>(Usage.JAVA_RUNTIME)
                   )
            }
            it.isCanBeConsumed = false
        }
        return bundle
    }

    class DontIncludeResourceTransformer : Transformer {
        @Optional @Input var dropResourcesWithSuffix: String? = null

        override fun getName(): String {
            return "DontIncludeResourceTransformer"
        }

        override fun canTransformResource(element: FileTreeElement?): Boolean {
            val path = element?.relativePath?.pathString
            return dropResourcesWithSuffix != null &&
                (path?.endsWith(dropResourcesWithSuffix!!) == true)
        }

        override fun transform(context: TransformerContext?) {
            // no op
        }

        override fun hasTransformedResource(): Boolean {
            return true
        }

        override fun modifyOutputStream(zipOutputStream: ZipOutputStream?, b: Boolean) {
            // no op
        }
    }
}
