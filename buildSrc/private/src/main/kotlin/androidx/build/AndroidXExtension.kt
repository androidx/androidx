/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import androidx.build.checkapi.shouldConfigureApiTasks
import androidx.build.gitclient.getHeadShaProvider
import androidx.build.transform.configureAarAsJarForConfiguration
import groovy.lang.Closure
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Extension for [AndroidXImplPlugin] that's responsible for holding configuration options. */
abstract class AndroidXExtension(val project: Project) : ExtensionAware, AndroidXConfiguration {

    @JvmField val LibraryVersions: Map<String, Version>

    @JvmField val AllLibraryGroups: List<LibraryGroup>

    val libraryGroupsByGroupId: Map<String, LibraryGroup>
    val overrideLibraryGroupsByProjectPath: Map<String, LibraryGroup>

    var copySampleSourceJarsTask: TaskProvider<LazyInputsCopyTask>? = null

    val mavenGroup: LibraryGroup?

    val listProjectsService: Provider<ListProjectsService>

    private val versionService: LibraryVersionsService

    val deviceTests = DeviceTests.register(project.extensions)

    init {
        val tomlFileName = "libraryversions.toml"
        val toml = lazyReadFile(tomlFileName)

        // These parameters are used when building pre-release binaries for androidxdev.
        // These parameters are only expected to be compatible with :compose:compiler:compiler .
        // To use them may require specifying specific projects and disabling some checks
        // like this:
        // `./gradlew :compose:compiler:compiler:publishToMavenLocal
        // -Pandroidx.versionExtraCheckEnabled=false`
        val composeCustomVersion = project.providers.environmentVariable("COMPOSE_CUSTOM_VERSION")
        val composeCustomGroup = project.providers.environmentVariable("COMPOSE_CUSTOM_GROUP")
        // service that can compute group/version for a project
        versionService =
            project.gradle.sharedServices
                .registerIfAbsent("libraryVersionsService", LibraryVersionsService::class.java) {
                    spec ->
                    spec.parameters.tomlFileName = tomlFileName
                    spec.parameters.tomlFileContents = toml
                    spec.parameters.composeCustomVersion = composeCustomVersion
                    spec.parameters.composeCustomGroup = composeCustomGroup
                }
                .get()
        AllLibraryGroups = versionService.libraryGroups.values.toList()
        LibraryVersions = versionService.libraryVersions
        libraryGroupsByGroupId = versionService.libraryGroupsByGroupId
        overrideLibraryGroupsByProjectPath = versionService.overrideLibraryGroupsByProjectPath

        // Always set a known default based on project path. see: b/302183954
        setDefaultGroupFromProjectPath()
        mavenGroup = chooseLibraryGroup()
        chooseProjectVersion()

        // service that can compute full list of projects in settings.gradle
        val settings = lazyReadFile("settings.gradle")
        listProjectsService =
            project.gradle.sharedServices.registerIfAbsent(
                "listProjectsService",
                ListProjectsService::class.java
            ) { spec ->
                spec.parameters.settingsFile = settings
            }

        kotlinTarget.set(KotlinTarget.DEFAULT)
        kotlinTestTarget.set(kotlinTarget)
    }

    var name: Property<String?> = project.objects.property(String::class.java)

    fun setName(newName: String) {
        name.set(newName)
    }

    /**
     * Maven version of the library.
     *
     * Note that, setting this is an error if the library group sets an atomic version.
     */
    var mavenVersion: Version? = null
        set(value) {
            field = value
            chooseProjectVersion()
        }

    internal var projectDirectlySpecifiesMavenVersion: Boolean = false

    fun getOtherProjectsInSameGroup(): List<SettingsParser.IncludedProject> {
        val allProjects = listProjectsService.get().allPossibleProjects
        val ourGroup = chooseLibraryGroup()
        if (ourGroup == null) return listOf()
        val otherProjectsInSameGroup =
            allProjects.filter { otherProject ->
                if (otherProject.gradlePath == project.path) {
                    false
                } else {
                    getLibraryGroupFromProjectPath(otherProject.gradlePath) == ourGroup
                }
            }
        return otherProjectsInSameGroup
    }

