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

import androidx.build.buildInfo.CreateLibraryBuildInfoFileTask
import androidx.build.sources.PublishingVariant
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.utils.childrenIterator
import com.android.utils.forEach
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.StringWriter
import org.dom4j.Element
import org.dom4j.io.XMLWriter
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper

fun Project.configureMavenArtifactUpload(
    androidXExtension: AndroidXExtension,
    androidXKmpExtension: AndroidXMultiplatformExtension,
    afterConfigure: () -> Unit,
) {
    apply(mapOf("plugin" to "maven-publish"))
    var registered = false
    fun registerOnFirstPublishableArtifact(component: SoftwareComponent) {
        if (!registered) {
            configureComponentPublishing(
                androidXExtension,
                androidXKmpExtension,
                component,
                afterConfigure,
            )
            Release.register(this, androidXExtension)
            registered = true
        }
    }
    afterEvaluate {
        if (!androidXExtension.shouldPublish()) {
            return@afterEvaluate
        }
        components.configureEach { component ->
            if (isValidReleaseComponent(component)) {
                registerOnFirstPublishableArtifact(component)
            }
        }
    }
    // validate that all libraries that should be published actually get registered.
    gradle.taskGraph.whenReady {
        if (releaseTaskShouldBeRegistered(androidXExtension)) {
            validateTaskIsRegistered(Release.PROJECT_ARCHIVE_ZIP_TASK_NAME)
        }
        if (buildInfoTaskShouldBeRegistered(androidXExtension)) {
            if (!androidXExtension.isIsolatedProjectsEnabled()) {
                validateTaskIsRegistered(CreateLibraryBuildInfoFileTask.TASK_NAME)
            }
        }
    }
}

private fun Project.validateTaskIsRegistered(taskName: String) =
    tasks.findByName(taskName)
        ?: throw GradleException(
            "Project $name is configured for publishing, but a '$taskName' task was never " +
                "registered. This is likely a bug in AndroidX plugin configuration."
        )

private fun Project.releaseTaskShouldBeRegistered(extension: AndroidXExtension): Boolean {
    if (plugins.hasPlugin(AppPlugin::class.java)) {
        return false
    }
    if (!extension.shouldRelease() && !isSnapshotBuild()) {
        return false
    }
    return extension.shouldPublish()
}

private fun Project.buildInfoTaskShouldBeRegistered(extension: AndroidXExtension): Boolean {
    if (plugins.hasPlugin(AppPlugin::class.java)) {
        return false
    }
    return extension.shouldRelease()
}

