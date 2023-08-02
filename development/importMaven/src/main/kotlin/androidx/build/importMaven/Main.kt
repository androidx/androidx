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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlin.system.exitProcess
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.apache.logging.log4j.kotlin.logger

/**
 * Base class for all commands which only reads the support repo folder.
 */
internal abstract class BaseCommand(
    help: String,
    treatUnknownOptionsAsArgs: Boolean = false,
    invokeWithoutSubcommand: Boolean = false,
) : CliktCommand(
    help = help,
    invokeWithoutSubcommand = invokeWithoutSubcommand,
    treatUnknownOptionsAsArgs = treatUnknownOptionsAsArgs
) {
    private var interceptor: ((Context) -> Unit)? = null
    protected val logger by lazy {
        // make this lazy so that it can be created after root logger config is changed.
        logger("main")
    }
    internal val supportRepoFolder by option(
        help = """
            Path to the support repository (frameworks/support).
            By default, it is inherited from the build of import maven itself.
        """.trimIndent(),
        envvar = "SUPPORT_REPO"
    )

    internal val verbose by option(
        names = arrayOf("-v", "--verbose"),
        help = """
            Enables verbose logging
        """.trimIndent()
    ).flag(
        default = false
    )

    /**
     * Utility method to get the value or infer from the support root folder based on the given
     * [relativePath].
     */
    protected fun String?.orFromSupportRepoFolder(
        relativePath: String,
    ): Path {
        return when {
            this != null -> this.toPath()
            supportRepoFolder != null -> supportRepoFolder!!.toPath() / relativePath
            else -> EnvironmentConfig.supportRoot / relativePath
        }
    }

    /**
     * Disables executing the command, which is useful for testing.
     */
    fun intercept(interceptor: (Context) -> Unit) {
        this.interceptor = interceptor
        registeredSubcommands().forEach {
            (it as BaseCommand).intercept(interceptor)
        }
    }

    final override fun run() {
        if (verbose) {
            enableVerboseLogs()
        } else {
            enableInfoLogs()
        }
        if (interceptor != null) {
            interceptor!!.invoke(currentContext)
        } else {
            execute()
        }
    }

    abstract fun execute()
}

/**
 * Base class to import maven artifacts.
 */