    /** Returns a string explaining the value of mavenGroup */
    fun explainMavenGroup(): List<String> {
        val explanationBuilder = mutableListOf<String>()
        chooseLibraryGroup(explanationBuilder)
        return explanationBuilder
    }

    private fun lazyReadFile(fileName: String): Provider<String> {
        val fileProperty =
            project.objects.fileProperty().fileValue(File(project.getSupportRootFolder(), fileName))
        return project.providers.fileContents(fileProperty).asText
    }

    private fun chooseLibraryGroup(explanationBuilder: MutableList<String>? = null): LibraryGroup? {
        return getLibraryGroupFromProjectPath(project.path, explanationBuilder)
    }

    private fun substringBeforeLastColon(projectPath: String): String {
        val lastColonIndex = projectPath.lastIndexOf(":")
        return projectPath.substring(0, lastColonIndex)
    }

    // gets the library group from the project path, including special cases
    private fun getLibraryGroupFromProjectPath(
        projectPath: String,
        explanationBuilder: MutableList<String>? = null
    ): LibraryGroup? {
        val overridden = overrideLibraryGroupsByProjectPath.get(projectPath)
        explanationBuilder?.add(
            "Library group (in libraryversions.toml) having" +
                " overrideInclude=[\"$projectPath\"] is $overridden"
        )
        if (overridden != null) return overridden

        val result = getStandardLibraryGroupFromProjectPath(projectPath, explanationBuilder)
        if (result != null) return result

        // samples are allowed to be nested deeper
        if (projectPath.contains("samples")) {
            val parentPath = substringBeforeLastColon(projectPath)
            return getLibraryGroupFromProjectPath(parentPath, explanationBuilder)
        }
        return null
    }

    // simple function to get the library group from the project path, without special cases
    private fun getStandardLibraryGroupFromProjectPath(
        projectPath: String,
        explanationBuilder: MutableList<String>?
    ): LibraryGroup? {
        // Get the text of the library group, something like "androidx.core"
        val parentPath = substringBeforeLastColon(projectPath)

        if (parentPath == "") {
            explanationBuilder?.add("Parent path for $projectPath is empty")
            return null
        }
        // convert parent project path to groupId
        val groupIdText =
            if (projectPath.startsWith(":external")) {
                projectPath.replace(":external:", "")
            } else {
                "androidx.${parentPath.substring(1).replace(':', '.')}"
            }

        // get the library group having that text
        val result = libraryGroupsByGroupId[groupIdText]
        explanationBuilder?.add(
            "Library group (in libraryversions.toml) having group=\"$groupIdText\" is $result"
        )
        return result
    }

    /**
     * Sets a group for the project based on its path.
     * This ensures we always use a known value for the project group instead of what Gradle assigns
     * by default. Furthermore, it also helps make them consistent between the main build and
     * the playground builds.
     */
    private fun setDefaultGroupFromProjectPath() {
        project.group = project.path
            .split(":")
            .filter {
                it.isNotEmpty()
            }.dropLast(1)
            .joinToString(separator = ".", prefix = "androidx.")
    }

    private fun chooseProjectVersion() {
        val version: Version
        val group: String? = mavenGroup?.group
        val groupVersion: Version? = mavenGroup?.atomicGroupVersion
        val mavenVersion: Version? = mavenVersion
        if (mavenVersion != null) {
            projectDirectlySpecifiesMavenVersion = true
            if (groupVersion != null && !isGroupVersionOverrideAllowed()) {
                throw GradleException(
                    "Cannot set mavenVersion (" +
                        mavenVersion +
                        ") for a project (" +
                        project +
                        ") whose mavenGroup already specifies forcedVersion (" +
                        groupVersion +
                        ")"
                )
            } else {
                verifyVersionExtraFormat(mavenVersion)
                version = mavenVersion
            }
        } else {
            projectDirectlySpecifiesMavenVersion = false
            if (groupVersion != null) {
                verifyVersionExtraFormat(groupVersion)
                version = groupVersion
            } else {
                return
            }
        }
        if (group != null) {
            project.group = group
        }
        project.version = if (isSnapshotBuild()) version.copy(extra = "-SNAPSHOT") else version
        versionIsSet = true
    }

