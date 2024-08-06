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

package androidx.build.license

import androidx.build.License
import androidx.build.ZipStubAarTask
import androidx.build.androidXExtension
import androidx.build.getSupportRootFolder
import androidx.build.multiplatformExtension
import com.android.build.gradle.tasks.BundleAar
import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

/** Adds license file to published JAR, AAR, and Klib artifacts. */
internal fun Project.addLicensesToPublishedArtifacts(license: License) {
    val groupSubdir = androidXExtension.mavenGroup?.group!!.replace('.', '/')
    val projectSubdir = File(groupSubdir, project.name)
    val licenseFile = licenseUrlToLicenseFile[license.url]

    checkNotNull(licenseFile) {
        "The ${license.name} license being added to the project ${project.path} is not approved."
    }

    // Remove when Gradle creates API for adding license file and setting its location:
    // https://github.com/gradle/gradle/issues/29536
    tasks.withType<Jar>().configureEach { task ->
        task.from(licenseFile) { it.into("META-INF/$projectSubdir") }
    }

    // Remove when AGP creates API for adding license file and setting its location:
    // https://issuetracker.google.com/337785420
    tasks.withType<BundleAar>().configureEach { task ->
        task.from(licenseFile) { it.into("META-INF/$projectSubdir") }
    }

    tasks.withType<ZipStubAarTask>().configureEach { task ->
        task.from(licenseFile) { it.into("META-INF/$projectSubdir") }
    }

    // Remove when KMP creates API for adding license file and setting its location:
    // https://youtrack.jetbrains.com/issue/KT-69084
    tasks.withType<CInteropProcess>().configureEach { task ->
        task.doLast {
            val outputFile = task.outputFileProvider.get()
            outputFile.writeToZip { fileSystem ->
                val licenseDir = fileSystem.getPath("/default/licenses/$projectSubdir")
                Files.createDirectories(licenseDir)
                Files.writeString(licenseDir.resolve("LICENSE.txt"), licenseFile.readText())
            }
        }
    }

    // Remove when KMP creates API for adding license file and setting its location:
    // https://youtrack.jetbrains.com/issue/KT-69084
    multiplatformExtension?.targets?.withType<KotlinNativeTarget>()?.configureEach { target ->
        target.compilations.configureEach { compilation ->
            val compileTaskOutputFileProvider =
                compilation.compileTaskProvider.flatMap { it.outputFile }

            compilation.compileTaskProvider.configure {
                it.doLast {
                    compileTaskOutputFileProvider.get().writeToZip { fileSystem ->
                        val licenseDir = fileSystem.getPath("/default/licenses/$projectSubdir")
                        Files.createDirectories(licenseDir)
                        Files.writeString(licenseDir.resolve("LICENSE.txt"), licenseFile.readText())
                    }
                }
            }
        }
    }
}

private fun File.writeToZip(write: (FileSystem) -> Unit) {
    val fileUri = toURI()
    val uri =
        URI(
            "jar:file",
            fileUri.userInfo,
            fileUri.host,
            fileUri.port,
            fileUri.path,
            fileUri.query,
            fileUri.fragment
        )

    val fileSystem =
        try {
            FileSystems.getFileSystem(uri)
        } catch (e: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, mapOf("create" to "true"))
        }
    fileSystem.use(write)
}

private val Project.licenseUrlToLicenseFile: Map<String, File>
    get() {
        val allowedLicensesFolder = File(getSupportRootFolder(), "buildSrc/allowedLicenses")
        return mapOf(
            "http://www.apache.org/licenses/LICENSE-2.0.txt" to
                File("$allowedLicensesFolder/Apache-2.0/LICENSE.txt"),
            "https://opensource.org/licenses/BSD-3-Clause" to
                File("$allowedLicensesFolder/BSD-3-Clause/LICENSE.txt"),
        )
    }