internal abstract class BaseImportMavenCommand(
    invokeWithoutSubcommand: Boolean = false,
    help: String
) : BaseCommand(
    help = help,
    invokeWithoutSubcommand = invokeWithoutSubcommand,
    treatUnknownOptionsAsArgs = true,
) {
    internal val prebuiltsFolder by option(
        help = """
            Path to the prebuilts folder. Can be relative to the current working
            directory.
            By default, inherited from the support-repo root folder.
        """.trimIndent()
    )
    internal val androidXBuildId by option(
        names = arrayOf("--androidx-build-id"),
        help = """
            The build id of https://ci.android.com/builds/branches/aosp-androidx-main/grid?
            to use for fetching androidx prebuilts.
        """.trimIndent()
    ).int()
    internal val metalavaBuildId by option(
        help = """
            The build id of https://androidx.dev/metalava/builds to fetch metalava from.
        """.trimIndent()
    ).int()
    internal val allowJetbrainsDev by option(
        help = """
            Whether or not to allow artifacts to be fetched from Jetbrains' dev repository
            E.g. https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev
        """.trimIndent()
    ).flag()

    internal val redownload by option(
        help = """
            If set to true, local repositories will be ignored while resolving artifacts so
            all of them will be redownloaded.
        """.trimIndent()
    ).flag(default = false)

    internal val repositories by option(
        help = """
            Comma separated list of additional repositories.
        """.trimIndent()
    ).convert("COMMA SEPARATED URLS") {
        it.split(',')
    }

    internal val cleanLocalRepo by option(
        help = """
            This flag tries to remove unnecessary / bad files from the local maven repository.
            It must be used with the `redownload` flag.
            For instance, if we refetch a particular artifact and the download folder has some files
            that are not re-fetched, they'll be deleted.
        """.trimIndent()
    ).flag(default = false)

    internal val explicitlyFetchInheritedDependencies by option(
        help = """
            If set, all inherited dependencies will be fetched individually, with their own
            dependencies.
            For instance, for the given dependency tree:
            artifact1:v1
              artifact2:v2
                artifact3:v1
              artifact3:v3
            When this script is invoked with `artifact1:v1`;
            If this flag is `false`, we'll only fetch artifact1:v1, artifact2:v2, artifact3:v3.
            If this flag is `true`, we'll fetch `artifact3:v1` as well (because artifact2:v2
            declares a dependency on it even though it is overridden by the dependency of
            artifact1:v1
        """.trimIndent()
    ).flag(default = false)

    /**
     * Return the list of artifacts to fetch.
     */
    abstract fun artifacts(): List<String>

    override fun execute() {
        if (currentContext.invokedSubcommand != null) {
            // skip, invoking a sub command instead
            return
        }
        val artifactsToBeResolved = artifacts()
        val extraRepositories = mutableListOf<String>()
        androidXBuildId?.let {
            extraRepositories.add(ArtifactResolver.createAndroidXRepo(it))
        }
        metalavaBuildId?.let {
            extraRepositories.add(ArtifactResolver.createMetalavaRepo(it))
        }
        if (allowJetbrainsDev) {
            extraRepositories.addAll(ArtifactResolver.jetbrainsRepositories)
        }
        if (cleanLocalRepo) {
            check(redownload) {
                """
                    Passing clean repo without passing redownload might break the local repository
                    since some files might've been used during resolution.
                """.trimIndent()
            }
        }
        val downloadFolder = prebuiltsFolder.orFromSupportRepoFolder(
            "../../prebuilts/androidx"
        )
        val downloader = LocalMavenRepoDownloader(
            fileSystem = FileSystem.SYSTEM,
            internalFolder = downloadFolder / "internal",
            externalFolder = downloadFolder / "external"
        )
        repositories?.let {
            extraRepositories.addAll(it)
        }
        val result = ArtifactResolver.resolveArtifacts(
            artifacts = artifactsToBeResolved,
            additionalRepositories = extraRepositories,
            explicitlyFetchInheritedDependencies = explicitlyFetchInheritedDependencies,
            localRepositories = if (redownload) {
                emptyList()
            } else {
                listOf(
                    "file:///" + downloader.internalFolder.normalized().toString(),
                    "file:///" + downloader.externalFolder.normalized().toString(),
                )
            },
            downloadObserver = downloader
        )
        val resolvedArtifacts = result.artifacts
        if (cleanLocalRepo) {
            downloader.cleanupLocalRepositories()
        }

        val downloadedFiles = downloader.getDownloadedFiles()
        logger.info {
            """
                --------------------------------------------------------------------------------
                Resolved ${resolvedArtifacts.size} artifacts.
                Downloaded ${downloadedFiles.size} new files.
                --------------------------------------------------------------------------------
            """.trimIndent()
        }
        updatePlaygroundMetalavaBuildIfNecessary(downloadedFiles)
        if (downloadedFiles.isEmpty()) {
            logger.warn(
                """
                [31mDidn't download any files. It might be either a bug or all files might be
                available in the local prebuilts.

                If you think it is a bug, please re-run the command with `--verbose` and file
                a bug with the output.
                https://issuetracker.google.com/issues/new?component=705292[0m
                """.trimIndent()
            )
        } else {
            if (!result.dependenciesPassedVerification) {
                logger.warn(
                    """
                   [33mOur Gradle build won't trust any artifacts that are unsigned or are signed with new keys. To trust these artifacts, run `development/update-verification-metadata.sh
                   """.trimIndent()
                )
            }
        }
        flushLogs()
    }

    /**
     * GitHub Playground's metalava build id needs to match the build id used by androidx.
     *
     * This method takes care of updating playground.properties if the metalava build id
     * is specified in the import maven script, and we've downloaded metalava.
     */
    private fun updatePlaygroundMetalavaBuildIfNecessary(downloadedFiles: Set<Path>) {
        val metalavaBuild = metalavaBuildId ?: return
        val downloadedMetalava = downloadedFiles.any {
            it.name.contains("metalava")
        }
        if (!downloadedMetalava) {
            return
        }
        val playgroundPropertiesFile = null.orFromSupportRepoFolder(
            "playground-common/playground.properties"
        ).toFile()
        check(playgroundPropertiesFile.exists()) {
            """
                Cannot find playground properties file. This is needed to update metalava in
                playground to match AndroidX.
            """.trimIndent()
        }
        val updatedProperties = playgroundPropertiesFile.readLines(
            Charsets.UTF_8
        ).joinToString("\n") {
            if (it.trim().startsWith("androidx.playground.metalavaBuildId=")) {
                "androidx.playground.metalavaBuildId=$metalavaBuild"
            } else {
                it
            }
        }
        playgroundPropertiesFile.writeText(updatedProperties)
        logger.info { "updated playground properties" }
    }
}