    private fun verifyVersionExtraFormat(version: Version) {
        val ALLOWED_EXTRA_PREFIXES = listOf("-alpha", "-beta", "-rc", "-dev", "-SNAPSHOT")
        val extra = version.extra
        if (extra != null) {
            if (!version.isSnapshot() && project.isVersionExtraCheckEnabled()) {
                if (ALLOWED_EXTRA_PREFIXES.any { extra.startsWith(it) }) {
                    for (potentialPrefix in ALLOWED_EXTRA_PREFIXES) {
                        if (extra.startsWith(potentialPrefix)) {
                            val secondExtraPart = extra.removePrefix(potentialPrefix)
                            if (secondExtraPart.toIntOrNull() == null) {
                                throw IllegalArgumentException(
                                    "Version $version is not" +
                                        " a properly formatted version, please ensure that " +
                                        "$potentialPrefix is followed by a number only"
                                )
                            }
                        }
                    }
                } else {
                    throw IllegalArgumentException(
                        "Version $version is not a proper " +
                            "version, version suffixes following major.minor.patch should " +
                            "be one of ${ALLOWED_EXTRA_PREFIXES.joinToString(", ")}"
                    )
                }
            }
        }
    }

    private fun isGroupVersionOverrideAllowed(): Boolean {
        // Grant an exception to the same-version-group policy for artifacts that haven't shipped a
        // stable API surface, e.g. 1.0.0-alphaXX, to allow for rapid early-stage development.
        val version = mavenVersion
        return version != null &&
            version.major == 1 &&
            version.minor == 0 &&
            version.patch == 0 &&
            version.isAlpha()
    }

    private var versionIsSet = false

    fun isVersionSet(): Boolean {
        return versionIsSet
    }

    var description: String? = null
    var inceptionYear: String? = null

    /**
     * targetsJavaConsumers = true, if project is intended to be accessed from Java-language source
     * code.
     */
    var targetsJavaConsumers = true
        get() {
            when (project.path) {
            // add per-project overrides here
            // for example
            // the following project is intended to be accessed from Java
            // ":compose:lint:internal-lint-checks" -> return true
            // the following project is not intended to be accessed from Java
            // ":annotation:annotation" -> return false
            }
            // TODO: rework this to use LibraryType. Fork Library and KolinOnlyLibrary?
            if (project.path.contains("-ktx")) return false
            if (project.path.contains("compose")) return false
            if (project.path.startsWith(":ui")) return false
            if (project.path.startsWith(":text:text")) return false
            return field
        }

    private var licenses: MutableCollection<License> = ArrayList()

    // Should only be used to override LibraryType.publish, if a library isn't ready to publish yet
    var publish: Publish = Publish.UNSET

    internal fun shouldPublish(): Boolean =
        if (publish != Publish.UNSET) {
            publish.shouldPublish()
        } else if (type != LibraryType.UNSET) {
            type.publish.shouldPublish()
        } else {
            false
        }

    internal fun shouldRelease(): Boolean =
        if (publish != Publish.UNSET) {
            publish.shouldRelease()
        } else if (type != LibraryType.UNSET) {
            type.publish.shouldRelease()
        } else {
            false
        }

    internal fun ifReleasing(action: () -> Unit) {
        project.afterEvaluate {
            if (shouldRelease()) {
                action()
            }
        }
    }

    internal fun isPublishConfigured(): Boolean =
        (publish != Publish.UNSET || type.publish != Publish.UNSET)

    fun shouldPublishSbom(): Boolean {
        // IDE plugins are used by and ship inside Studio
        return shouldPublish() || type == LibraryType.IDE_PLUGIN
    }

