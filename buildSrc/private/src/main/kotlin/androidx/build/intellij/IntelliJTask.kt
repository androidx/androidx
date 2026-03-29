/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.intellij

import androidx.build.ProjectLayoutType
import androidx.build.getSdkPath
import androidx.build.getSupportRootFolder
import androidx.build.getVersionByName
import androidx.build.studio.StudioTask.Companion.validateEnvironment
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "the purpose of this task is to launch IntelliJ")
abstract class IntelliJTask : DefaultTask() {

    @get:Internal protected open val installParentDir: File = project.rootDir
    @get:Internal protected val projectRoot: File = project.rootDir
    @get:Inject abstract val execOperations: ExecOperations

    /** The idea.properties file that we want to tell IntelliJ to use */
    @get:Internal protected abstract val ideaProperties: File

    private val intelliJVersion by lazy { project.getVersionByName("intelliJVersion") }

    /** Directory name (not path) that IntelliJ will be unzipped into. */
    private val intelliJDirectoryName: String
        get() {
            val osName = IntelliJPlatformUtilities.osName
            return "intellij-$intelliJVersion-$osName"
        }

    /** Filename (not path) of the IntelliJ archive */
    private val intelliJArchiveName: String
        get() = intelliJDirectoryName + platformUtilities.archiveExtension

    /** Absolute path of the IntelliJ archive */
    private val intelliJArchivePath: String by lazy {
        File(intelliJInstallationDir.parentFile, intelliJArchiveName).absolutePath
    }

    /**
     * The install directory containing IntelliJ
     *
     * Note: Given that the contents of this directory changes a lot, we don't want to annotate this
     * property for task avoidance - it's not stable enough for us to get any value out of this.
     */
    private val intelliJInstallationDir by lazy {
        File(installParentDir, "intellij/$intelliJDirectoryName")
    }

    private val licenseAcceptedFile: File by lazy {
        File("$intelliJInstallationDir/INTELLIJW_LICENSE_ACCEPTED")
    }

    private val intelliJConfigBaseDir =
        File(project.providers.environmentVariable("HOME").get(), ".IntelliJAndroidX/config").also {
            it.mkdirs()
        }

    private val intelliJOptionsDir = File(intelliJConfigBaseDir, "options").also { it.mkdirs() }

    /** The path to the SDK directory used by IntelliJ. */
    @get:Internal
    open val localSdkPath = project.getSdkPath().relativeTo(project.getSupportRootFolder())

    @TaskAction
    fun intellijw() {
        validateEnvironment("IntelliJ")
        install()
        writeAndroidSdkPath()
    }

    private val platformUtilities by lazy {
        IntelliJPlatformUtilities.get(projectRoot, intelliJInstallationDir)
    }

    /** Install IntelliJ and removes any old installation files if they exist. */
    private fun install() {
        val successfulInstallFile = File("$intelliJInstallationDir/INSTALL_SUCCESSFUL")
        if (!licenseAcceptedFile.exists() && !successfulInstallFile.exists()) {
            // Attempt to remove any old installations in the parent intellij/ folder
            intelliJInstallationDir.parentFile.deleteRecursively()
            // Create installation directory and any needed parent directories
            intelliJInstallationDir.mkdirs()
            downloadIntelliJArchive(
                execOperations,
                intelliJVersion,
                intelliJArchiveName,
                intelliJArchivePath,
            )
            println("Extracting archive...")
            extractIntelliJArchive()
            // Finish install process
            successfulInstallFile.createNewFile()
        }
    }

    private fun downloadIntelliJArchive(
        execOperations: ExecOperations,
        intelliJVersion: String,
        filename: String,
        destinationPath: String,
    ) {
        val url =
            if (filename.contains("-mac")) {
                "https://download.jetbrains.com/idea/idea-$intelliJVersion-aarch64.dmg"
            } else {
                "https://download.jetbrains.com/idea/idea-$intelliJVersion.tar.gz"
            }
        val tmpDownloadPath = File("$destinationPath.tmp").absolutePath
        println("Downloading $url to $tmpDownloadPath")
        execOperations.exec { execSpec ->
            with(execSpec) {
                executable("curl")
                args("-L", url, "--output", tmpDownloadPath)
            }
        }

        // Renames temp archive to the final archive name
        Files.move(Paths.get(tmpDownloadPath), Paths.get(destinationPath))
    }

    private fun extractIntelliJArchive() {
        val fromPath = intelliJArchivePath
        val toPath = intelliJInstallationDir.absolutePath
        println("Extracting to $toPath...")
        platformUtilities.extractArchive(fromPath, toPath, execOperations)
        // Remove intellij archive once done
        File(intelliJArchivePath).delete()
    }

    // TODO(b/443681166) Remove when fixed
    private fun writeAndroidSdkPath() {
        val sdkPathFile = File(intelliJOptionsDir, "android.sdk.path.xml")
        sdkPathFile.writeText(
            """
                <application>
                  <component name="AndroidSdkPathStore">
                    <option name="androidSdkAbsolutePath" value="${localSdkPath.path}" />
                  </component>
                </application>
                        """
                .trimIndent()
        )
    }

    companion object {
        private const val INTELLIJ_TASK = "intellij"

        fun Project.registerIntelliJTask() {
            val intellijTask =
                when (ProjectLayoutType.from(this)) {
                    ProjectLayoutType.ANDROIDX -> RootIntelliJTask::class.java
                    ProjectLayoutType.PLAYGROUND -> return
                }
            tasks.register(INTELLIJ_TASK, intellijTask)
        }
    }
}

/** Task for launching intellij in the frameworks/support project */
@DisableCachingByDefault(because = "the purpose of this task is to launch IntelliJ")
abstract class RootIntelliJTask : IntelliJTask() {
    override val ideaProperties
        get() = projectRoot.resolve("development/intellij/idea.properties")
}