/**
 * Imports the maven artifacts in the [artifacts] parameter.
 */
internal class ImportArtifact : BaseImportMavenCommand(
    help = "Imports given artifacts",
    invokeWithoutSubcommand = true
) {
    private val args by argument(
        help = """
            The dependency notation of the artifact you want to add to the prebuilts folder.
            Can be passed multiple times.
            E.g. android.arch.work:work-runtime-ktx:1.0.0-alpha07
        """.trimIndent()
    ).multiple(
        required = false,
        default = emptyList()
    )
    private val artifacts by option(
        help = """
            The dependency notation of the artifact you want to add to the prebuilts folder.
            E.g. android.arch.work:work-runtime-ktx:1.0.0-alpha07
            Multiple artifacts can be provided with a `,` in between them.
        """.trimIndent()
    ).default("")

    override fun artifacts(): List<String> {
        // artifacts passed via --artifacts
        val optionArtifacts = artifacts.split(',')
        // artficats passed as command line argument
        val argArtifacts = args.flatMap { it.split(',') }
        val artifactsToBeResolved = (optionArtifacts + argArtifacts).distinct()
            .filter {
                it.isNotBlank()
            }
        if (artifactsToBeResolved.isEmpty()) {
            // since we run this command as the default one, we cannot enforce arguments.
            // instead, we check them in first access
            throw UsageError(
                text = """
                        Missing artifact coordinates.
                        You can either pass them as arguments or explicitly via --artifacts option.
                        e.g. ./importMaven.sh foo:bar:baz:123
                             ./importMaven.sh --artifacts foo:bar:baz:123
                        help:
                        ${getFormattedHelp()}
                    """.trimIndent()
            )
        }
        return artifactsToBeResolved
    }
}

/**
 * Downloads konan binaries that are needed to compile native targets.
 */
internal class ImportKonanBinariesCommand : BaseCommand(
    help = "Downloads konan binaries"
) {
    internal val konanPrebuiltsFolder by option(
        help = """
            Path to the prebuilts folder. Can be relative to the current working
            directory.
            By default, inherited from the support-repo root folder.
        """.trimIndent()
    )
    internal val konanCompilerVersion by option(
        help = """
            Konan compiler version to download. This is usually your kotlin version.
        """.trimIndent()
    ).required()

    override fun execute() {
        val downloadFolder = konanPrebuiltsFolder.orFromSupportRepoFolder(
            "../../prebuilts/androidx/konan"
        )
        KonanPrebuiltsDownloader(
            fileSystem = FileSystem.SYSTEM,
            downloadPath = downloadFolder,
            testMode = false
        ).download(
            konanCompilerVersion
        )
    }
}

/**
 * Imports all libraries declared in a toml file.
 */
internal class ImportToml : BaseImportMavenCommand(
    help = "Downloads all artifacts declared in the project's toml file"
) {
    internal val tomlFile by option(
        help = """
            Path to the toml file. If not provided, main androidx toml file is obtained from the
            supportRepoFolder argument.
        """.trimIndent()
    )

    override fun artifacts(): List<String> {
        val file = tomlFile.orFromSupportRepoFolder(
            "gradle/libs.versions.toml"
        )
        return ImportVersionCatalog.load(file)
    }
}

internal fun createCliCommands() = ImportArtifact()
    .subcommands(
        ImportKonanBinariesCommand(), ImportToml()
    )

fun main(args: Array<String>) {
    createCliCommands().main(args)
    exitProcess(0)
}
