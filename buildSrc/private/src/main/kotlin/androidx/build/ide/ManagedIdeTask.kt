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

package androidx.build.ide

import androidx.build.BuildEnvironment
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Base task for the managed IDE launchers like [androidx.build.studio.StudioTask]
 *
 * Encodes the lifecycle managed IDE follows: validate the environment, install a managed copy of
 * the IDE, provision it, gate on license acceptance and project scoping, and launch it with the
 * AndroidX build environment variables so that IDE-initiated Gradle builds run against the same
 * JDK, SDK, output directories, and AGP version as the command line.
 */
@DisableCachingByDefault(because = "the purpose of these tasks is to launch an IDE")
abstract class ManagedIdeTask : DefaultTask() {

    @get:Inject abstract val execOperations: ExecOperations
    @get:Inject abstract val archiveOperations: ArchiveOperations
    @get:Inject abstract val fileSystemOperations: FileSystemOperations
    @get:Inject abstract val layout: ProjectLayout
    @get:Inject abstract val providers: ProviderFactory
    @get:Inject
    @get:Suppress("InternalGradleApiUsage")
    abstract val userInputHandler: org.gradle.api.internal.tasks.userinput.UserInputHandler

    @get:Input abstract val ideName: Property<String>
    @get:Input abstract val ideArchiveName: Property<String>
    @get:Input abstract val archiveUrl: Property<String>
    @get:Input abstract val licenseAgreementPath: Property<String>
    @get:Input abstract val additionalEnvironmentProperties: MapProperty<String, String>
    @get:Input abstract val requiresProjectList: Property<Boolean>
    @get:Input abstract val launchArguments: ListProperty<String>
    @get:Input abstract val ideBinaryRelativePath: Property<String>

    @get:Input
    @get:Option(option = "acceptTos", description = "Accept the IDE Terms of Service")
    @get:Optional
    abstract val acceptTos: Property<Boolean>

    @get:Internal
    val projectRoot: File
        get() = layout.projectDirectory.asFile

    @get:Internal abstract val installParentDir: DirectoryProperty

    @get:Internal abstract val provisionAction: Property<Action<ManagedIdeTask>>

    @get:Internal
    val osName =
        when {
            System.getProperty("os.name").lowercase(Locale.ROOT).contains("linux") -> "linux"
            System.getProperty("os.arch") == "aarch64" -> "mac_arm"
            else -> "mac"
        }

    init {
        requiresProjectList.convention(true)
        installParentDir.convention(layout.projectDirectory)
        launchArguments.convention(listOf(projectRoot.absolutePath))
    }

    companion object {
        private const val EXT_DMG = "dmg"
        private const val EXT_TAR_GZ = "tar.gz"
        private const val EXT_ZIP = "zip"
    }

    private fun getArchiveExtension(): String =
        ideArchiveName.get().let { name ->
            when {
                name.endsWith(EXT_TAR_GZ) -> EXT_TAR_GZ
                name.endsWith(EXT_DMG) -> EXT_DMG
                name.endsWith(EXT_ZIP) -> EXT_ZIP
                else -> throw GradleException("Unsupported archive extension in filename: $name")
            }
        }

    private fun getIdeDirectoryName(): String =
        ideArchiveName.get().removeSuffix(".${getArchiveExtension()}")

    private fun getIdeInstallationDir(): File =
        installParentDir
            .get()
            .asFile
            .resolve(ideName.get().lowercase())
            .resolve(getIdeDirectoryName())

    private fun getIdeRoot(): File {
        val installDir = getIdeInstallationDir()
        return if (osName == "linux") {
            resolveLinuxIdeRoot(installDir)
        } else {
            resolveMacAppBundle(installDir)
        }
    }

    private fun resolveLinuxIdeRoot(installDir: File): File {
        return installDir.listFiles { f -> f.isDirectory }?.singleOrNull() ?: installDir
    }

    private fun resolveMacAppBundle(installDir: File): File {
        return installDir.listFiles()?.find { it.extension == "app" }
            ?: throw GradleException("App bundle (.app) not found in $installDir")
    }

    private fun getIdeArchiveFile(): File =
        installParentDir
            .get()
            .asFile
            .resolve(ideName.get().lowercase())
            .resolve(ideArchiveName.get())

    private fun getLicenseAcceptedFile(): File =
        getIdeInstallationDir().resolve("${ideName.get().uppercase()}_LICENSE_ACCEPTED")

    private fun getLaunchTaskName(): String = ideName.get().lowercase()