/** Configure publishing for a [SoftwareComponent]. */
private fun Project.configureComponentPublishing(
    extension: AndroidXExtension,
    androidxKmpExtension: AndroidXMultiplatformExtension,
    component: SoftwareComponent,
    afterConfigure: () -> Unit,
) {
    val androidxGroup = validateCoordinatesAndGetGroup(extension)
    val projectArchiveDir =
        File(getRepositoryDirectory(), "${androidxGroup.group.replace('.', '/')}/$name")
    group = androidxGroup.group

    /*
     * Provides a set of maven coordinates (groupId:artifactId) of artifacts in AndroidX
     * that are Android Libraries.
     */
    val androidLibrariesSetProvider: Provider<Set<String>> = provider {
        val androidxAndroidProjects = mutableSetOf<String>()
        // Check every project is the project map to see if they are an Android Library
        val projectModules = extension.mavenCoordinatesToProjectPathMap
        for ((mavenCoordinates, projectPath) in projectModules) {
            project.findProject(projectPath)?.let { project ->
                if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
                    if (project.plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java)) {
                        // For KMP projects, android AAR is published under -android
                        androidxAndroidProjects.add("$mavenCoordinates-android")
                    } else {
                        androidxAndroidProjects.add(mavenCoordinates)
                    }
                }
                if (project.hasAndroidMultiplatformPlugin()) {
                    androidxAndroidProjects.add("$mavenCoordinates-android")
                }
            }
        }
        androidxAndroidProjects
    }

    configure<PublishingExtension> {
        repositories {
            it.maven { repo -> repo.setUrl(getRepositoryDirectory()) }
            it.maven { repo -> repo.setUrl(getPerProjectRepositoryDirectory()) }
        }
        publications {
            if (appliesJavaGradlePluginPlugin()) {
                // The 'java-gradle-plugin' will also add to the 'pluginMaven' publication
                it.create<MavenPublication>("pluginMaven")
                tasks.getByName("publishPluginMavenPublicationToMavenRepository").doFirst {
                    removePreviouslyUploadedArchives(projectArchiveDir)
                }
                afterConfigure()
            } else {
                if (project.isMultiplatformPublicationEnabled()) {
                    afterConfigure()
                } else {
                    it.create<MavenPublication>("maven") { from(component) }
                    tasks.getByName("publishMavenPublicationToMavenRepository").doFirst {
                        removePreviouslyUploadedArchives(projectArchiveDir)
                    }
                    afterConfigure()
                }
            }
        }
        publications.withType(MavenPublication::class.java).configureEach { publication ->
            // Used to add buildId to Gradle module metadata set below
            publication.withBuildIdentifier()
            val isKmpAnchor = (publication.name == KMP_ANCHOR_PUBLICATION_NAME)
            val pomPlatform = androidxKmpExtension.defaultPlatform
            // b/297355397 If a kmp project has Android as the default platform, there might
            // externally be legacy projects depending on its .pom
            // We advertise a stub .aar in this .pom for backwards compatibility and
            // add a dependency on the actual .aar
            val addStubAar = isKmpAnchor && pomPlatform == PlatformIdentifier.ANDROID.id
            val buildDir = project.layout.buildDirectory
            if (addStubAar) {
                @Suppress("DEPRECATION") // TODO(aurimas): migrate to new API
                val minSdk =
                    project.extensions.findByType<LibraryExtension>()?.defaultConfig?.minSdk
                        ?: extensions
                            .findByType<AndroidXMultiplatformExtension>()
                            ?.agpKmpExtension
                            ?.minSdk
                        ?: throw GradleException(
                            "Couldn't find valid Android extension to read minSdk from"
                        )
                // create a unique namespace for this .aar, different from the android artifact
                val stubNamespace =
                    project.group.toString().replace(':', '.') +
                        "." +
                        project.name.toString().replace('-', '.') +
                        ".anchor"
                val unpackedStubAarTask =
                    tasks.register("unpackedStubAar", UnpackedStubAarTask::class.java) { aarTask ->
                        aarTask.aarPackage.set(stubNamespace)
                        aarTask.minSdkVersion.set(minSdk)
                        aarTask.outputDir.set(buildDir.dir("intermediates/stub-aar"))
                    }
                val stubAarTask =
                    tasks.register("stubAar", ZipStubAarTask::class.java) { zipTask ->
                        zipTask.from(unpackedStubAarTask.flatMap { it.outputDir })
                        zipTask.destinationDirectory.set(buildDir.dir("outputs"))
                        zipTask.archiveExtension.set("aar")
                    }
                publication.artifact(stubAarTask)
            }

            publication.pom { pom ->
                if (addStubAar) {
                    pom.packaging = "aar"
                }
                addInformativeMetadata(extension, pom)
                tweakDependenciesMetadata(
                    androidxGroup,
                    pom,
                    androidLibrariesSetProvider,
                    isKmpAnchor,
                    pomPlatform,
                )
            }
        }
    }

    // Workarounds for https://github.com/gradle/gradle/issues/20011
    project.tasks.withType(GenerateModuleMetadata::class.java).configureEach { task ->
        task.doLast {
            val metadataFile = task.outputFile.asFile.get()
            val metadata = metadataFile.readText()
            verifyGradleMetadata(metadata)
            val sortedMetadata = sortGradleMetadataDependencies(metadata)

            if (metadata != sortedMetadata) {
                metadataFile.writeText(sortedMetadata)
            }
        }
    }
    project.tasks.withType(GenerateMavenPom::class.java).configureEach { task ->
        task.doLast {
            val pomFile = task.destination
            val pom = pomFile.readText()
            val sortedPom = sortPomDependencies(pom)

            if (pom != sortedPom) {
                pomFile.writeText(sortedPom)
            }
        }
    }

    // Workaround for https://github.com/gradle/gradle/issues/31218
    project.tasks.withType(GenerateModuleMetadata::class.java).configureEach { task ->
        task.doLast {
            val metadata = task.outputFile.asFile.get()
            val text = metadata.readText()
            metadata.writeText(
                text.replace("\"buildId\": .*".toRegex(), "\"buildId:\": \"${getBuildId()}\"")
            )
        }
    }
}

