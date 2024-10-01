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

import groovy.lang.Closure
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Extension for [AndroidXImplPlugin] that's responsible for holding configuration options. */
abstract class AndroidXExtension(
    val project: Project,
    @Suppress("unused", "PropertyName") // used from build.gradle
    @JvmField
    val LibraryVersions: Map<String, Version>,
    @Suppress("unused") // used from build.gradle
    @JvmField
    val AllLibraryGroups: List<LibraryGroup>,
    private val libraryGroupsByGroupId: Map<String, LibraryGroup>,
    private val overrideLibraryGroupsByProjectPath: Map<String, LibraryGroup>,
    private val allPossibleProjects: Provider<List<IncludedProject>>,
    private val headShaProvider: () -> Provider<String>,
    private val configureAarAsJarForConfigurationDelegate: (String) -> Unit,
) : ExtensionAware, AndroidXConfiguration {
    val mavenGroup: LibraryGroup?
    val deviceTests = DeviceTests.register(project.extensions)

    init {
        // Always set a known default based on project path. see: b/302183954
        setDefaultGroupFromProjectPath()
        mavenGroup = chooseLibraryGroup()
        chooseProjectVersion()
    }

    @get:Inject abstract val buildFeatures: BuildFeatures

    fun isIsolatedProjectsEnabled(): Boolean {
        return buildFeatures.isIsolatedProjectsEnabled()
    }

    /**
     * Map of maven coordinates (e.g. "androidx.core:core") to a Gradle project path (e.g.
     * ":core:core")
     */
    val mavenCoordinatesToProjectPathMap: Map<String, String> by lazy {
        val newProjectMap: MutableMap<String, String> = mutableMapOf()
        allPossibleProjects.get().forEach {
            val group =
                overrideLibraryGroupsByProjectPath[it.gradlePath]
                    ?: getLibraryGroupFromProjectPath(it.gradlePath, null)
            if (group != null) {
                newProjectMap["${group.group}:${substringAfterLastColon(it.gradlePath)}"] =
                    it.gradlePath
            }
        }
        newProjectMap
    }

    val name: Property<String?> = project.objects.property(String::class.java)

    /** The name for this artifact to be used in .pom files. */
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

    var projectDirectlySpecifiesMavenVersion: Boolean = false
        private set

    fun getOtherProjectsInSameGroup(): List<IncludedProject> {
        val ourGroup = chooseLibraryGroup() ?: return listOf()
        val otherProjectsInSameGroup =
            allPossibleProjects.get().filter { otherProject ->
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

    private fun chooseLibraryGroup(explanationBuilder: MutableList<String>? = null): LibraryGroup? {
        return getLibraryGroupFromProjectPath(project.path, explanationBuilder)
    }

    private fun substringBeforeLastColon(projectPath: String): String {
        val lastColonIndex = projectPath.lastIndexOf(":")
        return projectPath.substring(0, lastColonIndex)
    }

    private fun substringAfterLastColon(projectPath: String): String {
        val lastColonIndex = projectPath.lastIndexOf(":")
        return projectPath.substring(lastColonIndex + 1)
    }

    // gets the library group from the project path, including special cases
    private fun getLibraryGroupFromProjectPath(
        projectPath: String,
        explanationBuilder: MutableList<String>? = null
    ): LibraryGroup? {
        val overridden = overrideLibraryGroupsByProjectPath[projectPath]
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
     * Sets a group for the project based on its path. This ensures we always use a known value for
     * the project group instead of what Gradle assigns by default. Furthermore, it also helps make
     * them consistent between the main build and the playground builds.
     */
    private fun setDefaultGroupFromProjectPath() {
        project.group =
            project.path
                .split(":")
                .filter { it.isNotEmpty() }
                .dropLast(1)
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
            if (!version.isSnapshot()) {
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

    /** Whether the version for this artifact has been set */
    var versionIsSet = false
        private set

    /** Description for this artifact to use in .pom files */
    var description: String? = null
    /** The year when the development of this library started to use in .pom files */
    var inceptionYear: String? = null

    /** The main license to add when publishing. Default is Apache 2. */
    var license: License =
        License().apply {
            name = "The Apache Software License, Version 2.0"
            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
        }

    private var extraLicenses: MutableCollection<License> = ArrayList()

    // Should only be used to override LibraryType.publish, if a library isn't ready to publish yet
    var publish: Publish = Publish.UNSET

    fun shouldPublish(): Boolean =
        if (publish != Publish.UNSET) {
            publish.shouldPublish()
        } else if (type != LibraryType.UNSET) {
            type.publish.shouldPublish()
        } else {
            false
        }

    fun shouldRelease(): Boolean =
        if (publish != Publish.UNSET) {
            publish.shouldRelease()
        } else if (type != LibraryType.UNSET) {
            type.publish.shouldRelease()
        } else {
            false
        }

    fun ifReleasing(action: () -> Unit) {
        project.afterEvaluate {
            if (shouldRelease()) {
                action()
            }
        }
    }

    fun isPublishConfigured(): Boolean = (publish != Publish.UNSET || type.publish != Publish.UNSET)

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

    var doNotDocumentReason: String? = null

    var type: LibraryType = LibraryType.UNSET

    /** Whether this project should fail on javac compilation warnings */
    var failOnDeprecationWarnings = true

    /**
     * Whether Kotlin Strict API mode is enabled, see
     * [kotlin 1.4 release notes](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors)
     */
    var legacyDisableKotlinStrictApiMode = false

    var bypassCoordinateValidation = false

    /** Whether Metalava should use K2 Kotlin front-end for source analysis */
    var metalavaK2UastEnabled = true

    val additionalDeviceTestApkKeys = mutableListOf<String>()

    val additionalDeviceTestTags: MutableList<String> by lazy {
        val tags =
            when {
                project.path.startsWith(":privacysandbox:ads:") ->
                    mutableListOf("privacysandbox", "privacysandbox_ads")
                project.path.startsWith(":privacysandbox:") -> mutableListOf("privacysandbox")
                project.path.startsWith(":wear:") -> mutableListOf("wear")
                else -> mutableListOf()
            }
        if (deviceTests.enableAlsoRunningOnPhysicalDevices) {
            tags.add("all_run_on_physical_device")
        }
        return@lazy tags
    }

    fun shouldEnforceKotlinStrictApiMode(): Boolean {
        return !legacyDisableKotlinStrictApiMode && runApiTasks is RunApiTasks.Yes
    }

    fun extraLicense(closure: Closure<Any>): License {
        val license = project.configure(License(), closure) as License
        extraLicenses.add(license)
        return license
    }

    fun getExtraLicenses(): Collection<License> {
        return extraLicenses
    }

    fun configureAarAsJarForConfiguration(name: String) {
        configureAarAsJarForConfigurationDelegate(name)
    }

    fun getReferenceSha(): Provider<String> = headShaProvider()

    /**
     * Specify the version for Kotlin API compatibility mode used during Kotlin compilation.
     *
     * Changing this value will force clients to update their Kotlin compiler version, which may be
     * disruptive. Library developers should only change this value if there is a strong reason to
     * upgrade their Kotlin API version ahead of the rest of Jetpack.
     */
    abstract val kotlinTarget: Property<KotlinTarget>

    /**
     * A list of test module names for the project.
     *
     * Includes both host and device tests. These names should match the ones in AnTS.
     */
    abstract val testModuleNames: SetProperty<String>

    override val kotlinApiVersion: Provider<KotlinVersion>
        get() = kotlinTarget.map { it.apiVersion }

    override val kotlinBomVersion: Provider<String>
        get() = kotlinTarget.map { project.getVersionByName(it.catalogVersion) }

    companion object {
        const val DEFAULT_UNSPECIFIED_VERSION = "unspecified"
    }

    /** List of documentation samples projects for this project. */
    var samplesProjects: MutableCollection<Project> = mutableSetOf()
        private set

    /**
     * Used to register a project that will be providing documentation samples for this project. Can
     * only be called once so only one samples library can exist per library b/318840087.
     */
    fun samples(samplesProject: Project) {
        samplesProjects.add(samplesProject)
    }

    /** Locates a project by path. */
    // This method is needed for Gradle project isolation to avoid calls to parent projects due to
    // androidx { samples(project(":foo")) }
    // Without this method, the call above results into a call to the parent object, because
    // AndroidXExtension has `val project: Project`, which from groovy `project` call within
    // `androidx` block tries retrieves that project object and calls to look for :foo property
    // on it, then checking all the parents for it.
    fun project(name: String): Project = project.project(name)
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

    /** Whether this project's Android on device tests should be run in CI. */
    var enabled = true
    /** The app project that this project's Android on device tests require to be able to run. */
    var targetAppProject: Project? = null
    var targetAppVariant = "debug"

    /**
     * Whether this project's Android on device tests should also run on a physical Android device
     * when run in CI.
     */
    var enableAlsoRunningOnPhysicalDevices = false
}
