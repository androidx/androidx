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

import okhttp3.MediaType
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
    repositories {
        jcenter()
        mavenCentral()
        google()
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
// Passed in as a project property
val artifactName = project.findProperty("artifactName")
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

plugins {
    java
}

val metalavaBuildId: String? = findProperty("metalavaBuildId") as String?
repositories {
    jcenter()
    mavenCentral()
    google()
    gradlePluginPortal()
    if (metalavaBuildId != null) {
        maven(url="https://androidx.dev/metalava/builds/${metalavaBuildId}/artifacts/repo/m2repository")
    }

    val allowBintray: String? = findProperty("allowBintray") as String?
    if (allowBintray != null) {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/")
            metadataSources {
                artifact()
            }
        }
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-dev/")
            metadataSources {
                artifact()
            }
        }
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-eap/")
            metadataSources {
                artifact()
            }
        }
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlinx/")
            metadataSources {
                artifact()
            }
        }
    }

    val allowJetbrainsDev: String? = findProperty("allowJetbrainsDev") as String?
    if (allowJetbrainsDev != null) {
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

    ivy {
        setUrl("https://download.jetbrains.com/kotlin/native/builds/releases")
        patternLayout {
            artifact("[revision]/macos/[artifact]-[revision].[ext]")
        }
        metadataSources {
            artifact()
        }
        content {
            includeGroup("")
        }
    }
    ivy {
        setUrl("https://download.jetbrains.com/kotlin/native/builds/releases")
        patternLayout {
            artifact("[revision]/linux/[artifact]-[revision].[ext]")
        }
        metadataSources {
            artifact()
        }
        content {
            includeGroup("")
        }
    }
}

val gradleModuleMetadata: Configuration by configurations.creating {
    attributes {
        // We define this attribute in DirectMetadataAccessVariantRule
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION) )
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("gradle-module-metadata"))
    }
    extendsFrom(configurations.runtimeClasspath.get())
}

val allFilesWithDependencies: Configuration by configurations.creating {
    attributes {
        // We define this attribute in DirectMetadataAccessVariantRule
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("all-files-with-dependencies"))
    }
    extendsFrom(configurations.runtimeClasspath.get())
}

if (artifactName != null) {
    dependencies {
        // This is the configuration container that we use to lookup the
        // transitive closure of all dependencies.
        implementation(artifactName)

        // For metadata access
        components {
            all<DirectMetadataAccessVariantRule>()
        }
    }
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
    if (group != "") {
        return group.split(".").joinToString("/")
    } else {
        return "no-group"
    }
}

/**
 * This rule runs in a sandbox, and does not have access ot things in scope which it should usually
 * have access to. This is why the constant `all-files-with-dependencies` is being duplicated.
 */
@CacheableRule
open class DirectMetadataAccessVariantRule : ComponentMetadataRule {
    @javax.inject.Inject
    open fun getObjects(): ObjectFactory = throw UnsupportedOperationException()

    override fun execute(ctx: ComponentMetadataContext) {
        val id = ctx.details.id
        ctx.details.addVariant("moduleMetadata") {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, getObjects().named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, getObjects().named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, getObjects().named("gradle-module-metadata"))
            }
            withFiles {
                addFile("${id.name}-${id.version}.module")
            }
        }
        val variantNames = listOf(
            "runtimeElements",
            "releaseRuntimePublication",
            "metadata-api",
            "metadataApiElements-published",
            "runtime"
        )
        variantNames.forEach { name ->
            ctx.details.maybeAddVariant("allFilesWithDependencies${name.capitalize()}", name) {
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, getObjects().named(Usage.JAVA_RUNTIME))
                    attribute(Category.CATEGORY_ATTRIBUTE, getObjects().named(Category.DOCUMENTATION))
                    attribute(
                        DocsType.DOCS_TYPE_ATTRIBUTE,
                        getObjects().named("all-files-with-dependencies")
                    )
                }
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
}

tasks {
    val fetchArtifacts by creating {
        doLast {
            var numArtifactsFound = 0
            println("\r\nAll Files with Dependencies")
            allFilesWithDependencies.incoming.artifactView {
                lenient(true)
            }.artifacts.forEach {
                copyArtifact(it, internal = isInternalArtifact(it))
                numArtifactsFound++
            }
            gradleModuleMetadata.incoming.artifactView {
                lenient(true)
            }.artifacts.forEach {
                copyArtifact(it, internal = isInternalArtifact(it))
                numArtifactsFound++
            }
            if (numArtifactsFound < 1) {
                var message = "Artifact $artifactName not found!"
                if (metalavaBuildId != null) {
                    message += "\nMake sure that ab/$metalavaBuildId contains the `metalava` "
                    message += "target and that it has finished building, or see "
                    message += "ab/metalava-master for available build ids"
                }
                throw GradleException(message)
            }
	    println("\r\nResolved $numArtifactsFound artifacts for $artifactName.")
        }
    }
}

defaultTasks("fetchArtifacts")
