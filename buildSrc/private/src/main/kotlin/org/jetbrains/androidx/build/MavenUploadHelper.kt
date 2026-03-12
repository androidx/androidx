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

package org.jetbrains.androidx.build

import androidx.build.AndroidXExtension
import androidx.build.AndroidXMultiplatformExtension
import androidx.build.getRepositoryDirectory
import androidx.build.hasAndroidMultiplatformPlugin
import androidx.build.multiplatformExtension
import com.android.build.gradle.LibraryPlugin
import com.android.utils.childrenIterator
import com.android.utils.forEach
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.StringTokenizer
import kotlin.collections.find
import org.apache.xerces.jaxp.SAXParserImpl.JAXPSAXParser
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.DocumentFactory
import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.w3c.dom.Node

fun Project.configureMavenArtifactUpload(
    componentFactory: SoftwareComponentFactory
) {
    if (!JetBrainsPublication.shouldPublish(project)) return
    apply(mapOf("plugin" to "maven-publish"))
    var registered = false
    fun registerOnFirstPublishableArtifact(component: SoftwareComponent) {
        if (!registered) {
            configureComponentPublishing(component, componentFactory)
            registered = true
        }
    }
    afterEvaluate {
        components.all { component ->
            if (isValidReleaseComponent(component)) {
                registerOnFirstPublishableArtifact(component)
            }
        }
    }
}

/**
 * Configure publishing for a [SoftwareComponent].
 */
