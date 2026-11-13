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

package androidx.build.androidcli

import androidx.build.OperatingSystem
import androidx.build.getOperatingSystem
import androidx.build.getOutDirectory
import androidx.build.getPrebuiltsRoot
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations

/**
 * On-demand CLI packs checked into
 * `prebuilts/androidx/android-cli/packs/<packName>/<packName>.jar`.
 */
internal enum class AndroidCliPack(val packName: String) {
    OPTIMIZATION("optimization")
}

/**
 * Shared Gradle [BuildService] that prepares an offline `ANDROID_USER_HOME` directory from
 * `prebuilts/androidx/android-cli`, stages requested CLI packs, and executes `android` CLI
 * commands.
 */
internal abstract class AndroidCliBuildService
@Inject
constructor(private val execOperations: ExecOperations) :
    BuildService<AndroidCliBuildService.Parameters> {

    interface Parameters : BuildServiceParameters {
        /** Location of `prebuilts/androidx/android-cli`. */
        val prebuiltsDir: DirectoryProperty

        /**
         * Writable directory in `out/` used as `ANDROID_USER_HOME` for offline execution.
         *
         * `ANDROID_USER_HOME` must be in `out/` rather than inside `prebuilts/androidx/android-cli`
         * because the CLI extracts its ~166 MB runtime bundle (`cli/bundles/<sha1>/`) and writes
         * lock and state files (`cli/bundles/lock`, `cli/packs/lock`, `cli/tos_displayed`) into
         * `$ANDROID_USER_HOME`, which would otherwise dirty the read-only `prebuilts/` git
         * checkout.
         */
        val cliHomeDir: DirectoryProperty
    }

    private data class PreparedCliEnvironment(
        val executable: File,
        val homeDir: File,
        val cliDir: File,
        val version: String,
    )

    private val preparedEnvironment: PreparedCliEnvironment by lazy { prepareCliEnvironment() }
    private val stagedPacks = ConcurrentHashMap.newKeySet<AndroidCliPack>()
    private val packLock = Any()

    /**
     * Stages any [requiredPacks] into `ANDROID_USER_HOME`, runs `android --no-metrics <args>`
     * offline, and returns standard output as a string.
     */
    fun execute(args: List<String>, requiredPacks: Set<AndroidCliPack> = emptySet()): String {
        val env = preparedEnvironment
        for (pack in requiredPacks) {
            ensurePackStaged(env, pack)
        }

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val result = execOperations.exec { spec ->
            configureCliExecSpec(
                spec = spec,
                executable = env.executable,
                homeDir = env.homeDir,
                args = args,
                stdout = stdout,
                stderr = stderr,
            )
        }
        if (result.exitValue != 0) {
            throw GradleException(
                "android CLI failed for args $args (exit ${result.exitValue}):\n" +
                    stderr.toString(Charsets.UTF_8) +
                    "\n" +
                    stdout.toString(Charsets.UTF_8)
            )
        }
        return stdout.toString(Charsets.UTF_8)
    }

    private fun configureCliExecSpec(
        spec: org.gradle.process.ExecSpec,
        executable: File,
        homeDir: File,
        args: List<String>,
        stdout: ByteArrayOutputStream,
        stderr: ByteArrayOutputStream,
    ) {
        spec.executable = executable.absolutePath
        spec.args(listOf("--no-metrics") + args)
        // b/582756527: Point ANDROID_USER_HOME to a writable directory under `out/` so bundle
        // extraction and lock files do not dirty the `prebuilts/` git checkout, and so PackManager
        // finds staged packs under `$ANDROID_USER_HOME/cli/packs/<version>/`.
        spec.environment("ANDROID_USER_HOME", homeDir.absolutePath)
        // TODO(b/581945721): Temporary workaround to prevent the CLI's Updater from making network
        // requests to dl.google.com or printing an update banner to stdout. Remove once Android CLI
        // is updated with the fix.
        spec.environment("BAZEL_TEST", "1")
        spec.standardOutput = stdout
        spec.errorOutput = stderr
        spec.isIgnoreExitValue = true
    }

    /**
     * Stages `<prebuilts>/packs/<packName>/<packName>.jar` into
     * `$ANDROID_USER_HOME/cli/packs/<version>/<packName>/<packName>.jar` under `out/`.
     *
     * Because `ANDROID_USER_HOME` must be a writable directory in `out/` (to avoid dirtying the
     * `prebuilts/` git checkout when the CLI extracts `cli/bundles/` and creates `cli/packs/lock`),
     * `out/android-cli-home` starts empty on a clean build. Meanwhile, `PackManager` hardcodes
     * resolving packs from `$ANDROID_USER_HOME/cli/packs/<version>/` rather than relative to the
     * CLI binary in `prebuilts/`.
     *
     * TODO(b/582756527): Remove this staging copy once Android CLI supports loading packs directly
     *   from the read-only `prebuilts/` directory independently of `ANDROID_USER_HOME`.
     */
    private fun ensurePackStaged(env: PreparedCliEnvironment, pack: AndroidCliPack) {
        if (pack in stagedPacks) return
        synchronized(packLock) {
            if (pack in stagedPacks) return
            val packName = pack.packName
            val prebuilts = parameters.prebuiltsDir.get().asFile
            val srcPack = prebuilts.resolve("packs/$packName/$packName.jar")
            if (!srcPack.exists()) {
                throw GradleException("Missing $packName pack at ${srcPack.absolutePath}")
            }

            val destPackDir =
                env.cliDir.resolve("packs/${env.version}/$packName").apply { mkdirs() }
            val destPack = destPackDir.resolve("$packName.jar")
            if (!destPack.exists() || destPack.length() != srcPack.length()) {
                val tempPack =
                    destPackDir.resolve(
                        ".$packName.jar.${ProcessHandle.current().pid()}.${System.nanoTime()}.tmp"
                    )
                try {
                    Files.copy(
                        srcPack.toPath(),
                        tempPack.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                    try {
                        Files.move(
                            tempPack.toPath(),
                            destPack.toPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    } catch (_: Exception) {
                        Files.move(
                            tempPack.toPath(),
                            destPack.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
                } finally {
                    Files.deleteIfExists(tempPack.toPath())
                }
            }
            stagedPacks.add(pack)
        }
    }

    private fun prepareCliEnvironment(): PreparedCliEnvironment {
        val prebuilts = parameters.prebuiltsDir.get().asFile
        // b/582756096: Each OS/ABI currently uses a separate standalone binary embedding main.jar.
        val binaryName =
            when (getOperatingSystem()) {
                OperatingSystem.LINUX -> "android_linux_x86_64"
                OperatingSystem.MAC -> "android_darwin_arm64"
                else ->
                    throw GradleException(
                        "Unsupported operating system for android CLI: ${getOperatingSystem()}"
                    )
            }
        val binaryFile = prebuilts.resolve(binaryName)
        if (!binaryFile.exists()) {
            throw GradleException(
                "Android CLI binary not found at ${binaryFile.absolutePath}. " +
                    "Ensure prebuilts/androidx/android-cli is synced."
            )
        }
        val versionFile = prebuilts.resolve("VERSION")
        if (!versionFile.exists()) {
            throw GradleException("Missing VERSION file in ${prebuilts.absolutePath}")
        }
        val version = versionFile.readText().trim()

        val homeDir = parameters.cliHomeDir.get().asFile
        val cliDir = homeDir.resolve("cli").apply { mkdirs() }
        // TODO(b/581945721): Temporary workaround to suppress network update checks
        // (`UpdateScheduler`
        // parses `last_update_check` as epoch seconds and skips checking when within 24 hours).
        // Remove once Android CLI is updated with the fix.
        cliDir
            .resolve("last_update_check")
            .writeText((System.currentTimeMillis() / 1000L).toString())

        val initializedMarker = cliDir.resolve("initialized_version")
        val bundlesDir = cliDir.resolve("bundles")
        val alreadyExtracted =
            initializedMarker.exists() &&
                initializedMarker.readText().trim() == version &&
                bundlesDir.exists() &&
                !bundlesDir.listFiles().isNullOrEmpty()

        if (!alreadyExtracted) {
            val out = ByteArrayOutputStream()
            val err = ByteArrayOutputStream()
            val result = execOperations.exec { spec ->
                configureCliExecSpec(
                    spec = spec,
                    executable = binaryFile,
                    homeDir = homeDir,
                    args = listOf("--version"),
                    stdout = out,
                    stderr = err,
                )
            }
            if (result.exitValue != 0) {
                throw GradleException(
                    "Failed to initialize android CLI (exit ${result.exitValue}):\n" +
                        err.toString(Charsets.UTF_8) +
                        "\n" +
                        out.toString(Charsets.UTF_8)
                )
            }
            initializedMarker.writeText(version)
        }

        return PreparedCliEnvironment(
            executable = binaryFile,
            homeDir = homeDir,
            cliDir = cliDir,
            version = version,
        )
    }

    companion object {
        private const val SERVICE_NAME = "androidCliBuildService"

        // Capped at 4 because `android analyze --analysis=optimization` runs D8 and R8
        // compilations that are internally multi-threaded. This limit may need to be revisited
        // (or separated by command type) if AndroidCliBuildService is used for non-analyze CLI
        // invocations.
        private const val MAX_PARALLEL_USAGES = 4

        fun registerOrGet(project: Project): Provider<AndroidCliBuildService> {
            val prebuiltsDir = File(project.getPrebuiltsRoot(), "androidx/android-cli")
            val cliHomeDir = File(project.getOutDirectory(), "android-cli-home")
            return project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                AndroidCliBuildService::class.java,
            ) { spec ->
                spec.parameters.prebuiltsDir.set(prebuiltsDir)
                spec.parameters.cliHomeDir.set(cliHomeDir)
                spec.maxParallelUsages.set(
                    Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_PARALLEL_USAGES)
                )
            }
        }
    }
}
