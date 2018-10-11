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

import java.security.MessageDigest
import org.gradle.api.artifacts.result.ResolvedArtifactResult

// The output folder inside prebuilts
val prebuiltsLocation = file("../../../../prebuilts/androidx")
val internalFolder = "internal"
val externalFolder = "external"
val configurationName = "fetchArtifacts"
val fetchArtifacts = configurations.create(configurationName)
val fetchArtifactsContainer = configurations.getByName(configurationName)
// Passed in as a project property
val artifactName = project.findProperty("artifactName")

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
fun supportingArtifacts(artifact: ResolvedArtifact): List<ResolvedArtifactResult> {
    val supportingArtifacts = mutableListOf<ResolvedArtifactResult>()
    val pomQuery = project.dependencies.createArtifactResolutionQuery()
    val pomQueryResult = pomQuery.forComponents(artifact.id.componentIdentifier)
            .withArtifacts(
                    MavenModule::class.java,
                    MavenPomArtifact::class.java)
            .execute()

    for (component in pomQueryResult.resolvedComponents) {
        // DefaultResolvedArtifactResult is an internal Gradle class.
        // However, it's being widely used anyway.
        val pomArtifacts = component.getArtifacts(MavenPomArtifact::class.java)
        for (pomArtifact in pomArtifacts) {
            val pomFile = pomArtifact as? ResolvedArtifactResult
            if (pomFile != null) {
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
                    SourcesArtifact::class.java)
            .execute()

    for (component in sourcesQueryResult.resolvedComponents) {
        val sourcesArtifacts = component.getArtifacts(SourcesArtifact::class.java)
        for (sourcesArtifact in sourcesArtifacts) {
            val sourcesFile = sourcesArtifact as? ResolvedArtifactResult
            if (sourcesFile != null) {
                supportingArtifacts.add(sourcesFile)
            }
        }
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
    outputFile.deleteOnExit()
    return outputFile
}

/**
 * Copies artifacts to the right locations.
 */
fun copyLibrary(artifact: ResolvedArtifact, internal: Boolean = false) {
    val folder = if (internal) internalFolder else externalFolder
    val moduleVersionId = artifact.moduleVersion.id
    val group = moduleVersionId.group
    val groupPath = group.split(".").joinToString("/")
    val pathComponents = listOf(prebuiltsLocation,
            folder,
            groupPath,
            moduleVersionId.name,
            moduleVersionId.version)
    val location = pathComponents.joinToString("/")
    val supportingArtifacts = supportingArtifacts(artifact)
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
    copy {
        into(location)
    }
    // Copy supporting artifacts
    for (supportingArtifact in supportingArtifacts) {
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

tasks {
    val fetchArtifacts by creating {
        doLast {
            // Collect all the internal and external dependencies.
            // Copy the jar/aar's and their respective POM files.
            val internalLibraries =
                    filterInternalLibraries(
                        fetchArtifactsContainer
                                    .resolvedConfiguration
                                    .resolvedArtifacts)

            val externalLibraries =
                    fetchArtifactsContainer
                            .resolvedConfiguration
                            .resolvedArtifacts.filter {
                        val isInternal = internalLibraries.contains(it)
                        !isInternal
                    }

            println("\r\nInternal Libraries")
            internalLibraries.forEach { library ->
                copyLibrary(library, internal = true)
            }

            println("\r\nExternal Libraries")
            externalLibraries.forEach { library ->
                copyLibrary(library, internal = false)
            }
            println("\r\nResolved artifacts for $artifactName.")
        }
    }
}

defaultTasks("fetchArtifacts")
