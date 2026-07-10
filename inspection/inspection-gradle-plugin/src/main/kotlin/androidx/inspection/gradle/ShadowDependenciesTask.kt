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

package androidx.inspection.gradle

import com.android.build.api.variant.Variant
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import java.io.File
import java.util.jar.JarFile
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider

fun Project.registerShadowDependenciesTask(
    variant: Variant,
    extension: InspectionExtension,
    zipTask: TaskProvider<Copy>,
): TaskProvider<ShadowJar> {
    val versionTask = registerGenerateInspectionPlatformVersionTask(variant)
    return tasks.register(
        variant.taskName("inspectionShadowDependencies"),
        ShadowJar::class.java,
    ) { task ->
        task.dependsOn(versionTask)
        val fileTree = project.fileTree(zipTask.get().destinationDir)
        fileTree.include("**/*.jar", "**/*.so")
        task.from(fileTree)
        task.from(versionTask.get().outputDir)
        task.includeEmptyDirs = false
        task.filesMatching("**/*.so") {
            if (it.path.startsWith("jni")) {
                it.path = "lib/${it.path.removePrefix("jni")}"
            }
        }
        task.transform(RenameServicesTransformer::class.java)
        task.destinationDirectory.set(taskWorkingDir(variant, "shadowedJar"))
        task.archiveBaseName.set("${extension.name ?: project.name}-nondexed")
        task.archiveVersion.set("")
        task.dependsOn(zipTask)
        val prefix = "deps.${project.name.replace('-', '.')}"

        val view =
            variant.runtimeConfiguration.incoming.artifactView { viewConfig ->
                viewConfig.attributes.attribute(
                    Attribute.of("artifactType", String::class.java),
                    ArtifactTypeDefinition.JAR_TYPE,
                )
            }

        val filteredRuntimeDepsProvider =
            extension.excludedModules
                .zip(extension.allowedModules) { excludes, allows -> excludes to allows }
                .zip(view.files.elements) { (excludes, allows), elements ->
                    Triple(excludes, allows, elements)
                }
                .zip(view.artifacts.resolvedArtifacts) { (excludes, allows, elements), artifacts ->
                    fun matches(id: ModuleComponentIdentifier, patterns: Set<String>): Boolean {
                        val fullId = "${id.group}:${id.module}"
                        return patterns.any { pattern ->
                            when {
                                pattern == fullId -> true
                                pattern == "${id.group}:*" -> true
                                pattern.endsWith("*") ->
                                    fullId.startsWith(pattern.removeSuffix("*"))
                                else -> false
                            }
                        }
                    }

                    if (excludes.isEmpty() && allows.isEmpty()) {
                        elements.map { it.asFile }.toSet()
                    } else {
                        val artifactsToExclude =
                            artifacts
                                .filter { artifact ->
                                    val id = artifact.id.componentIdentifier
                                    if (id is ModuleComponentIdentifier) {
                                        val excluded = matches(id, excludes)
                                        val allowed = matches(id, allows)
                                        excluded && !allowed
                                    } else {
                                        false
                                    }
                                }
                                .map { it.file }
                                .toSet()
                        elements
                            .map { it.asFile }
                            .filter { file -> file !in artifactsToExclude }
                            .toSet()
                    }
                }

        task.exclude("**/module-info.class")
        task.exclude("google/**/*.proto")
        task.exclude("META-INF/versions/9/**/*.class")

        task.from(filteredRuntimeDepsProvider)

        task.doFirst {
            val shadow = it as ShadowJar
            filteredRuntimeDepsProvider
                .get()
                .flatMap { file -> file.extractPackageNames() }
                .toSet()
                .forEach { pkg -> shadow.relocate(pkg, "$prefix.$pkg") }
        }
    }
}

private fun File.extractPackageNames(): Set<String> =
    JarFile(this)
        .use { it.entries().toList() }
        .filter { jarEntry -> jarEntry.name.endsWith(".class") }
        .map { jarEntry -> jarEntry.name.substringBeforeLast("/").replace('/', '.') }
        .toSet()

/**
 * Transformer that renames services included in META-INF.
 *
 * kotlin-reflect has two META-INF/services in it. Interfaces of these services and theirs
 * implementations live in the kotlin-reflect itself. This transformer renames files that live in
 * meta-inf directory and their contents respecting the rules supplied into shadowJar.
 */
class RenameServicesTransformer : Transformer {
    private val renamed = mutableMapOf<String, String>()

    override fun getName(): String {
        return "RenameServicesTransformer"
    }

    override fun canTransformResource(element: FileTreeElement?): Boolean {
        return element?.relativePath?.startsWith("META-INF/services") ?: false
    }

    override fun transform(context: TransformerContext?) {
        if (context == null) return
        val path = context.path.removePrefix("META-INF/services/")

        renamed[context.relocateOrSelf(path)] =
            context.`is`
                .bufferedReader()
                .use { it.readLines() }
                .joinToString("\n") { line -> context.relocateOrSelf(line) }
    }

    override fun hasTransformedResource(): Boolean {
        return renamed.isNotEmpty()
    }

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        renamed.forEach { (name, text) ->
            val entry = ZipEntry("META-INF/services/$name")
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            os.putNextEntry(entry)
            text.byteInputStream().copyTo(os)
            os.closeEntry()
        }
    }
}

private fun TransformerContext.relocateOrSelf(className: String): String {
    val relocateContext = RelocateClassContext(className, stats)
    val relocator = relocators.find { it.canRelocateClass(className) }
    return relocator?.relocateClass(relocateContext) ?: className
}
