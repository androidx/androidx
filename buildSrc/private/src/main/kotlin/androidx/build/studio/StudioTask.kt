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

package androidx.build.studio

import androidx.build.OperatingSystem
import androidx.build.ProjectLayoutType
import androidx.build.getOperatingSystem
import androidx.build.getSdkPath
import androidx.build.getSupportRootFolder
import androidx.build.getVersionByName
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Base task with common logic for updating and launching studio in both the frameworks/support
 * project and playground projects. Project-specific configuration is provided by [RootStudioTask]
 * and [PlaygroundStudioTask].
 */
@DisableCachingByDefault(because = "the purpose of this task is to launch Studio")
abstract class StudioTask : DefaultTask() {

    // TODO: support -y and --update-only options? Can use @Option for this
    @TaskAction
    fun studiow() {
        validateEnvironment()
        install()
        launch()
    }

    private val platformUtilities by lazy {
        StudioPlatformUtilities.get(projectRoot, studioInstallationDir)
    }

    @get:Inject abstract val execOperations: ExecOperations

    /**
     * If `true`, checks for `ANDROIDX_PROJECTS` environment variable to decide which projects need
     * to be loaded.
     */
    @get:Internal protected open val requiresProjectList: Boolean = true

    @get:Internal protected val projectRoot: File = project.rootDir

    @get:Internal protected open val installParentDir: File = project.rootDir

    private val studioVersion by lazy { project.getVersionByName("androidStudio") }

    /** Directory name (not path) that Studio will be unzipped into. */
    private val studioDirectoryName: String
        get() {
            val osName = StudioPlatformUtilities.osName
            return "android-studio-$studioVersion-$osName"
        }

    /** Filename (not path) of the Studio archive */
    private val studioArchiveName: String
        get() = studioDirectoryName + platformUtilities.archiveExtension

    /**
     * The install directory containing Studio
     *
     * Note: Given that the contents of this directory changes a lot, we don't want to annotate this
     * property for task avoidance - it's not stable enough for us to get any value out of this.
     */
    private val studioInstallationDir by lazy {
        File(installParentDir, "studio/$studioDirectoryName")
    }

    /** Absolute path of the Studio archive */
    private val studioArchivePath: String by lazy {
        File(studioInstallationDir.parentFile, studioArchiveName).absolutePath
    }

    /** The idea.properties file that we want to tell Studio to use */
    @get:Internal protected abstract val ideaProperties: File

    /** The studio.vmoptions file that we want to start Studio with */
    @get:Internal
    open val vmOptions = File(project.getSupportRootFolder(), "development/studio/studio.vmoptions")

    /** The path to the SDK directory used by Studio. */
    @get:Internal open val localSdkPath = project.getSdkPath()

    /** List of additional environment variables to pass into the Studio application. */
    @get:Internal open val additionalEnvironmentProperties: Map<String, String> = emptyMap()

    private val licenseAcceptedFile: File by lazy {
        File("$studioInstallationDir/STUDIOW_LICENSE_ACCEPTED")
    }

    /** Ensure that we can launch Studio without issue. */
    private fun validateEnvironment() {
        if (System.getenv().containsKey("SSH_CLIENT") && !System.getenv().containsKey("DISPLAY")) {
            throw GradleException(
                """
                Studio must be run from a graphical session.

                Could not read DISPLAY environment variable.  If you are using SSH into a remote
                machine, consider using either ssh -X or switching to Chrome Remote Desktop.
                """
                    .trimIndent()
            )
        }
    }

    /** Install Studio and removes any old installation files if they exist. */
    private fun install() {
        val successfulInstallFile = File("$studioInstallationDir/INSTALL_SUCCESSFUL")
        if (!licenseAcceptedFile.exists() && !successfulInstallFile.exists()) {
            // Attempt to remove any old installations in the parent studio/ folder
            studioInstallationDir.parentFile.deleteRecursively()
            // Create installation directory and any needed parent directories
            studioInstallationDir.mkdirs()
            downloadStudioArchive(
                execOperations,
                studioVersion,
                studioArchiveName,
                studioArchivePath
            )
            println("Extracting archive...")
            extractStudioArchive()
            with(platformUtilities) { updateJvmHeapSize() }
            // Finish install process
            successfulInstallFile.createNewFile()
        }
    }