    @TaskAction
    fun updateAndLaunchIde() {
        BuildEnvironment.validateEnvironment(ideName.get())
        install()
        provision()
        launch()
    }

    /** Install the IDE and removes any old installation files if they exist. */
    private fun install() {
        val installDir = getIdeInstallationDir()
        val licenseFile = getLicenseAcceptedFile()
        val successfulInstallFile = File(installDir, "INSTALL_SUCCESSFUL")
        if (!licenseFile.exists() && !successfulInstallFile.exists()) {
            // Attempt to remove any old installations in the parent folder
            installDir.parentFile.deleteRecursively()
            // Create installation directory and any needed parent directories
            installDir.mkdirs()
            val archiveFile = getIdeArchiveFile()
            downloadIdeArchive(archiveUrl.get(), archiveFile)
            println("Extracting archive...")
            extractIdeArchive()
            // Finish install process
            successfulInstallFile.createNewFile()
        }
    }

    private fun extractIdeArchive() {
        val archiveFile = getIdeArchiveFile()
        val fromPath = archiveFile.absolutePath
        val toPath = getIdeInstallationDir().absolutePath
        println("Extracting to $toPath...")
        when (getArchiveExtension()) {
            EXT_DMG -> {
                val mountPoint =
                    File.createTempFile("mount", null).apply {
                        delete()
                        mkdir()
                    }
                execOperations.exec { execSpec ->
                    execSpec.executable("hdiutil")
                    execSpec.args(
                        "attach",
                        fromPath,
                        "-noverify",
                        "-mountpoint",
                        mountPoint.absolutePath,
                    )
                }
                execOperations.exec { execSpec ->
                    execSpec.commandLine(
                        "sh",
                        "-c",
                        "cp -R ${mountPoint.absolutePath}/*.app $toPath",
                    )
                }
                execOperations.exec { execSpec ->
                    execSpec.executable("hdiutil")
                    execSpec.args("detach", mountPoint.absolutePath)
                }
                mountPoint.delete()
            }
            EXT_TAR_GZ -> {
                fileSystemOperations.copy {
                    with(it) {
                        from(archiveOperations.tarTree(archiveFile))
                        into(getIdeInstallationDir())
                    }
                }
            }
            EXT_ZIP -> {
                execOperations.exec { execSpec ->
                    execSpec.executable("unzip")
                    execSpec.args("-q", "-o", fromPath, "-d", toPath)
                }
            }
            else -> throw GradleException("Unsupported archive extension: ${getArchiveExtension()}")
        }
        archiveFile.delete()
    }

    private fun provision() {
        if (provisionAction.isPresent) {
            provisionAction.get().execute(this)
        }
    }

    /** Launches the IDE if the user accepts / has accepted the license agreement. */
    private fun launch() {
        if (checkLicenseAgreement()) {
            val launchTaskName = getLaunchTaskName()
            if (requiresProjectList.get()) {
                BuildEnvironment.requireProjectScope(
                    ide = launchTaskName,
                    launchTask = launchTaskName,
                )
            }

            println("Launching $launchTaskName...")
            startIde()
        } else {
            println("Exiting without launching ${getLaunchTaskName()}...")
        }
    }

    private fun startIde() {
        verifyNoActiveProcess()

        val logFile = File(System.getProperty("user.home"), ".AndroidX${ideName.get()}Log")
        val launchCommand = getLaunchCommand()
        startIdeProcess(launchCommand, logFile)
        println("${ideName.get()} log at $logFile")
    }

    fun resolveInstallationFile(relativePath: String): File {
        return getIdeRoot().resolve(relativePath)
    }

    private fun getLaunchCommand(): List<String> {
        val binary = resolveInstallationFile(ideBinaryRelativePath.get())
        check(binary.exists() && Files.isExecutable(binary.toPath())) {
            "Executable binary not found or not executable at: ${binary.absolutePath}"
        }
        return listOf(binary.absolutePath) + launchArguments.get()
    }

    private fun findProcess(): Int? {
        println("Detecting active managed ${ideName.get()} instances...")
        val process =
            ProcessBuilder("ps", "-x").redirectError(ProcessBuilder.Redirect.INHERIT).start()
        val stdout = process.inputStream.bufferedReader().use { it.readLines() }
        process.waitFor()
        val projectRootPath = projectRoot.absolutePath
        val pattern = ideName.get().lowercase()
        return stdout
            .firstOrNull { line -> line.contains(pattern) && line.endsWith(projectRootPath) }
            ?.trim()
            ?.substringBefore(' ')
            ?.toIntOrNull()
    }