/** Looks for a dependencies XML element within [pom] and sorts its contents. */
fun sortPomDependencies(pom: String): String {
    // Workaround for using the default namespace in dom4j.
    val namespaceUris = mapOf("ns" to "http://maven.apache.org/POM/4.0.0")
    val document = parseXml(pom, namespaceUris)

    // For each <dependencies> element, sort the contained elements in-place.
    document.rootElement.selectNodes("ns:dependencies").filterIsInstance<Element>().forEach {
        element ->
        val deps = element.elements()
        val sortedDeps = deps.toSortedSet(compareBy { it.stringValue }).toList()
        // Content contains formatting nodes, so to avoid modifying those we replace
        // each element with the sorted element from its respective index. Note this
        // will not move adjacent elements, so any comments would remain in their
        // original order.
        element.content().replaceAll {
            val index = sortedDeps.indexOf(it)
            if (index >= 0) {
                deps[index]
            } else {
                it
            }
        }
    }

    // Write to string. Note that this does not preserve the original indent level, but it
    // does preserve line breaks -- not that any of this matters for client XML parsing.
    val stringWriter = StringWriter()
    XMLWriter(stringWriter).apply {
        setIndentLevel(2)
        write(document)
        close()
    }

    return stringWriter.toString()
}

/** Looks for a dependencies JSON element within [metadata] and sorts its contents. */
fun sortGradleMetadataDependencies(metadata: String): String {
    val gson = GsonBuilder().create()
    val jsonObj = gson.fromJson(metadata, JsonObject::class.java)!!
    jsonObj.getAsJsonArray("variants").forEach { entry ->
        (entry as? JsonObject)?.getAsJsonArray("dependencies")?.let { jsonArray ->
            val sortedSet = jsonArray.toSortedSet(compareBy { it.toString() })
            jsonArray.removeAll { true }
            sortedSet.forEach { element -> jsonArray.add(element) }
        }
    }

    val stringWriter = StringWriter()
    val jsonWriter = JsonWriter(stringWriter)
    jsonWriter.setIndent("  ")
    gson.toJson(jsonObj, jsonWriter)
    return stringWriter.toString()
}

/**
 * Checks the variants field in the metadata file has an entry containing "sourcesElements". All our
 * publications must be published with a sources variant.
 */
fun verifyGradleMetadata(metadata: String) {
    val gson = GsonBuilder().create()
    val jsonObj = gson.fromJson(metadata, JsonObject::class.java)!!
    jsonObj.getAsJsonArray("variants").firstOrNull { variantElement ->
        variantElement.asJsonObject
            .get("name")
            .asString
            .contains(other = PublishingVariant.SourcesElements.name, ignoreCase = true)
    }
        ?: throw Exception(
            "The ${PublishingVariant.SourcesElements.name} variant must exist in the module file."
        )
}

private fun Project.isMultiplatformPublicationEnabled(): Boolean {
    return extensions.findByType<KotlinMultiplatformExtension>() != null
}

private fun Project.isValidReleaseComponent(component: SoftwareComponent) =
    component.name == releaseComponentName()

private fun Project.releaseComponentName() =
    when {
        plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java) -> "kotlin"
        plugins.hasPlugin(JavaPlugin::class.java) -> "java"
        else -> "release"
    }

private fun Project.validateCoordinatesAndGetGroup(extension: AndroidXExtension): LibraryGroup {
    val mavenGroup = extension.mavenGroup
    if (mavenGroup == null) {
        val groupExplanation = extension.explainMavenGroup().joinToString("\n")
        throw Exception("You must specify mavenGroup for $path :\n$groupExplanation")
    }
    val strippedGroupId = mavenGroup.group.substringAfterLast(".")
    if (
        !extension.bypassCoordinateValidation &&
            mavenGroup.group.startsWith("androidx") &&
            !name.startsWith(strippedGroupId)
    ) {
        throw Exception("Your artifactId must start with '$strippedGroupId'. (currently is $name)")
    }
    return mavenGroup
}