    /** Attempts to symlink the system-images and emulator SDK directories to a canonical SDK. */
    private fun setupSymlinksIfNeeded() {
        val paths = listOf("system-images", "emulator")
        if (!localSdkPath.exists()) {
            // We probably got the support root folder wrong. Fail gracefully.
            return
        }

        val relativeSdkPath =
            when (val osType = getOperatingSystem()) {
                OperatingSystem.MAC -> "Library/Android/sdk"
                OperatingSystem.LINUX -> "Android/Sdk"
                else -> {
                    println("Failed to locate canonical SDK, unsupported operating system: $osType")
                    return
                }
            }

        val canonicalSdkPath = File(File(System.getProperty("user.home")).parent, relativeSdkPath)
        if (!canonicalSdkPath.exists()) {
            // In the future, we might want to try a little harder to locate a canonical SDK path.
            println("Failed to locate canonical SDK, not found at: $canonicalSdkPath")
            return
        }

        paths.forEach { path ->
            val link = File(localSdkPath, path)
            val target = File(canonicalSdkPath, path)
            if (!target.exists()) {
                println("Skipping canonical SDK symlink creation, not found at: $target")
            } else if (!link.exists()) {
                println("Creating canonical SDK symlink for $target...")
                Files.createSymbolicLink(link.toPath(), target.toPath())
            }
        }
    }

    /** Launches Studio if the user accepts / has accepted the license agreement. */
    private fun launch() {
        if (checkLicenseAgreement(services)) {
            if (
                requiresProjectList &&
                    !System.getenv().containsKey("ANDROIDX_PROJECTS") &&
                    !System.getenv().containsKey("PROJECT_PREFIX")
            ) {
                throw GradleException(
                    """
                    Please specify which set of projects you'd like to open in studio
                    with ANDROIDX_PROJECTS=MAIN ./gradlew studio
                    or PROJECT_PREFIX=:room: ./gradlew studio

                    For possible options see settings.gradle
                    """
                        .trimIndent()
                )
            }

            // This seems like as good a time as any to set up SDK symlinks...
            setupSymlinksIfNeeded()

            println("Launching studio...")
            launchStudio()
        } else {
            println("Exiting without launching studio...")
        }
    }

    private fun launchStudio() {
        check(ideaProperties.exists()) {
            "Invalid Studio properties file location: ${ideaProperties.canonicalPath}"
        }
        check(vmOptions.exists()) {
            "Invalid Studio vm options file location: ${vmOptions.canonicalPath}"
        }
        val pid = with(platformUtilities) { findProcess() }
        check(pid == null) {
            "Found managed instance of Studio already running as PID $pid"
        }
        val logFile = File(System.getProperty("user.home"), ".AndroidXStudioLog")
        ProcessBuilder().apply {
            // Can't just use inheritIO due to https://github.com/gradle/gradle/issues/16719
            // Also can't use waitFor because it causes Studio to get stuck: b/241386076
            // So, we save this output in a file and display the path to the user
            redirectOutput(logFile)
            redirectError(logFile)
            with(platformUtilities) { command(launchCommandArguments) }

            val additionalStudioEnvironmentProperties =
                mapOf(
                    // These environment variables are used to set up AndroidX's default
                    // configuration.
                    "STUDIO_PROPERTIES" to ideaProperties.canonicalPath,
                    "STUDIO_VM_OPTIONS" to vmOptions.canonicalPath,
                    // This environment variable prevents Studio from showing IDE inspection
                    // warnings
                    // for nullability issues, if the context is deprecated. This environment
                    // variable
                    // is consumed by InteroperabilityDetector.kt
                    "ANDROID_LINT_NULLNESS_IGNORE_DEPRECATED" to "true",
                    // This environment variable is read by AndroidXRootImplPlugin to ensure that
                    // Studio-initiated Gradle tasks are run against the same version of AGP that
                    // was
                    // used to start Studio, which prevents version mismatch after repo sync.
                    "EXPECTED_AGP_VERSION" to ANDROID_GRADLE_PLUGIN_VERSION
                ) + additionalEnvironmentProperties

            // Append to the existing environment variables set by gradlew and the user.
            environment().putAll(additionalStudioEnvironmentProperties)
            start()
        }
        println("Studio log at $logFile")
    }