    private fun verifyNoActiveProcess() {
        val pid = findProcess()
        check(pid == null) {
            "Found managed instance of ${ideName.get()} already running as PID $pid"
        }
    }

    /**
     * Starts the IDE process with the managed environment on top of the environment inherited from
     * gradlew, redirecting its output to [logFile].
     */
    protected fun startIdeProcess(launchCommand: List<String>, logFile: File) {
        ProcessBuilder().apply {
            // Can't just use inheritIO due to https://github.com/gradle/gradle/issues/16719
            // Also can't use waitFor because it causes the IDE to get stuck: b/241386076
            // So, we save this output in a file and display the path to the user
            redirectOutput(logFile)
            redirectError(logFile)
            command(launchCommand)

            val additionalIdeEnvironmentProperties =
                BuildEnvironment.ideEnvironment() + additionalEnvironmentProperties.get()

            // Append to the existing environment variables set by gradlew and the user.
            environment().putAll(additionalIdeEnvironmentProperties)
            start()
        }
    }

    private fun checkLicenseAgreement(): Boolean {
        val licenseFile = getLicenseAcceptedFile()
        if (!licenseFile.exists()) {
            if (!acceptTos.isPresent) {
                val pathOrUrl = licenseAgreementPath.get()
                val displayPath =
                    if (pathOrUrl.startsWith("http")) {
                        pathOrUrl
                    } else {
                        resolveInstallationFile(pathOrUrl).absolutePath
                    }

                @Suppress("InternalGradleApiUsage")
                val acceptAgreement =
                    userInputHandler.askYesNoQuestion(
                        "Do you accept the license agreement at $displayPath?"
                    )
                if (acceptAgreement == null || !acceptAgreement) {
                    return false
                }
            }
            licenseFile.createNewFile()
        }
        return true
    }

    private fun downloadIdeArchive(url: String, destinationFile: File) {
        val tmpDownloadFile = File("${destinationFile.absolutePath}.tmp")
        val tmpDownloadPath = tmpDownloadFile.absolutePath
        println("Downloading $url to $tmpDownloadPath")
        execOperations.exec { execSpec ->
            with(execSpec) {
                executable("curl")
                args("-L", url, "--output", tmpDownloadPath)
            }
        }

        // Renames temp archive to the final archive name
        Files.move(tmpDownloadFile.toPath(), destinationFile.toPath())
    }
}

// TODO(b/443681166) Remove when fixed
fun ManagedIdeTask.writeAndroidSdkPath(configBaseDir: File, sdkPath: File) {
    val optionsDir = configBaseDir.resolve("options").also { it.mkdirs() }
    val sdkPathFile = File(optionsDir, "android.sdk.path.xml")
    sdkPathFile.writeText(
        """
            <application>
              <component name="AndroidSdkPathStore">
                <option name="androidSdkAbsolutePath" value="${sdkPath.path}" />
              </component>
            </application>
                    """
            .trimIndent()
    )
}

fun ManagedIdeTask.configureIntellijLikeIde(
    envPrefix: String,
    ideaPropertiesFile: RegularFileProperty,
    vmOptionsFile: RegularFileProperty,
) {
    additionalEnvironmentProperties.put(
        "${envPrefix}_PROPERTIES",
        ideaPropertiesFile.map { file ->
            val asFile = file.asFile
            check(asFile.exists()) { "Invalid properties file: ${asFile.canonicalPath}" }
            asFile.canonicalPath
        },
    )
    additionalEnvironmentProperties.put(
        "${envPrefix}_VM_OPTIONS",
        vmOptionsFile.map { file ->
            val asFile = file.asFile
            check(asFile.exists()) { "Invalid vm options file: ${asFile.canonicalPath}" }
            asFile.canonicalPath
        },
    )
}

/** Verifies this file against [expectedChecksum], deleting it and failing on a mismatch. */
internal fun File.verifyChecksum(expectedChecksum: String) {
    val actualChecksum =
        MessageDigest.getInstance("SHA-256")
            .also { it.update(this.readBytes()) }
            .digest()
            .joinToString(separator = "") { "%02x".format(it) }

    if (actualChecksum != expectedChecksum) {
        this.delete()
        throw GradleException(
            """
            Checksum mismatch for file: ${this.absolutePath}
            Expected: $expectedChecksum
            Actual:   $actualChecksum
            """
                .trimIndent()
        )
    }
}
