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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.maven.model.Dependency
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuildingException
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.building.ModelSource
import org.apache.maven.model.resolution.ModelResolver
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
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
        classpath(gradleApi())
        classpath("org.apache.maven:maven-model:3.5.4")
        classpath("org.apache.maven:maven-model-builder:3.5.4")
        classpath("com.squareup.okhttp3:okhttp:3.11.0")
    }
}

typealias MavenListener = (String, String, String, File) -> Unit

/**
 * A Maven module resolver which uses Gradle.
 */
class MavenModuleResolver(val project: Project, val listener: MavenListener) : ModelResolver {

    override fun resolveModel(dependency: Dependency): ModelSource? {
        return resolveModel(dependency.groupId, dependency.artifactId, dependency.version)
    }

    override fun resolveModel(parent: Parent): ModelSource? {
        return resolveModel(parent.groupId, parent.artifactId, parent.version)
    }

    override fun resolveModel(
        groupId: String,
        artifactId: String,
        version: String
    ): ModelSource? {
        val pomQuery = project.dependencies.createArtifactResolutionQuery()
        val pomQueryResult = pomQuery.forModule(groupId, artifactId, version)
            .withArtifacts(
                MavenModule::class.java,
                MavenPomArtifact::class.java
            )
            .execute()
        var result: File? = null
        for (component in pomQueryResult.resolvedComponents) {
            val pomArtifacts = component.getArtifacts(MavenPomArtifact::class.java)
            for (pomArtifact in pomArtifacts) {
                val pomFile = pomArtifact as? ResolvedArtifactResult
                if (pomFile != null) {
                    result = pomFile.file
                    listener.invoke(groupId, artifactId, version, result)
                }
            }
        }
        return object : ModelSource {
            override fun getInputStream(): InputStream {
                return result!!.inputStream()
            }

            override fun getLocation(): String {
                return result!!.absolutePath
            }
        }
    }

    override fun addRepository(repository: Repository?) {
        // We don't need to support this
    }

    override fun addRepository(repository: Repository?, replace: Boolean) {
        // We don't need to support this
    }

    override fun newCopy(): ModelResolver {
        return this
    }
}

// The output folder inside prebuilts
val prebuiltsLocation = file("../../../../prebuilts/androidx")
val internalFolder = "internal"
val externalFolder = "external"
val configurationName = "fetchArtifacts"
val fetchArtifacts = configurations.create(configurationName)
val fetchArtifactsContainer = configurations.getByName(configurationName)
// Passed in as a project property
val artifactName = project.findProperty("artifactName")
val mediaType = MediaType.get("application/json; charset=utf-8")
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

repositories {
    jcenter()
    mavenCentral()
    google()
}

if (artifactName != null) {
    dependencies {
        // This is the configuration container that we use to lookup the
        // transitive closure of all dependencies.
        fetchArtifacts(artifactName)
    }
}

/**
 * Returns the list of libraries that are *internal*.
 */
fun filterInternalLibraries(artifacts: Set<ResolvedArtifact>): Set<ResolvedArtifact> {
    return artifacts.filter {
        val moduleVersionId = it.moduleVersion.id
        val group = moduleVersionId.group

        for (regex in internalArtifacts) {
            val match = regex.matches(group)
            if (match) {
                return@filter regex.matches(group)
            }
        }

        for (regex in potentialInternalArtifacts) {
            val matchResult = regex.matchEntire(group)
            val match = regex.matches(group) &&
                    matchResult?.destructured?.let { (sub) ->
                        !forceExternal.contains(sub)
                    } ?: true
            if (match) {
                return@filter true
            }
        }
        false
    }.toSet()
}

/**
 * Returns the supporting files (POM, Source files) for a given artifact.
 */