    private fun checkLicenseAgreement(services: ServiceRegistry): Boolean {
        if (!licenseAcceptedFile.exists()) {
            val licensePath = with(platformUtilities) { licensePath }

            val userInput = services.get(UserInputHandler::class.java)
            val acceptAgreement =
                userInput.askYesNoQuestion(
                    "Do you accept the license agreement at $licensePath?"
                )
            if (acceptAgreement == null || !acceptAgreement) {
                return false
            }
            licenseAcceptedFile.createNewFile()
        }
        return true
    }

    private fun downloadStudioArchive(
        execOperations: ExecOperations,
        studioVersion: String,
        filename: String,
        destinationPath: String
    ) {
        val url = "https://dl.google.com/dl/android/studio/ide-zips/$studioVersion/$filename"
        val tmpDownloadPath = File("$destinationPath.tmp").absolutePath
        println("Downloading $url to $tmpDownloadPath")
        execOperations.exec { execSpec ->
            with(execSpec) {
                executable("curl")
                args(url, "--output", tmpDownloadPath)
            }
        }

        // Renames temp archive to the final archive name
        Files.move(Paths.get(tmpDownloadPath), Paths.get(destinationPath))
    }

    private fun extractStudioArchive() {
        val fromPath = studioArchivePath
        val toPath = studioInstallationDir.absolutePath
        println("Extracting to $toPath...")
        execOperations.exec { execSpec ->
            platformUtilities.extractArchive(fromPath, toPath, execSpec)
        }
        // Remove studio archive once done
        File(studioArchivePath).delete()
    }

    companion object {
        private const val STUDIO_TASK = "studio"

        fun Project.registerStudioTask() {
            val studioTask =
                when (ProjectLayoutType.from(this)) {
                    ProjectLayoutType.ANDROIDX -> RootStudioTask::class.java
                    ProjectLayoutType.PLAYGROUND -> PlaygroundStudioTask::class.java
                }
            tasks.register(STUDIO_TASK, studioTask)
        }
    }
}

/** Task for launching studio in the frameworks/support project */
@DisableCachingByDefault(because = "the purpose of this task is to launch Studio")
abstract class RootStudioTask : StudioTask() {
    override val ideaProperties
        get() = projectRoot.resolve("development/studio/idea.properties")
}

/** Task for launching studio in a playground project */
@DisableCachingByDefault(because = "the purpose of this task is to launch Studio")
abstract class PlaygroundStudioTask : RootStudioTask() {
    @get:Internal
    val supportRootFolder =
        (project.rootProject.property("ext") as ExtraPropertiesExtension).let {
            it.get("supportRootFolder") as File
        }

    /** Playground projects have only 1 setup so there is no need to specify the project list. */
    override val requiresProjectList
        get() = false

    override val installParentDir
        get() = supportRootFolder

    override val additionalEnvironmentProperties: Map<String, String>
        get() = mapOf("ALLOW_PUBLIC_REPOS" to "true")

    override val ideaProperties
        get() = supportRootFolder.resolve("playground-common/idea.properties")

    override val vmOptions
        get() = supportRootFolder.resolve("playground-common/studio.vmoptions")
}