    /**
     * Whether to run API tasks such as tracking and linting. The default value is
     * [RunApiTasks.Auto], which automatically picks based on the project's properties.
     */
    // TODO: decide whether we want to support overriding runApiTasks
    // @Deprecated("Replaced with AndroidXExtension.type: LibraryType.runApiTasks")
    var runApiTasks: RunApiTasks = RunApiTasks.Auto
        get() = if (field == RunApiTasks.Auto && type != LibraryType.UNSET) type.checkApi else field

    var type: LibraryType = LibraryType.UNSET
    var failOnDeprecationWarnings = true

    var legacyDisableKotlinStrictApiMode = false

    var bypassCoordinateValidation = false

    var metalavaK2UastEnabled = false

    val additionalDeviceTestApkKeys = mutableListOf<String>()

    val additionalDeviceTestTags: MutableList<String> by lazy {
        when {
            project.path.startsWith(":privacysandbox:ads:") ->
                mutableListOf("privacysandbox", "privacysandbox_ads")
            project.path.startsWith(":privacysandbox:") -> mutableListOf("privacysandbox")
            project.path.startsWith(":wear:") -> mutableListOf("wear")
            else -> mutableListOf()
        }
    }

    fun shouldEnforceKotlinStrictApiMode(): Boolean {
        return !legacyDisableKotlinStrictApiMode && shouldConfigureApiTasks()
    }

    fun license(closure: Closure<Any>): License {
        val license = project.configure(License(), closure) as License
        licenses.add(license)
        return license
    }

    fun getLicenses(): Collection<License> {
        return licenses
    }

    fun configureAarAsJarForConfiguration(name: String) {
        configureAarAsJarForConfiguration(project, name)
    }

    fun getReferenceSha(): Provider<String> = getHeadShaProvider(project)

    /**
     * Specify the version for Kotlin API compatibility mode used during Kotlin compilation.
     *
     * Changing this value will force clients to update their Kotlin compiler version, which may be
     * disruptive. Library developers should only change this value if there is a strong reason to
     * upgrade their Kotlin API version ahead of the rest of Jetpack.
     */
    abstract val kotlinTarget: Property<KotlinTarget>

    override val kotlinApiVersion: Provider<KotlinVersion>
        get() = kotlinTarget.map { it.apiVersion }

    override val kotlinBomVersion: Provider<String>
        get() = kotlinTarget.map { project.getVersionByName(it.catalogVersion) }

    /**
     * Specify the version for Kotlin API compatibility mode used during Kotlin compilation of
     * tests.
     */
    abstract val kotlinTestTarget: Property<KotlinTarget>

    override val kotlinTestApiVersion: Provider<KotlinVersion>
        get() = kotlinTestTarget.map { it.apiVersion }

    override val kotlinTestBomVersion: Provider<String>
        get() = kotlinTestTarget.map { project.getVersionByName(it.catalogVersion) }

    /**
     * Whether to validate the androidx configuration block using validateProjectParser. This should
     * always be set to true unless we are temporarily working around a bug.
     */
    var runProjectParser: Boolean = true

    companion object {
        const val DEFAULT_UNSPECIFIED_VERSION = "unspecified"
    }

    /**
     * Used to register a project that will be providing documentation samples for this project.
     * Can only be called once so only one samples library can exist per library b/318840087.
     */
    fun samples(samplesProject: Project) = registerSamplesLibrary(samplesProject)
}

class License {
    var name: String? = null
    var url: String? = null
}

abstract class DeviceTests {

    companion object {
        private const val EXTENSION_NAME = "deviceTests"

        internal fun register(extensions: ExtensionContainer): DeviceTests {
            return extensions.findByType(DeviceTests::class.java)
                ?: extensions.create(EXTENSION_NAME, DeviceTests::class.java)
        }
    }

    var enabled = true
    var targetAppProject: Project? = null
    var targetAppVariant = "debug"

    /**
     * Whether to extract and include APKs from PrivacySandbox SDKs dependencies.
     * TODO (b/309610890): Replace for dependency on AGP artifact.
     */
    var includePrivacySandboxSdks = false
}