fun supportingArtifacts(
    artifact: ResolvedArtifact,
    internal: Boolean = false
): List<ResolvedArtifactResult> {
    val supportingArtifacts = mutableListOf<ResolvedArtifactResult>()
    val pomQuery = project.dependencies.createArtifactResolutionQuery()
    val modelBuilderFactory = DefaultModelBuilderFactory()
    val builder = modelBuilderFactory.newInstance()
    val resolver = MavenModuleResolver(project) { groupId, artifactId, version, pomFile ->
        copyPomFile(groupId, artifactId, version, pomFile, internal)
    }
    val pomQueryResult = pomQuery.forComponents(artifact.id.componentIdentifier)
        .withArtifacts(
            MavenModule::class.java,
            MavenPomArtifact::class.java
        )
        .execute()

    for (component in pomQueryResult.resolvedComponents) {
        val pomArtifacts = component.getArtifacts(MavenPomArtifact::class.java)
        for (pomArtifact in pomArtifacts) {
            val pomFile = pomArtifact as? ResolvedArtifactResult
            if (pomFile != null) {
                try {
                    val request: ModelBuildingRequest = DefaultModelBuildingRequest()
                    request.modelResolver = resolver
                    request.pomFile = pomFile.file
                    // Turn off validations becuase there are lots of bad POM files out there.
                    request.validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL
                    builder.build(request).effectiveModel
                } catch (exception: ModelBuildingException) {
                    println("Error building model request for $pomArtifact")
                }
                supportingArtifacts.add(pomFile)
            }
        }
    }

    // Create a separate query for a sources. This is because, withArtifacts seems to be an AND.
    // So if artifacts only have a distributable without a source, we still want to copy the POM file.
    val sourcesQuery = project.dependencies.createArtifactResolutionQuery()
    val sourcesQueryResult = sourcesQuery.forComponents(artifact.id.componentIdentifier)
        .withArtifacts(
            MavenModule::class.java,
            SourcesArtifact::class.java
        )
        .execute()

    if (sourcesQueryResult.resolvedComponents.size > 0) {
        for (component in sourcesQueryResult.resolvedComponents) {
            val sourcesArtifacts = component.getArtifacts(SourcesArtifact::class.java)
            for (sourcesArtifact in sourcesArtifacts) {
                val sourcesFile = sourcesArtifact as? ResolvedArtifactResult
                if (sourcesFile != null) {
                    supportingArtifacts.add(sourcesFile)
                }
            }
        }
    } else {
        project.logger.warn("No sources found for $artifact")
    }
    return supportingArtifacts
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
                    val payload = RequestBody.create(mediaType, "{\"url\": \"$url\"}")
                    val request = Request.Builder().url(licenseEndpoint).post(payload).build()
                    val response = client.newCall(request).execute()
                    val contents = response.body()?.string()
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
 * We are doing this for all internal libraries to account for -PuseMaxDepVersions which swaps out
 * the dependencies of all androidx libraries with their respective ToT versions.
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
fun copyArtifact(artifact: ResolvedArtifact, internal: Boolean = false) {
    val folder = if (internal) internalFolder else externalFolder
    val moduleVersionId = artifact.moduleVersion.id
    val group = moduleVersionId.group
    val groupPath = group.split(".").joinToString("/")
    val pathComponents = listOf(
        prebuiltsLocation,
        folder,
        groupPath,
        moduleVersionId.name,
        moduleVersionId.version
    )
    val location = pathComponents.joinToString("/")
    val supportingArtifacts = supportingArtifacts(artifact, internal = internal)
    // Copy main artifact
    println("Copying $artifact to $location")
    copy {
        from(
            artifact.file,
            digest(artifact.file, "MD5"),
            digest(artifact.file, "SHA1")
        )
        into(location)
    }
    // Copy supporting artifacts
    for (supportingArtifact in supportingArtifacts) {
        val file = supportingArtifact.file
        if (file.name.endsWith(".pom")) {
            copyPomFile(group, moduleVersionId.name, moduleVersionId.version, file, internal)
        } else {
            println("Copying $supportingArtifact to $location")
            copy {
                from(
                    supportingArtifact.file,
                    digest(supportingArtifact.file, "MD5"),
                    digest(supportingArtifact.file, "SHA1")
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
    val groupPath = group.split(".").joinToString("/")
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

tasks {
    val fetchArtifacts by creating {
        doLast {
            // Collect all the internal and external dependencies.
            // Copy the jar/aar's and their respective POM files.
            val internalLibraries =
                filterInternalLibraries(
                    fetchArtifactsContainer
                        .resolvedConfiguration
                        .resolvedArtifacts
                )

            val externalLibraries =
                fetchArtifactsContainer
                    .resolvedConfiguration
                    .resolvedArtifacts.filter {
                    val isInternal = internalLibraries.contains(it)
                    !isInternal
                }

            println("\r\nInternal Libraries")
            internalLibraries.forEach { library ->
                copyArtifact(library, internal = true)
            }

            println("\r\nExternal Libraries")
            externalLibraries.forEach { library ->
                copyArtifact(library, internal = false)
            }
            println("\r\nResolved artifacts for $artifactName.")
        }
    }
}

defaultTasks("fetchArtifacts")