/**
 * Delete any existing archives, so that developers don't get confused/surprised by the presence of
 * old versions. Additionally, deleting old versions makes it more convenient to iterate over all
 * existing archives without visiting archives having old versions too
 */
private fun removePreviouslyUploadedArchives(projectArchiveDir: File) {
    projectArchiveDir.deleteRecursively()
}

private fun Project.addInformativeMetadata(extension: AndroidXExtension, pom: MavenPom) {
    pom.name.set(extension.name)
    pom.description.set(provider { extension.description })
    pom.url.set(
        provider {
            fun defaultUrl() =
                "https://developer.android.com/jetpack/androidx/releases/" +
                    extension.mavenGroup!!.group.removePrefix("androidx.").replace(".", "-") +
                    "#" +
                    extension.project.version()
            getAlternativeProjectUrl() ?: defaultUrl()
        }
    )
    pom.inceptionYear.set(provider { extension.inceptionYear })
    pom.licenses { licenses ->
        licenses.license { license ->
            license.name.set(extension.license.name)
            license.url.set(extension.license.url)
            license.distribution.set("repo")
        }

        for (extraLicense in extension.getExtraLicenses()) {
            licenses.license { license ->
                license.name.set(provider { extraLicense.name })
                license.url.set(provider { extraLicense.url })
                license.distribution.set("repo")
            }
        }
    }
    pom.scm { scm ->
        scm.url.set("https://cs.android.com/androidx/platform/frameworks/support")
        scm.connection.set(ANDROID_GIT_URL)
    }
    pom.organization { org -> org.name.set("The Android Open Source Project") }
    pom.developers { devs ->
        devs.developer { dev -> dev.name.set("The Android Open Source Project") }
    }
}

private fun tweakDependenciesMetadata(
    mavenGroup: LibraryGroup,
    pom: MavenPom,
    androidLibrariesSetProvider: Provider<Set<String>>,
    kmpAnchor: Boolean,
    pomPlatform: String?,
) {
    pom.withXml { xml ->
        // The following code depends on getProjectsMap which is only available late in
        // configuration at which point Java Library plugin's variants are not allowed to be
        // modified. TODO remove the use of getProjectsMap and move to earlier configuration.
        // For more context see:
        // https://android-review.googlesource.com/c/platform/frameworks/support/+/1144664/8/buildSrc/src/main/kotlin/androidx/build/MavenUploadHelper.kt#177
        assignSingleVersionDependenciesInGroupForPom(xml, mavenGroup)
        assignAarDependencyTypes(xml, androidLibrariesSetProvider.get())
        ensureConsistentJvmSuffix(xml)

        if (kmpAnchor && pomPlatform != null) {
            insertDefaultMultiplatformDependencies(xml, pomPlatform)
        }
    }
}

// TODO(aurimas): remove this when Gradle bug is fixed.
// https://github.com/gradle/gradle/issues/3170
fun assignAarDependencyTypes(xml: XmlProvider, androidLibrariesSet: Set<String>) {
    val xmlElement = xml.asElement()
    val dependencies = xmlElement.find { it.nodeName == "dependencies" } as? org.w3c.dom.Element

    dependencies?.getElementsByTagName("dependency")?.forEach { dependency ->
        val groupId =
            dependency.find { it.nodeName == "groupId" }?.textContent
                ?: throw IllegalArgumentException("Failed to locate groupId node")
        val artifactId =
            dependency.find { it.nodeName == "artifactId" }?.textContent
                ?: throw IllegalArgumentException("Failed to locate artifactId node")
        if (androidLibrariesSet.contains("$groupId:$artifactId")) {
            dependency.appendElement("type", "aar")
        }
    }
}

fun insertDefaultMultiplatformDependencies(xml: XmlProvider, platformId: String) {
    val xmlElement = xml.asElement()
    val groupId =
        xmlElement.find { it.nodeName == "groupId" }?.textContent
            ?: throw IllegalArgumentException("Failed to locate groupId node")
    val artifactId =
        xmlElement.find { it.nodeName == "artifactId" }?.textContent
            ?: throw IllegalArgumentException("Failed to locate artifactId node")
    val version =
        xmlElement.find { it.nodeName == "version" }?.textContent
            ?: throw IllegalArgumentException("Failed to locate version node")

    // Find the top-level <dependencies> element or add one if there are no other dependencies.
    val dependencies =
        xmlElement.find { it.nodeName == "dependencies" }
            ?: xmlElement.appendElement("dependencies")
    dependencies.appendElement("dependency").apply {
        appendElement("groupId", groupId)
        appendElement("artifactId", "$artifactId-$platformId")
        appendElement("version", version)
        if (platformId == PlatformIdentifier.ANDROID.id) {
            appendElement("type", "aar")
        }
        appendElement("scope", "compile")
    }
}

