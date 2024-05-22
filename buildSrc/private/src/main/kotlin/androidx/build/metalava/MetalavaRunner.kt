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

package androidx.build.metalava

import androidx.build.Version
import androidx.build.checkapi.ApiLocation
import androidx.build.getLibraryByName
import androidx.build.java.JavaCompileInputs
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// MetalavaRunner stores common configuration for executing Metalava

fun runMetalavaWithArgs(
    metalavaClasspath: FileCollection,
    args: List<String>,
    k2UastEnabled: Boolean,
    kotlinSourceLevel: KotlinVersion,
    workerExecutor: WorkerExecutor,
) {
    val allArgs =
        args +
            listOf(
                "--hide",
                // Removing final from a method does not cause compatibility issues for AndroidX.
                "RemovedFinalStrict",
                "--error",
                "UnresolvedImport",
                "--kotlin-source",
                kotlinSourceLevel.version,

                // Metalava arguments to suppress compatibility checks for experimental API
                // surfaces.
                "--suppress-compatibility-meta-annotation",
                "androidx.annotation.RequiresOptIn",
                "--suppress-compatibility-meta-annotation",
                "kotlin.RequiresOptIn",

                // Skip reading comments in Metalava for two reasons:
                // - We prefer for developers to specify api information via annotations instead
                //   of just javadoc comments (like @hide)
                // - This allows us to improve cacheability of Metalava tasks
                "--ignore-comments",
                "--hide",
                "DeprecationMismatch",
                "--hide",
                "DocumentExceptions",

                // Don't track annotations that aren't needed for review or checking compat.
                "--exclude-annotation",
                "androidx.annotation.ReplaceWith",
            )
    val workQueue = workerExecutor.processIsolation()
    workQueue.submit(MetalavaWorkAction::class.java) { parameters ->
        parameters.args.set(allArgs)
        parameters.metalavaClasspath.set(metalavaClasspath.files)
        parameters.k2UastEnabled.set(k2UastEnabled)
    }
}

interface MetalavaParams : WorkParameters {
    val args: ListProperty<String>
    val metalavaClasspath: SetProperty<File>
    val k2UastEnabled: Property<Boolean>
}

abstract class MetalavaWorkAction @Inject constructor(private val execOperations: ExecOperations) :
    WorkAction<MetalavaParams> {
    override fun execute() {
        val outputStream = ByteArrayOutputStream()
        var successful = false
        val k2UastArg =
            listOfNotNull(
                // Enable Android Lint infrastructure used by Metalava to use K2 UAST
                // (also historically known as FIR) when running Metalava for this module.
                "--Xuse-k2-uast".takeIf { parameters.k2UastEnabled.get() }
            )
        try {
            execOperations.javaexec {
                // Intellij core reflects into java.util.ResourceBundle
                it.jvmArgs = listOf("--add-opens", "java.base/java.util=ALL-UNNAMED")
                it.systemProperty("java.awt.headless", "true")
                it.classpath(parameters.metalavaClasspath.get())
                it.mainClass.set("com.android.tools.metalava.Driver")
                it.args = parameters.args.get() + k2UastArg
                it.setStandardOutput(outputStream)
                it.setErrorOutput(outputStream)
            }
            successful = true
        } finally {
            if (!successful) {
                System.err.println(outputStream.toString(Charsets.UTF_8))
            }
        }
    }
}

fun Project.getMetalavaClasspath(): FileCollection {
    val configuration =
        configurations.detachedConfiguration(dependencies.create(getLibraryByName("metalava")))
    return project.files(configuration)
}

