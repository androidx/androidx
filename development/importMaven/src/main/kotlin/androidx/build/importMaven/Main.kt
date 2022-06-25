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
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.apache.logging.log4j.kotlin.logger
import kotlin.system.exitProcess

internal class Cli : CliktCommand() {
    override fun run() = Unit
}

/**
 * Base class for all commands which only reads the support repo folder.
 */
internal abstract class BaseCommand(
    help: String
) : CliktCommand(help) {
    protected val logger by lazy {
        // make this lazy so that it can be created after root logger config is changed.
        logger("main")
    }
    private val supportRepoFolder by option(
        help = """
            Path to the support repository (frameworks/support).
            By default, it is inherited from the build of import maven itself.
        """.trimIndent(),
        envvar = "SUPPORT_REPO"
    )

    private val verbose by option(
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

    final override fun run() {
        if (verbose) {
            enableVerboseLogs()
        } else {
            enableInfoLogs()
        }
        execute()
    }

    abstract fun execute()
}

/**
 * Base class to import maven artifacts.
 */
internal abstract class BaseImportMavenCommand(
    help: String
) : BaseCommand(help) {
    private val prebuiltsFolder by option(
        help = """
            Path to the prebuilts folder. Can be relative to the current working
            directory.
            By default, inherited from the support-repo root folder.
        """.trimIndent()
    )
    private val androidXBuildId by option(
        names = arrayOf("--androidx-build-id"),
        help = """
            The build id of https://ci.android.com/builds/branches/aosp-androidx-main/grid?
            to use for fetching androidx prebuilts.
        """.trimIndent()
    ).int()
    private val metalavaBuildId by option(
        help = """
            The build id of https://androidx.dev/metalava/builds to fetch metalava from.
        """.trimIndent()
    ).int()
    private val allowJetbrainsDev by option(
        help = """
            Whether or not to allow artifacts to be fetched from Jetbrains' dev repository
            E.g. https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev
        """.trimIndent()
    ).flag()

    private val redownload by option(
        help = """
            If set to true, local repositories will be ignored while resolving artifacts so
            all of them will be redownloaded.
        """.trimIndent()
    ).flag(default = false)

    private val repositories by option(
        help = """
            Comma separated list of additional repositories.
        """.trimIndent()
    ).convert("COMMA SEPARATED URLS") {
        it.split(',')
    }

    private val cleanLocalRepo by option(
        help = """
            This flag tries to remove unnecessary / bad files from the local maven repository.
            It must be used with the `redownload` flag.
            For instance, if we refetch a particular artifact and the download folder has some files
            that are not re-fetched, they'll be deleted.
        """.trimIndent()
    ).flag(default = false)

    private val explicitlyFetchInheritedDependencies by option(
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
        val resolvedArtifacts = ArtifactResolver.resolveArtifacts(
            artifacts = artifacts(),
            additionalRepositories = extraRepositories,
            explicitlyFetchInheritedDependencies = explicitlyFetchInheritedDependencies,
            localRepositories = if (redownload) {
                emptyList()
            } else {
                listOf(
                    "file:///" + downloader.internalFolder.toString(),
                    "file:///" + downloader.externalFolder.toString(),
                )
            },
            downloadObserver = downloader
        )
        if (cleanLocalRepo) {
            downloader.cleanupLocalRepositories()
        }

        val downloadedFiles = downloader.getDownloadedFiles()
        logger.info {
            """
                Import artifact action completed.
                Resolved ${resolvedArtifacts.size} artifacts.
                Downloaded ${downloadedFiles.size} files.
            """.trimIndent()
        }
        if (downloadedFiles.isEmpty()) {
            logger.warn(
                """
                Didn't download any files. It might be either a bug or all files might be available
                in the local prebuilts.

                If you think it is a bug, please re-run the command with `--verbose` and file
                a bug with the output.
                https://issuetracker.google.com/issues/new?component=705292
                """.trimIndent()
            )
        }
        flushLogs()
    }
}

/**
 * Imports the maven artifacts in the [artifacts] parameter.
 */
internal class ImportArtifact : BaseImportMavenCommand(
    help = "Imports given artifacts"
) {
    private val artifacts by option(
        help = """
            The dependency notation of the artifact you want to add to the prebuilts folder.
            E.g. android.arch.work:work-runtime-ktx:1.0.0-alpha07
            Multiple artifacts can be provided with a `,` in between them.
        """.trimIndent()
    ).required()

    override fun artifacts(): List<String> = artifacts.split(',')
}

/**
 * Downloads konan binaries that are needed to compile native targets.
 */
internal class ImportKonanBinariesCommand : BaseCommand(
    help = "Downloads konan binaries"
) {
    private val konanPrebuiltsFolder by option(
        help = """
            Path to the prebuilts folder. Can be relative to the current working
            directory.
            By default, inherited from the support-repo root folder.
        """.trimIndent()
    )
    private val konanCompilerVersion by option(
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
    private val tomlFile by option(
        help = """
            Path to the toml file. If not provided, main androidx toml file is obtained from the
            supportRepoFolder argument.
        """.trimIndent()
    )

    override fun artifacts(): List<String> {
        val file = tomlFile.orFromSupportRepoFolder(
            "gradle/libs.versions.toml"
        )
        return ImportVersionCatalog.load(
            fileSystem = FileSystem.SYSTEM,
            file = file
        )
    }
}

fun main(args: Array<String>) {
    Cli()
        .subcommands(ImportArtifact(), ImportKonanBinariesCommand(), ImportToml())
        .main(args)
    exitProcess(0)
}