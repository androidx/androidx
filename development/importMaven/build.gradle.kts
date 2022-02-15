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

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

buildscript {
    // Set of repositories only used for this build.gradle.kts itself. These are not used
    // for fetching artifacts
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.apache.maven:maven-model:3.5.4")
        classpath("org.apache.maven:maven-model-builder:3.5.4")
        classpath("com.squareup.okhttp3:okhttp:4.8.1")
        classpath("javax.inject:javax.inject:1")
    }
}

// The output folder inside prebuilts
val prebuiltsLocation = file("../../../../prebuilts/androidx")
val internalFolder = "internal"
val externalFolder = "external"
/**
 * The coordinates of the desired artifact to be fetched with all of its dependencies, specified
 * to the script via -PartifactNames="foo:bar:1.0" or -PartifactNames="foo:bar:1.0,foz:baz:1.0"
 */
var artifactNames = project.findProperty("artifactNames")?.toString()?.split(",")
    ?: throw GradleException("You are required to specify -PartifactNames property")
val mediaType = "application/json; charset=utf-8".toMediaType()
val licenseEndpoint = "https://fetch-licenses.appspot.com/convert/licenses"

val internalArtifacts = listOf(
    "android.arch(.*)?".toRegex(),
    "com.android.support(.*)?".toRegex()
)

val potentialInternalArtifacts = listOf(
    "androidx(.*)?".toRegex()
)

// Need to exclude androidx.databinding
val forceExternal = setOf(
    ".databinding"
)

// Apply java plugin so we have a base project set up with runtime configuration and ability to
// add our requested dependency passed it through the gradle property -PartifactNames="foo:bar:1.0"
plugins {
    java
    alias(libs.plugins.kotlinMp)
}

kotlin {
       linuxX64()
}

val metalavaBuildId: String? = findProperty("metalavaBuildId") as String?
// Set up repositories from this to fetch the artifacts
repositories {
    // Metalava has to be first on the list because it is also published on google() and we
    // sometimes re-use versions
    if (metalavaBuildId != null) {
        maven(url="https://androidx.dev/metalava/builds/${metalavaBuildId}/artifacts/repo/m2repository")
    }
    mavenCentral()
    google()
    gradlePluginPortal()

    val allowJetbrainsDev: String? = findProperty("allowJetbrainsDev") as String?
    if (allowJetbrainsDev != null) {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/")
            metadataSources {
                artifact()
            }
        }
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
            metadataSources {
                artifact()
            }
        }
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
            metadataSources {
                artifact()
            }
        }
    }

    listOf("macos", "macos-x86_64", "linux", "linux-x86_64").forEach { platstring ->
        ivy {
            setUrl("https://download.jetbrains.com/kotlin/native/builds/releases")
            patternLayout {
                artifact("[revision]/$platstring/[artifact]-[revision].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup("")
            }
        }
    }
}

/**
 * Configuration used to fetch the requested dependency's Java Runtime dependencies.
 */
val javaRuntimeConfiguration: Configuration by configurations.creating {
    attributes {
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(LibraryElements.JAR)
        )
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))

    }
    extendsFrom(configurations.runtimeClasspath.get())
}

/**
 * Configuration used to fetch the requested dependency's Java API dependencies.
 */
val javaApiConfiguration: Configuration by configurations.creating {
    attributes {
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(LibraryElements.JAR)
        )
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))

    }
    extendsFrom(configurations.runtimeClasspath.get())
}

/**
 * Configuration used to fetch the requested dependency's Kotlin API dependencies.
 * (klib, etc)
 */
val kotlinApiConfiguration: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
    }
    extendsFrom(configurations.runtimeClasspath.get())
}

dependencies {
    // We reuse the runtimeClasspath configuration provided by the java gradle plugin by
    // extending it in our directDependencyConfiguration and allDependenciesConfiguration,
    // so we can look up transitive closure of all the dependencies.
    for (artifactName in artifactNames) {
        implementation(artifactName)
    }

    // Specify to use our custom variant rule that sets up to fetch exactly the files
    // we care about
    components {
        all<DirectMetadataAccessVariantRule>()
    }
    // Specify that both AARs and Jars are compatible
    attributesSchema.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE)
        .compatibilityRules.add(JarAndAarAreCompatible::class.java)
}

/**
 * Checks if an artifact is *internal*.
 */
fun isInternalArtifact(artifact: ResolvedArtifactResult): Boolean {
    val component = artifact.id.componentIdentifier as? ModuleComponentIdentifier
    if (component != null) {
        val group = component.group
        for (regex in internalArtifacts) {
            val match = regex.matches(group)
            if (match) {
                return true
            }
        }

        for (regex in potentialInternalArtifacts) {
            val matchResult = regex.matchEntire(group)
            val match = regex.matches(group) &&
                    matchResult?.destructured?.let { (sub) ->
                        !forceExternal.contains(sub)
                    } ?: true
            if (match) {
                return true
            }
        }
    }
    return false
}