fun getApiLintArgs(targetsJavaConsumers: Boolean): List<String> {
    val args =
        mutableListOf(
            "--api-lint",
            "--hide",
            listOf(
                    // The list of checks that are hidden as they are not useful in androidx
                    "Enum", // Enums are allowed to be use in androidx
                    "CallbackInterface", // With target Java 8, we have default methods
                    "ProtectedMember", // We allow using protected members in androidx
                    "ManagerLookup", // Managers in androidx are not the same as platform services
                    "ManagerConstructor",
                    "RethrowRemoteException", // This check is for calls into system_server
                    "PackageLayering", // This check is not relevant to androidx.* code.
                    "UserHandle", // This check is not relevant to androidx.* code.
                    "ParcelableList", // This check is only relevant to android platform that has
                    // managers.

                    // List of checks that have bugs, but should be enabled once fixed.
                    "StaticUtils", // b/135489083
                    "StartWithLower", // b/135710527

                    // The list of checks that are API lint warnings and are yet to be enabled
                    "SamShouldBeLast",

                    // We should only treat these as warnings
                    "IntentBuilderName",
                    "OnNameExpected",
                    "UserHandleName"
                )
                .joinToString(),
            "--error",
            listOf(
                    "AllUpper",
                    "GetterSetterNames",
                    "MinMaxConstant",
                    "TopLevelBuilder",
                    "BuilderSetStyle",
                    "MissingBuildMethod",
                    "SetterReturnsThis",
                    "OverlappingConstants",
                    "IllegalStateException",
                    "ListenerLast",
                    "ExecutorRegistration",
                    "StreamFiles",
                    "AbstractInner",
                    "NotCloseable",
                    "MethodNameTense",
                    "UseIcu",
                    "NoByteOrShort",
                    "GetterOnBuilder",
                    "CallbackMethodName",
                    "StaticFinalBuilder",
                    "MissingGetterMatchingBuilder",
                    "HiddenSuperclass",
                    "KotlinOperator"
                )
                .joinToString()
        )
    if (targetsJavaConsumers) {
        args.addAll(listOf("--error", "MissingJvmstatic", "--error", "ArrayReturn"))
    } else {
        args.addAll(listOf("--hide", "MissingJvmstatic", "--hide", "ArrayReturn"))
    }
    return args
}

/** Returns the args needed to generate a version history JSON from the previous API files. */
internal fun getGenerateApiLevelsArgs(
    apiFiles: List<File>,
    currentVersion: Version,
    outputLocation: File
): List<String> {
    val versions = getVersionsForApiLevels(apiFiles) + currentVersion

    val args =
        listOf(
            "--generate-api-version-history",
            outputLocation.absolutePath,
            "--api-version-names",
            versions.joinToString(" ")
        )

    return if (apiFiles.isEmpty()) {
        args
    } else {
        args + listOf("--api-version-signature-files", apiFiles.joinToString(":"))
    }
}

sealed class GenerateApiMode {
    object PublicApi : GenerateApiMode()

    object AllRestrictedApis : GenerateApiMode()

    object RestrictToLibraryGroupPrefixApis : GenerateApiMode()
}

sealed class ApiLintMode {
    class CheckBaseline(val apiLintBaseline: File, val targetsJavaConsumers: Boolean) :
        ApiLintMode()

    object Skip : ApiLintMode()
}

/**
 * Generates all of the specified api files, as well as a version history JSON for the public API.
 */
fun generateApi(
    metalavaClasspath: FileCollection,
    files: JavaCompileInputs,
    apiLocation: ApiLocation,
    apiLintMode: ApiLintMode,
    includeRestrictToLibraryGroupApis: Boolean,
    apiLevelsArgs: List<String>,
    k2UastEnabled: Boolean,
    kotlinSourceLevel: KotlinVersion,
    workerExecutor: WorkerExecutor,
    pathToManifest: String? = null,
) {
    val generateApiConfigs: MutableList<Pair<GenerateApiMode, ApiLintMode>> =
        mutableListOf(GenerateApiMode.PublicApi to apiLintMode)

    @Suppress("LiftReturnOrAssignment")
    if (includeRestrictToLibraryGroupApis) {
        generateApiConfigs += GenerateApiMode.AllRestrictedApis to ApiLintMode.Skip
    } else {
        generateApiConfigs += GenerateApiMode.RestrictToLibraryGroupPrefixApis to ApiLintMode.Skip
    }

    generateApiConfigs.forEach { (generateApiMode, apiLintMode) ->
        generateApi(
            metalavaClasspath,
            files,
            apiLocation,
            generateApiMode,
            apiLintMode,
            apiLevelsArgs,
            k2UastEnabled,
            kotlinSourceLevel,
            workerExecutor,
            pathToManifest
        )
    }
}

/**
 * Gets arguments for generating the specified api file (and a version history JSON if the
 * [generateApiMode] is [GenerateApiMode.PublicApi].
 */
private fun generateApi(
    metalavaClasspath: FileCollection,
    files: JavaCompileInputs,
    outputLocation: ApiLocation,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode,
    apiLevelsArgs: List<String>,
    k2UastEnabled: Boolean,
    kotlinSourceLevel: KotlinVersion,
    workerExecutor: WorkerExecutor,
    pathToManifest: String? = null
) {
    val args =
        getGenerateApiArgs(
            files.bootClasspath,
            files.dependencyClasspath,
            files.sourcePaths.files,
            files.commonModuleSourcePaths.files,
            outputLocation,
            generateApiMode,
            apiLintMode,
            apiLevelsArgs,
            pathToManifest
        )
    runMetalavaWithArgs(metalavaClasspath, args, k2UastEnabled, kotlinSourceLevel, workerExecutor)
}