private fun Project.configureComponentPublishing(
    component: SoftwareComponent,
    componentFactory: SoftwareComponentFactory
) {
    val extension = project.extensions.getByType(AndroidXExtension::class.java)
    val kmpExtension =
        project.extensions.getByType(AndroidXMultiplatformExtension::class.java)

    val projectArchiveDir = File(
        getRepositoryDirectory(),
        "${group.toString().replace('.', '/')}/$name"
    )

    /*
     * Provides a set of maven coordinates (groupId:artifactId) of artifacts in AndroidX
     * that are Android Libraries.
     */
    val androidLibrariesSetProvider: Provider<Set<String>> = provider {
        val androidxAndroidProjects = mutableSetOf<String>()
        // Check every project is the project map to see if they are an Android Library
        for (projectPath in JetBrainsPublication.projectPathToLibrary.keys) {
            project.findProject(projectPath)?.let { project ->
                val mavenCoordinates = "${project.group}:${project.name}"
                if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
                    androidxAndroidProjects.add(mavenCoordinates)
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
            it.maven { repo ->
                repo.setUrl(getRepositoryDirectory())
            }
        }
        publications {
            if (appliesJavaGradlePluginPlugin()) {
                // The 'java-gradle-plugin' will also add to the 'pluginMaven' publication
                it.create<MavenPublication>("pluginMaven")
                tasks.getByName("publishPluginMavenPublicationToMavenRepository").doFirst {
                    removePreviouslyUploadedArchives(projectArchiveDir)
                }
            } else {
                if (project.isMultiplatformPublicationEnabled()) {
                    configureMultiplatformPublication(componentFactory)
                } else {
                    it.create<MavenPublication>("maven") {
                        from(component)
                    }
                    tasks.getByName("publishMavenPublicationToMavenRepository").doFirst {
                        removePreviouslyUploadedArchives(projectArchiveDir)
                    }
                }
            }
        }
        publications.withType(MavenPublication::class.java).all { publication ->
            if (artifactRedirection() != null && !JetBrainsPublication.isCompatibilityStubProject(project)) {
                // Gradle cannot map variant capabilities into POM metadata, so redirected
                // publications emit warning noise for their published component variants.
                publication.suppressRedirectionPomMetadataWarnings()
            }
            publication.pom { pom ->
                addInformativeMetadata(extension, pom)
                tweakDependenciesMetadata(
                    pom, androidLibrariesSetProvider,
                    publication.name == KMP_ANCHOR_PUBLICATION_NAME, kmpExtension.defaultPlatform)
            }
        }
    }

    project.tasks.withType(GenerateModuleMetadata::class.java).configureEach { task ->
        val capabilitiesToRemove = publishedRedirectionCapabilities()
        task.doLast {
            val metadataFile = task.outputFile.asFile.get()
            val metadataString = metadataFile.readText()
            val modifiedMetadataString = modifyGradleMetadata(metadataString) { metadata ->
                filterGradleMetadataCapabilities(metadata, capabilitiesToRemove)
                sortGradleMetadataDependencies(metadata)
            }

            if (metadataString != modifiedMetadataString) {
                metadataFile.writeText(modifiedMetadataString)
            }
        }
    }
    // run code only after all projects because it depends on redirection info,
    // which is constructed at a project evaluation step
    gradle.projectsEvaluated {
        project.tasks.withType(GenerateMavenPom::class.java).configureEach { task ->
            fun hasTargetWithComponent(componentName: String) =
                task.project.multiplatformExtension?.targets?.find { target ->
                    target.components.any { it.name == componentName }
                } != null

            // extract heuristically from the task name:
            // generatePomFileForKotlinMultiplatformDecoratedPublication
            // generatePomFileForDesktopDecoratedPublication
            // ...
            // and take only if it is a target's component (we redirect only targets)
            val componentName: String? = Regex("^generatePomFileFor(.*)Publication$")
                .matchEntire(task.name)
                ?.groupValues?.get(1)
                ?.replaceFirstChar { it.lowercase() }
                ?.takeIf(::hasTargetWithComponent)

            val originalToRedirected: Map<ModuleIdentifier, ModuleVersionIdentifier> = if (componentName != null) {
                originalToRedirectedDependency(componentName)
            } else {
                emptyMap()
            }

            task.doLast {
                val pomFile = task.destination
                val pom = pomFile.readText()
                val modifiedPom = modifyPomDependencies(pom, originalToRedirected)
                if (pom != modifiedPom) {
                    pomFile.writeText(modifiedPom)
                }
            }
        }
    }
}

/**
 * Looks for a dependencies XML element within [pom], sorts its contents and modify it by redirecting coordinates
 */
internal fun modifyPomDependencies(
    pom: String,
    originalToRedirected: Map<ModuleIdentifier, ModuleVersionIdentifier>
): String {
    // Workaround for using the default namespace in dom4j.
    val namespaceUris = mapOf("ns" to "http://maven.apache.org/POM/4.0.0")
    val docFactory = DocumentFactory()
    docFactory.xPathNamespaceURIs = namespaceUris
    // Ensure that we're consistently using JAXP parser.
    val xmlReader = JAXPSAXParser()
    val document = parseText(docFactory, xmlReader, pom)

    // For each <dependencies> element, sort the contained elements in-place.
    document.rootElement
        .selectNodes("ns:dependencies")
        .filterIsInstance<Element>()
        .forEach { element ->
            val deps = element.elements()
            val modifiedDeps = deps
                .onEach { modifyPomDependency(it, originalToRedirected) }
                .sortedBy { it.stringValue }

            // Content contains formatting nodes, so to avoid modifying those we replace
            // each element with the sorted element from its respective index. Note this
            // will not move adjacent elements, so any comments would remain in their
            // original order.
            element.content().replaceAll {
                val index = deps.indexOf(it)
                if (index >= 0) {
                    modifiedDeps[index]
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

internal fun modifyPomDependency(
    dependency: Element,
    originalToRedirected: Map<ModuleIdentifier, ModuleVersionIdentifier>
) {
    val groupIdNode = dependency.selectSingleNode("ns:groupId")
    val artifactIdNode = dependency.selectSingleNode("ns:artifactId")
    val versionNode = dependency.selectSingleNode("ns:version")
    val id = DefaultModuleIdentifier.newId(groupIdNode.stringValue, artifactIdNode.stringValue)
    val redirected = originalToRedirected[id]
    if (redirected != null) {
        groupIdNode.text = redirected.group
        artifactIdNode.text = redirected.name
        versionNode.text = redirected.version
    }
}

// Coped from org.dom4j.DocumentHelper with modifications to allow SAXReader configuration.
@Throws(DocumentException::class)
fun parseText(
    documentFactory: DocumentFactory,
    xmlReader: XMLReader,
    text: String,
): Document {
    val reader = SAXReader.createDefault()
    reader.documentFactory = documentFactory
    reader.xmlReader = xmlReader
    val encoding = getEncoding(text)
    val source = InputSource(StringReader(text))
    source.encoding = encoding
    val result = reader.read(source)
    if (result.xmlEncoding == null) {
        result.xmlEncoding = encoding
    }
    return result
}

// Coped from org.dom4j.DocumentHelper.
private fun getEncoding(text: String): String? {
    var result: String? = null
    val xml = text.trim { it <= ' ' }
    if (xml.startsWith("<?xml")) {
        val end = xml.indexOf("?>")
        val sub = xml.substring(0, end)
        val tokens = StringTokenizer(sub, " =\"'")
        while (tokens.hasMoreTokens()) {
            val token = tokens.nextToken()
            if ("encoding" == token) {
                if (tokens.hasMoreTokens()) {
                    result = tokens.nextToken()
                }
                break
            }
        }
    }
    return result
}

/**
 * Workaround for https://github.com/gradle/gradle/issues/20011.
 * Looks for a dependencies JSON element within [metadata] and sorts its contents.
 */
private fun sortGradleMetadataDependencies(metadata: JsonObject) {
    metadata.getAsJsonArray("variants").forEach { entry ->
        (entry as? JsonObject)?.getAsJsonArray("dependencies")?.let { jsonArray ->
            val sortedSet = jsonArray.toSortedSet(compareBy { it.toString() })
            jsonArray.removeAll { true }
            sortedSet.forEach { element -> jsonArray.add(element) }
        }
    }
}

/**
 * Removes only the capability declarations introduced by [Project.configureRedirectionCapability].
 * These capabilities are needed for local resolution inside the current build, but they should not
 * leak into published module metadata.
 */
private fun filterGradleMetadataCapabilities(
    metadata: JsonObject,
    capabilitiesToRemove: Set<String>,
) {
    if (capabilitiesToRemove.isEmpty()) return

    metadata.getAsJsonArray("variants").forEach { entry ->
        val variant = entry as? JsonObject ?: return@forEach
        val capabilities = variant.getAsJsonArray("capabilities") ?: return@forEach
        capabilities.removeAll { capabilityElement ->
            val capability = capabilityElement as? JsonObject ?: return@removeAll false
            capability.notation() in capabilitiesToRemove
        }
        if (capabilities.isEmpty) {
            variant.remove("capabilities")
        }
    }
}

private fun modifyGradleMetadata(
    metadata: String,
    block: (JsonObject) -> Unit,
): String {
    val gson = GsonBuilder().create()
    val jsonObj = gson.fromJson(metadata, JsonObject::class.java)!!
    block(jsonObj)
    val stringWriter = StringWriter()
    val jsonWriter = JsonWriter(stringWriter)
    jsonWriter.setIndent("  ")
    gson.toJson(jsonObj, jsonWriter)
    return stringWriter.toString()
}

private fun MavenPublication.suppressRedirectionPomMetadataWarnings() {
    listOf("ApiElements", "RuntimeElements", "SourcesElements", "MetadataElements").forEach { suffix ->
        suppressPomMetadataWarningsFor("${name}$suffix-published")
    }
}

private fun JsonObject.notation(): String = "${get("group").asString}:${get("name").asString}:${get("version").asString}"

private fun Project.isMultiplatformPublicationEnabled(): Boolean {
    return extensions.findByType<KotlinMultiplatformExtension>() != null
}

private fun Project.configureMultiplatformPublication(componentFactory: SoftwareComponentFactory) {
    if (!JetBrainsPublication.isJetBrainsProjectWithAndroidTarget(this)) return
    replaceBaseMultiplatformPublication(componentFactory)
}

/**
 * KMP does not include a sources configuration (b/235486368), so we replace it with our own
 * publication that includes it.  This uses internal API as a workaround while waiting for a fix
 * on the original bug.
 */
private fun Project.replaceBaseMultiplatformPublication(
    componentFactory: SoftwareComponentFactory
) {
    val kotlinComponent = components.findByName("kotlin") as SoftwareComponentInternal
    withSourcesComponents(
        componentFactory,
        setOf("androidxSourcesElements")
    ) { sourcesComponents ->
        configure<PublishingExtension> {
            publications { pubs ->
                pubs.create<MavenPublication>(KMP_ANCHOR_PUBLICATION_NAME) {
                    // Duplicate behavior from KMP plugin
                    // (https://cs.github.com/JetBrains/kotlin/blob/0c001cc9939a2ab11815263ed825c1096b3ce087/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/Publishing.kt#L42)
                    // Should be able to remove internal API usage once
                    // https://youtrack.jetbrains.com/issue/KT-36943 is fixed
                    (this as MavenPublicationInternal).publishWithOriginalFileName()

                    from(object : ComponentWithVariants, SoftwareComponentInternal {
                        override fun getName(): String {
                            return KMP_ANCHOR_PUBLICATION_NAME
                        }

                        override fun getUsages(): MutableSet<out UsageContext> {
                            // Include sources artifact we built and root artifacts from kotlin plugin.
                            return (
                                sourcesComponents.flatMap { it.usages } +
                                kotlinComponent.usages
                            ).toMutableSet()
                        }

                        override fun getVariants(): MutableSet<out SoftwareComponent> {
                            // Include all target-based variants from kotlin plugin.
                            return (kotlinComponent as ComponentWithVariants).variants
                        }
                    })
                }

                // mark original publication as an alias, so we do not try to publish it.
                pubs.named("kotlinMultiplatform").configure {
                    it as MavenPublicationInternal
                    it.isAlias = true
                }
            }

            disableBaseKmpPublications()
        }
    }
}

/**
 * If source configurations with the given names are currently in the project, or if they
 * eventually gets added, run the given [action] with those configurations as software components.
 */
private fun Project.withSourcesComponents(
    componentFactory: SoftwareComponentFactory,
    names: Set<String>,
    action: (List<SoftwareComponentInternal>) -> Unit
) {
    val targetConfigurations = mutableSetOf<Configuration>()
    configurations.configureEach {
        if (it.name in names) {
            targetConfigurations.add(it)
            if (targetConfigurations.size == names.size) {
                action(
                    targetConfigurations.map { configuration ->
                        componentFactory.adhoc(configuration.name).apply {
                            addVariantsFromConfiguration(configuration) {}
                        } as SoftwareComponentInternal
                    }
                )
            }
        }
    }
}

/**
 * Now that we have created our own publication that we want published, prevent the base publication
 * from being published using the roll-up tasks.  We should be able to remove this workaround when
 * b/235486368 is fixed.
 */
private fun Project.disableBaseKmpPublications() {
    listOf("publish", "publishToMavenLocal").forEach { taskName ->
        tasks.named(taskName).configure { publishTask ->
            publishTask.setDependsOn(publishTask.dependsOn.filterNot {
                (it as String).startsWith("publishKotlinMultiplatform")
            })
        }
    }
}

private fun Project.isValidReleaseComponent(component: SoftwareComponent) =
    component.name == releaseComponentName()

private fun Project.releaseComponentName() = when {
    plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java) -> "kotlin"
    plugins.hasPlugin(JavaPlugin::class.java) -> "java"
    else -> "release"
}

/**
 * Delete any existing archives, so that developers don't get
 * confused/surprised by the presence of old versions.
 * Additionally, deleting old versions makes it more convenient to iterate
 * over all existing archives without visiting archives having old versions too
 */
private fun removePreviouslyUploadedArchives(projectArchiveDir: File) {
    projectArchiveDir.deleteRecursively()
}

private fun Project.addInformativeMetadata(extension: AndroidXExtension, pom: MavenPom) {
    pom.name.set(extension.name)
    pom.description.set(extension.description)
    pom.url.set("https://github.com/JetBrains/compose-multiplatform")
    pom.inceptionYear.set(extension.inceptionYear)
    pom.licenses { licenses ->
        licenses.license { license ->
            license.name.set("The Apache Software License, Version 2.0")
            license.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            license.distribution.set("repo")
        }
        for (extraLicense in extension.getExtraLicenses()) {
            licenses.license { license ->
                license.name.set(provider { extraLicense.name!! })
                license.url.set(provider { extraLicense.url!! })
                license.distribution.set("repo")
            }
        }
    }
    pom.scm { scm ->
        scm.url.set("https://cs.android.com/androidx/platform/frameworks/support")
        scm.connection.set(ANDROID_GIT_URL)
    }
    pom.developers { devs ->
        devs.developer { dev ->
            dev.name.set("The Android Open Source Project")
        }
    }
}

private fun tweakDependenciesMetadata(
    pom: MavenPom,
    androidLibrariesSetProvider: Provider<Set<String>>,
    kmpAnchor: Boolean,
    pomPlatform: String?
) {
    pom.withXml { xml ->
        // The following code depends on getProjectsMap which is only available late in
        // configuration at which point Java Library plugin's variants are not allowed to be
        // modified. TODO remove the use of getProjectsMap and move to earlier configuration.
        // For more context see:
        // https://android-review.googlesource.com/c/platform/frameworks/support/+/1144664/8/buildSrc/src/main/kotlin/androidx/build/MavenUploadHelper.kt#177
        assignAarTypes(xml, androidLibrariesSetProvider.get())
        ensureConsistentJvmSuffix(xml)

        if (kmpAnchor && pomPlatform != null) {
            insertDefaultMultiplatformDependencies(xml, pomPlatform)
        }
    }
}

// TODO(aurimas): remove this when Gradle bug is fixed.
// https://github.com/gradle/gradle/issues/3170
fun assignAarTypes(
    xml: XmlProvider,
    androidLibrariesSet: Set<String>
) {
    val xmlElement = xml.asElement()
    val dependencies = xmlElement.find {
        it.nodeName == "dependencies"
    } as? org.w3c.dom.Element

    dependencies?.getElementsByTagName("dependency")?.forEach { dependency ->
        val groupId = dependency.find { it.nodeName == "groupId" }?.textContent
            ?: throw IllegalArgumentException("Failed to locate groupId node")
        val artifactId = dependency.find { it.nodeName == "artifactId" }?.textContent
            ?: throw IllegalArgumentException("Failed to locate artifactId node")
        if (androidLibrariesSet.contains("$groupId:$artifactId")) {
            dependency.appendElement("type", "aar")
        }
    }
}

fun insertDefaultMultiplatformDependencies(
    xml: XmlProvider,
    platformId: String
) {
    val xmlElement = xml.asElement()
    val groupId = xmlElement.find { it.nodeName == "groupId" }?.textContent
        ?: throw IllegalArgumentException("Failed to locate groupId node")
    val artifactId = xmlElement.find { it.nodeName == "artifactId" }?.textContent
        ?: throw IllegalArgumentException("Failed to locate artifactId node")
    val version = xmlElement.find { it.nodeName == "version" }?.textContent
        ?: throw IllegalArgumentException("Failed to locate version node")

    // Find the top-level <dependencies> element or add one if there are no other dependencies.
    val dependencies = xmlElement.find {
        it.nodeName == "dependencies"
    } ?: xmlElement.appendElement("dependencies")
    dependencies.appendElement("dependency").apply {
        appendElement("groupId", groupId)
        appendElement("artifactId", "$artifactId-$platformId")
        appendElement("version", version)
        appendElement("scope", "runtime")
    }
}

private fun Node.appendElement(
    tagName: String,
    textValue: String? = null
): org.w3c.dom.Element {
    val element = ownerDocument.createElement(tagName)
    appendChild(element)

    if (textValue != null) {
        val textNode = ownerDocument.createTextNode(textValue)
        element.appendChild(textNode)
    }

    return element
}

private fun Node.find(
    predicate: (Node) -> Boolean
): Node? {
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
 * Ensures that artifactIds are consistent when using configuration caching.
 * A workaround for https://github.com/gradle/gradle/issues/18369
 */
fun ensureConsistentJvmSuffix(
    xml: XmlProvider
) {
    val dependencies = xml.asElement().find {
        it.nodeName == "dependencies"
    } as? org.w3c.dom.Element ?: return

    dependencies.getElementsByTagName("dependency").forEach { dependency ->
        val artifactId = dependency.find { it.nodeName == "artifactId" }
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

internal const val KMP_ANCHOR_PUBLICATION_NAME = "androidxKmp"