/**
 * Helps generate digests for the artifacts.
 */
fun digest(file: File, algorithm: String): File {
    val messageDigest = MessageDigest.getInstance(algorithm)
    val contents = file.readBytes()
    val digestBytes = messageDigest.digest(contents)
    val builder = StringBuilder()
    for (byte in digestBytes) {
        builder.append(String.format("%02x", byte))
    }
    val parent = System.getProperty("java.io.tmpdir")
    val outputFile = File(parent, "${file.name}.${algorithm.toLowerCase()}")
    outputFile.deleteOnExit()
    outputFile.writeText(builder.toString())
    return outputFile
}

/**
 * Fetches license information for external dependencies.
 */
fun licenseFor(pomFile: File): File? {
    try {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.parse(pomFile)
        val client = OkHttpClient()
        /*
          This is what a licenses declaration looks like:
          <licenses>
            <license>
              <name>Android Software Development Kit License</name>
              <url>https://developer.android.com/studio/terms.html</url>
              <distribution>repo</distribution>
            </license>
          </licenses>
         */
        val licenses = document.getElementsByTagName("license")
        for (i in 0 until licenses.length) {
            val license = licenses.item(i)
            val children = license.childNodes
            for (j in 0 until children.length) {
                val element = children.item(j)
                if (element.nodeName.toLowerCase() == "url") {
                    val url = element.textContent
                    val payload = "{\"url\": \"$url\"}".toRequestBody(mediaType)
                    val request = Request.Builder().url(licenseEndpoint).post(payload).build()
                    val response = client.newCall(request).execute()
                    val contents = response.body?.string()
                    if (contents != null) {
                        val parent = System.getProperty("java.io.tmpdir")
                        val outputFile = File(parent, "${pomFile.name}.LICENSE")
                        outputFile.deleteOnExit()
                        outputFile.writeText(contents)
                        return outputFile
                    }
                }
            }
        }
    } catch (exception: Throwable) {
        println("Error fetching license information for $pomFile")
    }
    return null
}

/**
 * Transforms POM files so we automatically comment out nodes with <type>aar</type>.
 *
 * We are doing this for all internal libraries to account for -Pandroidx.useMaxDepVersions
 * which swaps out the dependencies of all androidx libraries with their respective ToT versions.
 * For more information look at b/127495641.
 */
fun transformInternalPomFile(file: File): File {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(file)
    document.normalizeDocument()

    val container = document.getElementsByTagName("dependencies")
    if (container.length <= 0) {
        return file
    }

    fun findTypeAar(dependency: Node): Element? {
        val children = dependency.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                if (element.tagName.toLowerCase() == "type" &&
                    element.textContent?.toLowerCase() == "aar"
                ) {
                    return element
                }
            }
        }
        return null
    }

    for (i in 0 until container.length) {
        val dependencies = container.item(i)
        for (j in 0 until dependencies.childNodes.length) {
            val dependency = dependencies.childNodes.item(j)
            val element = findTypeAar(dependency)
            if (element != null) {
                val replacement = document.createComment("<type>aar</type>")
                dependency.replaceChild(replacement, element)
            }
        }
    }

    val parent = System.getProperty("java.io.tmpdir")
    val outputFile = File(parent, "${file.name}.transformed")
    outputFile.deleteOnExit()

    val transformer = TransformerFactory.newInstance().newTransformer()
    val domSource = DOMSource(document)
    val result = StreamResult(outputFile)
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "true")
    transformer.setOutputProperty(OutputKeys.INDENT, "true")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.transform(domSource, result)
    return outputFile
}

/**
 * Copies artifacts to the right locations.
 */
fun copyArtifact(artifact: ResolvedArtifactResult, internal: Boolean = false) {
    val folder = if (internal) internalFolder else externalFolder
    val file = artifact.file
    val component = artifact.id.componentIdentifier as? ModuleComponentIdentifier
    if (component != null) {
        val group = component.group
        val moduleName = component.module
        val moduleVersion = component.version
        val groupPath = groupToPath(group)
        val pathComponents = listOf(
            prebuiltsLocation,
            folder,
            groupPath,
            moduleName,
            moduleVersion
        )
        val location = pathComponents.joinToString("/")
        if (file.name.endsWith(".pom")) {
            copyPomFile(group, moduleName, moduleVersion, file, internal)
        } else {
            println("Copying ${file.name} to $location")
            copy {
                from(
                    file,
                    digest(file, "MD5"),
                    digest(file, "SHA1")
                )
                into(location)
            }
        }
    }
}

/**
 * Copies associated POM files to the right location.
 */