private fun org.w3c.dom.Node.appendElement(
    tagName: String,
    textValue: String? = null,
): org.w3c.dom.Element {
    val element = ownerDocument.createElement(tagName)
    appendChild(element)

    if (textValue != null) {
        val textNode = ownerDocument.createTextNode(textValue)
        element.appendChild(textNode)
    }

    return element
}

private fun org.w3c.dom.Node.find(predicate: (org.w3c.dom.Node) -> Boolean): org.w3c.dom.Node? {
    val iterator = childrenIterator()
    while (iterator.hasNext()) {
        val node = iterator.next()
        if (predicate(node)) {
            return node
        }
    }
    return null
}

/**
 * Modifies the given .pom to specify that every dependency in <group> refers to a single version
 * and can't be automatically promoted to a new version. This will replace, for example, a version
 * string of "1.0" with a version string of "[1.0]"
 *
 * Note: this is not enforced in Gradle nor in plain Maven (without the Enforcer plugin)
 * (https://github.com/gradle/gradle/issues/8297)
 */
fun assignSingleVersionDependenciesInGroupForPom(xml: XmlProvider, mavenGroup: LibraryGroup) {
    if (!mavenGroup.requireSameVersion) {
        return
    }

    val dependencies =
        xml.asElement().find { it.nodeName == "dependencies" } as? org.w3c.dom.Element ?: return

    dependencies.getElementsByTagName("dependency").forEach { dependency ->
        val groupId =
            dependency.find { it.nodeName == "groupId" }?.textContent
                ?: throw IllegalArgumentException("Failed to locate groupId node")
        if (groupId == mavenGroup.group) {
            val versionNode =
                dependency.find { it.nodeName == "version" }
                    ?: throw IllegalArgumentException("Failed to locate version node")
            val version = versionNode.textContent
            if (isVersionRange(version)) {
                throw GradleException("Unsupported version '$version': already is a version range")
            }
            val pinnedVersion = "[$version]"
            versionNode.textContent = pinnedVersion
        }
    }
}

private fun isVersionRange(text: String): Boolean {
    return text.contains("[") ||
        text.contains("]") ||
        text.contains("(") ||
        text.contains(")") ||
        text.contains(",")
}

/**
 * Ensures that artifactIds are consistent when using configuration caching. A workaround for
 * https://github.com/gradle/gradle/issues/18369
 */
fun ensureConsistentJvmSuffix(xml: XmlProvider) {
    val dependencies =
        xml.asElement().find { it.nodeName == "dependencies" } as? org.w3c.dom.Element ?: return

    dependencies.getElementsByTagName("dependency").forEach { dependency ->
        val artifactId =
            dependency.find { it.nodeName == "artifactId" }
                ?: throw IllegalArgumentException("Failed to locate artifactId node")
        // kotlinx-coroutines-core is only a .pom and only depends on kotlinx-coroutines-core-jvm,
        // so the two artifacts should be approximately equivalent. However,
        // when loading from configuration cache, Gradle often returns a different resolution.
        // We replace it here to ensure consistency and predictability, and
        // to avoid having to rerun any zip tasks that include it
        if (artifactId.textContent == "kotlinx-coroutines-core-jvm") {
            artifactId.textContent = "kotlinx-coroutines-core"
        }
    }
}

private fun Project.appliesJavaGradlePluginPlugin() = pluginManager.hasPlugin("java-gradle-plugin")

private const val ANDROID_GIT_URL =
    "scm:git:https://android.googlesource.com/platform/frameworks/support"

// Name of KMP root publication
// https://github.com/JetBrains/kotlin/blob/bf6cb00fa8db7879c323bad863f58a0545c3d655/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/publishing/Publishing.kt#L54
internal const val KMP_ANCHOR_PUBLICATION_NAME = "kotlinMultiplatform"

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class ZipStubAarTask : Zip()
