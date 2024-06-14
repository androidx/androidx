/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.importMaven

import androidx.build.importMaven.KmpConfig.SUPPORTED_HOSTS
import androidx.build.importMaven.KmpConfig.SUPPORTED_KONAN_TARGETS
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.FileSystem
import okio.Path
import org.apache.logging.log4j.kotlin.logger
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

typealias KonanProperties = org.jetbrains.kotlin.konan.properties.Properties

/**
 * Utility class to download all konan prebuilts that are required to compile native code. These
 * files are often downloaded into `/prebuilts/androidx/konan`.
 */
class KonanPrebuiltsDownloader(
    private val fileSystem: FileSystem,
    private val downloadPath: Path,
    private val testMode: Boolean = false
) {
    private val logger = logger("KonanPrebuiltsDownloader")
    private val client = OkHttpClient()

    fun download(compilerVersion: String) {
        val project = ProjectService.createProject()
        project.initializeKotlin()

        val compiler = NativeCompilerDownloader(project = project)
        // make sure we have the local compiler downloaded so we can find the konan.properties
        compiler.downloadIfNeeded()
        val distribution = Distribution(compiler.compilerDirectory.canonicalPath)
        // base konan properties file for reference:
        // https://github.com/JetBrains/kotlin/blob/master/kotlin-native/konan/konan.properties
        // Note that [Distribution] might add overrides.
        val compilationDependencies =
            findCompilationDependencies(konanProperties = distribution.properties)
        downloadDistributions(compilationDependencies)
        // for linux x64 we need to provide the up-to-date sysroot, hence, update the config file
        // with the kotlinVersion -> sysroot zip file.
        updateSysrootFile(distribution.properties)
        downloadNativeCompiler(compilerVersion)
    }

    /**
     * NativeCompilerDownloader expects Kotlin plugin to be applied before the download starts. As
     * this class is simulating the same environment, apply the Kotlin plugin manually.
     */
    private fun Project.initializeKotlin() {
        project.pluginManager.apply(KotlinPluginWrapper::class.java)
    }

    private fun downloadNativeCompiler(compilerVersion: String) {
        SUPPORTED_HOSTS.forEach { host ->
            HostManager.simpleOsName()
            val osName =
                when (host.family) {
                    Family.OSX -> "macos"
                    Family.LINUX -> "linux"
                    else -> "unsupported host family: $host"
                }
            val archName =
                when (host.architecture) {
                    Architecture.X64 -> "x86_64"
                    Architecture.ARM64 -> "aarch64"
                    else -> "unsupported architecture: $host"
                }
            val platformName = "$osName-$archName"
            val subPath =
                listOf(
                        "releases",
                        compilerVersion,
                        platformName,
                        "kotlin-native-prebuilt-$platformName-$compilerVersion.tar.gz"
                    )
                    .joinToString("/")
            val url = listOf(NATIVE_COMPILERS_BASE_URL, subPath).joinToString("/")
            downloadIfNecessary(
                url = url,
                localFile = downloadPath / "nativeCompilerPrebuilts" / subPath
            )
        }
    }

    private fun updateSysrootFile(properties: KonanProperties) {
        val sysrootFile = downloadPath / "linux-prebuilts.properties"
        val sysrootProps = Properties()
        if (fileSystem.exists(sysrootFile)) {
            fileSystem.read(sysrootFile) { sysrootProps.load(this.inputStream()) }
        }
        val compilerVersion = properties.requireResolvablePropertyString("compilerVersion")
        val gccLinuxToolchain = properties.requireResolvablePropertyString("gccToolchain.linux_x64")
        sysrootProps.setProperty(compilerVersion, gccLinuxToolchain)
        fileSystem.write(sysrootFile) {
            // write manually to avoid the timestamp
            sysrootProps.forEach { key, value -> this.writeString("$key=$value\n", Charsets.UTF_8) }
        }
    }

    private fun Properties.requireResolvablePropertyString(key: String) =
        checkNotNull(resolvablePropertyString(key)) {
            val allProperties = this.keys.sortedBy { it.toString() }.joinToString("\n")
            "Cannot find required key : $key. Available properties:\n $allProperties"
        }

    /**
     * Finds the compilation dependencies of each supported host machine.
     *
     * There are 3 groups of distributions we need to compile offline:
     * * llvm -> needed for all targets (host based) llvm.<host>.user
     * * ffi -> needed for all targets (host based) llvm.<host>
     * * target specific dependencies (host + target combinations) dependencies.<host>(-<target>)?
     *
     * Technically, a host machine might be capable of building for multiple targets. For instance,
     * macOS can build for windows. To avoid excessive downloads, we only download artifacts that
     * are in the [SUPPORTED_TARGETS] lists.
     */
    private fun findCompilationDependencies(konanProperties: KonanProperties): List<String> {
        val hostDeps =
            SUPPORTED_HOST_NAMES.flatMap { host -> listOf("llvm.$host.user", "libffiDir.$host") }
        val dependencies = buildHostAndTargetKeys("dependencies")
        val gccDeps = SUPPORTED_HOST_NAMES.flatMap { host -> listOf("gccToolchain.$host") }
        val emulatorDependencies = buildHostAndTargetKeys("emulatorDependency")
        return (hostDeps + dependencies + gccDeps + emulatorDependencies)
            .flatMap { konanProperties.resolvablePropertyList(it) }
            .distinct()
    }

    private fun buildHostAndTargetKeys(prefix: String): List<String> {
        return SUPPORTED_HOST_NAMES.flatMap { host ->
            listOf("$prefix.$host") + SUPPORTED_TARGETS.map { target -> "$prefix.$host-$target" }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun downloadDistributions(distributions: List<String>) {
        runBlocking(Dispatchers.IO.limitedParallelism(4)) {
            val results =
                distributions
                    .map { dist ->
                        // since we always build on linux/mac, we use the tar.gz artifacts.
                        // if we ever add support for windows builds, we would need to use zip.
                        val fileName = "$dist.tar.gz"
                        val localFile = downloadPath / fileName
                        async {
                            val url = "$REPO_BASE_URL/$fileName"
                            url to
                                runCatching {
                                    downloadIfNecessary(url = url, localFile = localFile)
                                }
                        }
                    }
                    .awaitAll()
            val failures = results.filter { it.second.isFailure }
            if (failures.isNotEmpty()) {
                error(
                    buildString {
                        appendLine("Couldn't fetch ${failures.size} of ${results.size} artifacts")
                        appendLine("Failed artifacts:")
                        failures.forEach { failure ->
                            appendLine("${failure.first}:")
                            appendLine(failure.second.exceptionOrNull()!!.stackTraceToString())
                            appendLine("----")
                        }
                    }
                )
            }
        }
    }

    /** Downloads a url into [localFile] if it does not already exists. */
    private fun downloadIfNecessary(url: String, localFile: Path) {
        if (fileSystem.exists(localFile)) {
            logger.trace { "${localFile.name} exists, won't re-download" }
        } else {
            logger.trace { "will download $url into $localFile" }
            val tmpFile = localFile.parent!! / "${localFile.name}.tmp"
            if (fileSystem.exists(tmpFile)) {
                fileSystem.delete(tmpFile)
            }
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            try {
                check(response.isSuccessful) { "Failed to fetch $url" }
                fileSystem.createDirectories(localFile.parent!!)
                checkNotNull(response.body?.source()) { "No body while fetching $url" }
                    .use { bodySource ->
                        fileSystem.write(file = tmpFile, mustCreate = true) {
                            if (testMode) {
                                // don't download the whole file for tests
                                this.write(bodySource.readByteArray(10))
                            } else {
                                this.writeAll(bodySource)
                            }
                        }
                    }
                response.body?.close()
                fileSystem.atomicMove(source = tmpFile, target = localFile)
                logger.trace { "Finished downloading $url into $localFile" }
            } finally {
                response.closeQuietly()
            }
        }
    }

    companion object {
        // https://github.com/JetBrains/kotlin/releases/download/v2.0.10-RC/kotlin-native-prebuilt-linux-x86_64-2.0.10-RC.tar.gz
        private const val REPO_BASE_URL = "https://download.jetbrains.com/kotlin/native"
        private const val NATIVE_COMPILERS_BASE_URL = "$REPO_BASE_URL/builds"

        private val SUPPORTED_HOST_NAMES = SUPPORTED_HOSTS.map { it.name }

        // target architectures for which we might build artifacts for.
        private val SUPPORTED_TARGETS =
            SUPPORTED_HOST_NAMES + SUPPORTED_KONAN_TARGETS.map { it.name }
    }
}