fun copyPomFile(
    group: String,
    name: String,
    version: String,
    pomFile: File,
    internal: Boolean = false
) {
    val folder = if (internal) internalFolder else externalFolder
    val groupPath = groupToPath(group)
    val pathComponents = listOf(
        prebuiltsLocation,
        folder,
        groupPath,
        name,
        version
    )
    val location = pathComponents.joinToString("/")
    // Copy associated POM files.
    val transformed = if (internal) transformInternalPomFile(pomFile) else pomFile
    println("Copying ${pomFile.name} to $location")
    copy {
        from(transformed)
        into(location)
        rename {
            pomFile.name
        }
    }
    // Keep original MD5 and SHA1 hashes
    copy {
        from(
            digest(pomFile, "MD5"),
            digest(pomFile, "SHA1")
        )
        into(location)
    }
    // Copy licenses if available for external dependencies
    val license = if (!internal) licenseFor(pomFile) else null
    if (license != null) {
        println("Copying License files for ${pomFile.name} to $location")
        copy {
            from(license)
            into(location)
            // rename to a file called LICENSE
            rename { "LICENSE" }
        }
    }
}

/**
 * Given a groupId, returns a relative filepath telling where to place that group
 */
fun groupToPath(group: String): String {
    return if (group != "") {
        group.split(".").joinToString("/")
    } else {
        "no-group"
    }
}

/**
 * A [AttributeCompatibilityRule] that makes Gradle consider both aar and jar as compatible
 * artifacts (by default it would only want `jar` as this build.gradle.kts project is `java`)
 */
@CacheableRule
abstract class JarAndAarAreCompatible : AttributeCompatibilityRule<LibraryElements> {
    override fun execute(t: CompatibilityCheckDetails<LibraryElements>) {
        val consumer = t.consumerValue ?: return
        val producer = t.producerValue ?: return

        if (consumer.name.compareTo("jar", ignoreCase = true) == 0 &&
                producer.name.compareTo("aar", ignoreCase = true) == 0
            ) {
                t.compatible()
            } else if (consumer.name == producer.name) {
                t.compatible()
            }
    }
}

/**
 * A [ComponentMetadataRule] that allows us to pick which files we want to download.
 */
@CacheableRule
open class DirectMetadataAccessVariantRule : ComponentMetadataRule {
    @javax.inject.Inject
    open fun getObjects(): ObjectFactory = throw UnsupportedOperationException()

    override fun execute(ctx: ComponentMetadataContext) {
        val id = ctx.details.id
        ctx.details.allVariants {
            // With files lambda is executed for every dependency in the Gradle dependency graph
            // that was resolved using the specified attributes.
            withFiles {
                addFile("${id.name}-${id.version}.pom")
                addFile("${id.name}-${id.version}.pom.asc")
                addFile("${id.name}-${id.version}.module")
                addFile("${id.name}-${id.version}.module.asc")
                addFile("${id.name}-${id.version}.jar")
                addFile("${id.name}-${id.version}.jar.asc")
                addFile("${id.name}-${id.version}.aar")
                addFile("${id.name}-${id.version}.aar.asc")
                addFile("${id.name}-${id.version}-sources.jar")
                addFile("${id.name}-${id.version}.klib")
                addFile("${id.name}-${id.version}.klib.asc")
                addFile("${id.name}-${id.version}-cinterop-interop.klib")
                addFile("${id.name}-${id.version}-cinterop-interop.klib.asc")
            }
        }
    }
}
tasks.register("fetchArtifacts") {
    doLast {
        var numArtifactsFound = 0
        println("\r\nAll Files with Dependencies")
        val copiedArtifacts = mutableSetOf<File>()
        javaRuntimeConfiguration.incoming.artifactView {
            // We need to be lenient because we are requesting files that might not exist.
            // For example source.jar or .asc.
            lenient(true)
        }.artifacts.forEach {
            copiedArtifacts.add(it.file)
            copyArtifact(it, internal = isInternalArtifact(it))
            numArtifactsFound++
        }
        javaApiConfiguration.incoming.artifactView {
            // We need to be lenient because we are requesting files that might not exist.
            // For example source.jar or .asc.
            lenient(true)
        }.artifacts.forEach {
            if (copiedArtifacts.contains(it.file)) return@forEach
            copyArtifact(it, internal = isInternalArtifact(it))
            numArtifactsFound++
        }

        // catch any artifacts that are needed to resolve this as a non-JVM
        // Kotlin dependency
        kotlinApiConfiguration.incoming.artifactView {
            // We need to be lenient because we are requesting files that might not exist.
            lenient(true)
        }.artifacts.forEach {
            if (copiedArtifacts.contains(it.file)) return@forEach
            copyArtifact(it, internal = isInternalArtifact(it))
            numArtifactsFound++
        }

        if (numArtifactsFound < 1) {
            var message = "Artifact(s) ${artifactNames.joinToString { it }} not found!"
            if (metalavaBuildId != null) {
                message += "\nMake sure that ab/$metalavaBuildId contains the `metalava` "
                message += "target and that it has finished building, or see "
                message += "ab/metalava-master for available build ids"
            }
            throw GradleException(message)
        }
        println("\r\nResolved $numArtifactsFound artifacts for ${artifactNames.joinToString { it }}.")
    }
}

defaultTasks("fetchArtifacts")
