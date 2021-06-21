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

import com.android.build.gradle.api.BaseVariant
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import java.io.File
import java.util.jar.JarFile

// variant.taskName relies on @ExperimentalStdlibApi api
@ExperimentalStdlibApi
fun Project.registerShadowDependenciesTask(
    variant: BaseVariant,
    zipTask: TaskProvider<Copy>
): TaskProvider<ShadowJar> {
    val uberJar = registerUberJarTask(variant)
    val versionTask = project.registerGenerateInspectionPlatformVersionTask(variant)
    return tasks.register(
        variant.taskName("shadowDependencies"),
        ShadowJar::class.java
    ) {
        it.dependsOn(uberJar)
        it.dependsOn(versionTask)
        val fileTree = project.fileTree(zipTask.get().destinationDir)
        fileTree.include("**/*.jar", "**/*.so")
        it.from(fileTree)
        it.from(versionTask.get().outputDir)
        it.includeEmptyDirs = false
        it.filesMatching("**/*.so") {
            if (it.path.startsWith("jni")) {
                it.path = "lib/${it.path.removePrefix("jni")}"
            }
        }
        it.transform(RenameServicesTransformer::class.java)
        it.from(versionTask.get().outputDir)
        it.destinationDirectory.set(taskWorkingDir(variant, "shadowedJar"))
        it.archiveBaseName.set("${project.name}-shadowed")
        it.dependsOn(zipTask)
        val prefix = "deps.${project.name.replace('-', '.')}"
        it.doFirst {
            val task = it as ShadowJar
            val input = uberJar.get().outputs.files
            task.from(input)
            input.extractPackageNames().forEach { packageName ->
                task.relocate(packageName, "$prefix.$packageName")
            }
        }
    }
}

/**
 * Merges all runtime dependencies in one jar and removes module-info.class,
 * because jarjar and dx fail to process these classes.
 */
private fun Project.registerUberJarTask(variant: BaseVariant): TaskProvider<Jar> {
    return tasks.register("uberRuntimeDepsJar", Jar::class.java) {
        it.dependsOn(variant.assembleProvider)
        it.archiveClassifier.set("uberRuntimeDepsJar")
        it.exclude("**/module-info.class")
        it.exclude("META-INF/versions/9/**/*.class")
        it.from({
            variant.runtimeConfiguration.incoming.artifactView {
                it.attributes.attribute(
                    Attribute.of("artifactType", String::class.java),
                    ArtifactTypeDefinition.JAR_TYPE
                )
            }.files.filter { it.name.endsWith("jar") }.map(::zipTree)
        })
    }
}

private fun Iterable<File>.extractPackageNames(): Set<String> = map(::JarFile)
    .map { jar -> jar.use { it.entries().toList() } }.flatten()
    .filter { jarEntry -> jarEntry.name.endsWith(".class") }
    .map { jarEntry -> jarEntry.name.substringBeforeLast("/").replace('/', '.') }
    .toSet()

/**
 * Transformer that renames services included in META-INF.
 *
 * kotlin-reflect has two META-INF/services in it. Interfaces of these services and theirs
 * implementations live in the kotlin-reflect itself. This transformer renames files that
 * live in meta-inf directory and their contents respecting the rules supplied into shadowJar.
 */
class RenameServicesTransformer : Transformer {
    private val renamed = mutableMapOf<String, String>()

    override fun canTransformResource(element: FileTreeElement?): Boolean {
        return element?.relativePath?.startsWith("META-INF/services") ?: false
    }

    override fun transform(context: TransformerContext?) {
        if (context == null) return
        val path = context.path.removePrefix("META-INF/services/")

        renamed[context.relocateOrSelf(path)] = context.`is`.bufferedReader().use { it.readLines() }
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
    val relocator = relocators.find {
        it.canRelocateClass(className)
    }
    return relocator?.relocateClass(relocateContext) ?: className
}