/**
 * Generates the specified api file, and a version history JSON if the [generateApiMode] is
 * [GenerateApiMode.PublicApi].
 */
fun getGenerateApiArgs(
    bootClasspath: FileCollection,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    commonModuleSourcePaths: Collection<File>,
    outputLocation: ApiLocation?,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode,
    apiLevelsArgs: List<String>,
    pathToManifest: String? = null
): List<String> {
    // generate public API txt
    val args =
        mutableListOf(
            "--classpath",
            (bootClasspath.files + dependencyClasspath.files).joinToString(File.pathSeparator),
            "--source-path",
            sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),
        )

    val existentCommonModuleSourcePaths = commonModuleSourcePaths.filter { it.exists() }
    if (existentCommonModuleSourcePaths.isNotEmpty()) {
        args += listOf("--common-source-path", existentCommonModuleSourcePaths.joinToString(":"))
    }
    args += listOf("--format=v4", "--warnings-as-errors")

    pathToManifest?.let { args += listOf("--manifest", pathToManifest) }

    if (outputLocation != null) {
        when (generateApiMode) {
            is GenerateApiMode.PublicApi -> {
                args += listOf("--api", outputLocation.publicApiFile.toString())
                // Generate API levels just for the public API
                args += apiLevelsArgs
            }
            is GenerateApiMode.AllRestrictedApis,
            GenerateApiMode.RestrictToLibraryGroupPrefixApis -> {
                args += listOf("--api", outputLocation.restrictedApiFile.toString())
            }
        }
    }

    when (generateApiMode) {
        is GenerateApiMode.PublicApi -> {
            args += listOf("--hide-annotation", "androidx.annotation.RestrictTo")
            args += listOf("--show-unannotated")
        }
        is GenerateApiMode.AllRestrictedApis,
        GenerateApiMode.RestrictToLibraryGroupPrefixApis -> {
            // Despite being hidden we still track the following:
            // * @RestrictTo(Scope.LIBRARY_GROUP_PREFIX): inter-library APIs
            // * @PublishedApi: needs binary stability for inline methods
            // * @RestrictTo(Scope.LIBRARY_GROUP): APIs between libraries in non-atomic groups
            args +=
                listOf(
                    // hide RestrictTo(LIBRARY), use --show-annotation for RestrictTo with
                    // specific arguments
                    "--hide-annotation",
                    "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)",
                    "--show-annotation",
                    "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                        "LIBRARY_GROUP_PREFIX)",
                    "--show-annotation",
                    "kotlin.PublishedApi",
                    "--show-unannotated"
                )
            if (generateApiMode is GenerateApiMode.AllRestrictedApis) {
                args +=
                    listOf(
                        "--show-annotation",
                        "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                            "LIBRARY_GROUP)"
                    )
            } else {
                args +=
                    listOf(
                        "--hide-annotation",
                        "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                            "LIBRARY_GROUP)"
                    )
            }
        }
    }

    when (apiLintMode) {
        is ApiLintMode.CheckBaseline -> {
            args += getApiLintArgs(apiLintMode.targetsJavaConsumers)
            if (apiLintMode.apiLintBaseline.exists()) {
                args += listOf("--baseline", apiLintMode.apiLintBaseline.toString())
            }
            args.addAll(
                listOf(
                    "--error",
                    "ReferencesDeprecated",
                    "--error-message:api-lint",
                    """
    ${TERMINAL_RED}Your change has API lint issues. Fix the code according to the messages above.$TERMINAL_RESET

    If a check is broken, suppress it in code in Kotlin with @Suppress("id")/@get:Suppress("id")
    and in Java with @SuppressWarnings("id") and file bug to
    https://issuetracker.google.com/issues/new?component=739152&template=1344623

    If you are doing a refactoring or suppression above does not work, use ./gradlew updateApiLintBaseline
"""
                )
            )
        }
        is ApiLintMode.Skip -> {
            args.addAll(
                listOf(
                    "--hide",
                    "UnhiddenSystemApi",
                    "--hide",
                    "ReferencesHidden",
                    "--hide",
                    "ReferencesDeprecated"
                )
            )
        }
    }

    return args
}
