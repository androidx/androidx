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

package androidx.build.metalava

import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.SourceSetInputs
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import java.io.File
import kotlin.collections.isNotEmpty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.workers.WorkerExecutor

/** A metalava task that takes source code as input (other tasks take signature files). */
@CacheableTask
internal abstract class SourceMetalavaTask(workerExecutor: WorkerExecutor) :
    MetalavaTask(workerExecutor) {
    /**
     * Specifies both the source files and their corresponding compiled class files
     *
     * We specify the source files to pass to Metalava because that's the format that Metalava
     * needs.
     *
     * However, Metalava is only supposed to read the public API, so we don't need to rerun Metalava
     * if no API changes occurred.
     *
     * Gradle doesn't offer all of the same abilities as Metalava for writing a signature file and
     * validating its compatibility, but Gradle does offer the ability to check whether two sets of
     * classes have the same API.
     *
     * Ideally, we would ask Gradle to rerun this task only if the public API changes, by declaring
     * the compiled classes as inputs rather than the sources. However, annotations with source
     * retention do not appear in compiled classes but do change API file output, so sources are
     * also currently declared as inputs.
     */
    /** Source files against which API signatures will be validated. */
    @get:[InputFiles PathSensitive(PathSensitivity.NONE)]
    abstract val sourcePaths: ConfigurableFileCollection

    /** Class files compiled from sourcePaths */
    @get:Classpath abstract val compiledSources: ConfigurableFileCollection

    @get:[Optional InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val manifestPath: RegularFileProperty

    @get:Internal // already expressed by getApiLintBaseline()
    abstract val baselines: Property<ApiBaselinesLocation>

    @Optional
    @PathSensitive(PathSensitivity.NONE)
    @InputFile
    fun getInputApiLintBaseline(): File? {
        val baseline = baselines.get().apiLintFile
        return if (baseline.exists()) baseline else null
    }

    /**
     * Information about all source sets for multiplatform projects. Non-multiplatform projects
     * should be represented as a list with one source set.
     *
     * This is marked as [Internal] because [compiledSources] is what should determine whether to
     * rerun metalava.
     */
    @get:Internal abstract val sourceSets: ListProperty<SourceSetInputs>

    /** Whether metalava should process the project as multiplatform. */
    @get:Input abstract val multiplatform: Property<Boolean>

    /**
     * Whether the project has a jvm or android compilation. This is always true for non-KMP
     * projects, but can be false for KMP projects.
     */
    @get:Input abstract val hasJvmOrAndroidTarget: Property<Boolean>

    /**
     * Creates an XML file representing the project structure.
     *
     * This should only be called during task execution.
     */
    protected fun createProjectXmlFile(sourceSets: List<SourceSetInputs>): File {
        check(sourceSets.isNotEmpty()) { "Project must have at least one source set." }
        val outputFile = File(temporaryDir, "project.xml")
        ProjectXml.create(
            sourceSets,
            bootClasspath.files,
            compiledSources.singleOrNull(),
            outputFile,
        )
        return outputFile
    }

    protected fun getApiLintArgs(targetsJavaConsumers: Boolean): List<String> {
        val args =
            mutableListOf(
                "--api-lint",
                "--hide",
                listOf(
                        // The list of checks that are hidden as they are not useful in androidx
                        "Enum", // Enums are allowed to be use in androidx
                        "CallbackInterface", // With target Java 8, we have default methods
                        "ProtectedMember", // We allow using protected members in androidx
                        "ManagerLookup", // Managers in androidx are not the same as platform
                        // services
                        "ManagerConstructor",
                        "RethrowRemoteException", // This check is for calls into system_server
                        "PackageLayering", // This check is not relevant to androidx.* code.
                        "UserHandle", // This check is not relevant to androidx.* code.
                        "ParcelableList", // This check is only relevant to android platform that
                        // has
                        // managers.

                        // List of checks that have bugs, but should be enabled once fixed.
                        "StaticUtils", // b/135489083
                        "StartWithLower", // b/135710527

                        // The list of checks that are API lint warnings and are yet to be enabled
                        "SamShouldBeLast",

                        // We should only treat these as warnings
                        "IntentBuilderName",
                        "OnNameExpected",
                        "UserHandleName",
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
                        "KotlinOperator",
                        "DataClassDefinition",
                        "TypeParameterName",
                        "HiddenAbstractMethodInInterface",
                        "HidingApiMethodOverride",
                    )
                    .joinToString(),
            )
        // Acronyms that can be used in their all-caps form. "SQ" is included to allow "SQLite".
        val allowedAcronyms = listOf("SQL", "SQ", "URL", "EGL", "GL", "KHR")
        for (acronym in allowedAcronyms) {
            args.add("--api-lint-allowed-acronym")
            args.add(acronym)
        }
        val javaOnlyIssues =
            listOf(
                "MissingJvmstatic",
                "ArrayReturn",
                "ValueClassDefinition",
                "FacadeClassJvmName",
                "ValueClassUsageFromConstructor",
                "ValueClassUsageWithoutJvmName",
            )
        val javaOnlyErrorLevel =
            if (targetsJavaConsumers) {
                "--error"
            } else {
                "--hide"
            }
        args.add(javaOnlyErrorLevel)
        args.add(javaOnlyIssues.joinToString())
        return args
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
     * Generates the specified api file, and a version history JSON if the [generateApiMode] is
     * [GenerateApiMode.PublicApi].
     */
    protected fun getGenerateApiArgs(
        projectXml: File,
        sourcePaths: Collection<File>,
        compiledSources: File?,
        outputLocation: ApiLocation?,
        generateApiMode: GenerateApiMode,
        apiLintMode: ApiLintMode,
        apiLevelsArgs: List<String>,
        pathToManifest: String? = null,
        multiplatform: Boolean,
        hasJvmOrAndroidTarget: Boolean,
    ): List<String> {
        val args =
            mutableListOf("--project", projectXml.path, "--format=4.0", "--warnings-as-errors")

        // Generate public API txt if there is a jvm/android target. If there isn't, the
        // `generateApi`
        // task will just run API lint without creating a signature file.
        if (hasJvmOrAndroidTarget) {
            args +=
                listOf(
                    "--source-path",
                    sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),
                )

            // Include the jar file to generate bytecode-only APIs if this project has any Kotlin
            // source.
            if (compiledSources != null && sourcePaths.any { containsKotlinFiles(it) }) {
                args += listOf("--compiled-sources", compiledSources.absolutePath)
            }

            pathToManifest?.let { args += listOf("--manifest", pathToManifest) }

            if (outputLocation != null) {
                when (generateApiMode) {
                    is GenerateApiMode.PublicApi -> {
                        args +=
                            listOf(
                                "--trace-file",
                                ApiLocation.toTraceFilePath(outputLocation.publicApiFile),
                            )
                        args += listOf("--api", outputLocation.publicApiFile.toString())
                        // Generate API levels just for the public API
                        args += apiLevelsArgs
                    }

                    is GenerateApiMode.AllRestrictedApis,
                    GenerateApiMode.RestrictToLibraryGroupPrefixApis -> {
                        args +=
                            listOf(
                                "--trace-file",
                                ApiLocation.toTraceFilePath(outputLocation.restrictedApiFile),
                            )
                        args += listOf("--api", outputLocation.restrictedApiFile.toString())
                    }
                }
            }
        } else {
            // If there is no jvm/android target, generate multiplatform API files instead.
            if (outputLocation != null) {
                args +=
                    listOf(
                        "--trace-file",
                        ApiLocation.toTraceFilePath(outputLocation.multiplatformApiDirectory),
                    )
                args +=
                    listOf(
                        "--multiplatform-api-directory",
                        outputLocation.multiplatformApiDirectory.toString(),
                    )
            }
        }

        val apiSurfaceName =
            when (generateApiMode) {
                is GenerateApiMode.PublicApi -> "public"
                is GenerateApiMode.RestrictToLibraryGroupPrefixApis -> "restricted-non-atomic-group"
                is GenerateApiMode.AllRestrictedApis -> "restricted-atomic-group"
            }
        args += listOf("--api-surface", apiSurfaceName)

        if (generateApiMode is GenerateApiMode.PublicApi && multiplatform) {
            args += "--multiplatform-enabled"
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
""",
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
                        "ReferencesDeprecated",
                        "--hide",
                        "HiddenSuperclass",
                        "--hide",
                        "HiddenAbstractMethod",
                        "--hide",
                        "HiddenTypeParameter",
                        "--hide",
                        "UnavailableSymbol",
                    )
                )
            }
        }

        args += getConfigFileArgs()
        args +=
            listOf(
                "--kotlin-source",
                kotlinSourceLevel.get().version,
                // Skip reading comments in Metalava for two reasons:
                // - We prefer for developers to specify api information via annotations instead
                //   of just javadoc comments (like @hide)
                // - This allows us to improve cacheability of Metalava tasks
                "--ignore-comments",
            )
        args += commonIssueArgs
        args += excludeAnnotationArgs
        args += suppressCompatibilityAnnotationArgs

        return args
    }

    /** Whether the [file] is a kotlin file or is a directory containing one (recursively). */
    private fun containsKotlinFiles(file: File): Boolean {
        return if (file.isDirectory) {
            file.listFiles().any { containsKotlinFiles(it) }
        } else {
            file.extension == "kt"
        }
    }

    companion object {
        private val commonIssueArgs =
            listOf(
                "--warning",
                "UnresolvedImport",
                // This issue is important for stubs generation, which we don't do here.
                "--hide",
                "InheritChangesSignature",
            )

        private val excludeAnnotationArgs =
            listOf(
                // Don't track annotations that aren't needed for review or checking compat.
                "--exclude-annotation",
                "androidx.annotation.ReplaceWith",
                "--exclude-annotation",
                "androidx.compose.runtime.ComposableInferredTarget",
                "--exclude-annotation",
                "androidx.compose.runtime.ComposableTarget",
                // internal annotation, includes debug information and values are not constant
                "--exclude-annotation",
                "androidx.compose.runtime.internal.FunctionKeyMeta",
            )
    }
}
