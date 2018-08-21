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

import org.gradle.api.internal.artifacts.result.DefaultResolvedArtifactResult

// The output folder inside prebuilts
val prebuiltsLocation = file("../../../../prebuilts/androidx")
val internalFolder = "internal"
val externalFolder = "external"

// Passed in as a project property
val artifactName: String by project

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

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    java
}

repositories {
    jcenter()
    mavenCentral()
    google()
}

dependencies {
    compile(artifactName)
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
fun supportingArtifacts(artifact: ResolvedArtifact): List<DefaultResolvedArtifactResult> {
    val supportingArtifacts = mutableListOf<DefaultResolvedArtifactResult>()
    val pomQuery = project.dependencies.createArtifactResolutionQuery()
    val pomQueryResult = pomQuery.forComponents(artifact.id.componentIdentifier)
            .withArtifacts(
                    MavenModule::class.java,
                    MavenPomArtifact::class.java)
            .execute()
    val pomResult = pomQueryResult.resolvedComponents.firstOrNull()
    // DefaultResolvedArtifactResult is an internal Gradle class.
    // However, it's being widely used anyway.
    val pomFile = pomResult?.getArtifacts(MavenPomArtifact::class.java)
            ?.firstOrNull()
            as? DefaultResolvedArtifactResult
    if (pomFile != null) {
        supportingArtifacts.add(pomFile)
    }

    // Create a seperate query for a sources. This is because, withArtifacts seems to be an AND.
    // So if artifacts only have a distributable without a source, we still want to copy the POM file.
    val sourcesQuery = project.dependencies.createArtifactResolutionQuery()
    val sourcesQueryResult = sourcesQuery.forComponents(artifact.id.componentIdentifier)
            .withArtifacts(
                    MavenModule::class.java,
                    SourcesArtifact::class.java)
            .execute()
    val sourcesResult = sourcesQueryResult.resolvedComponents.firstOrNull()
    val sourcesFile = sourcesResult?.getArtifacts(SourcesArtifact::class.java)
            ?.firstOrNull()
            as? DefaultResolvedArtifactResult
    if (sourcesFile != null) {
        supportingArtifacts.add(sourcesFile)
    }
    return supportingArtifacts
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
    println("Copying $artifact to $location")
    val supportingArtifacts = supportingArtifacts(artifact)
    // Copy main artifact
    copy {
        from(artifact.file)
        into(location)
    }
    // Copy supporting artifacts
    for (supportingArtifact in supportingArtifacts) {
        println("Copying $supportingArtifact to $location")
        copy {
            from(supportingArtifact.file)
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
                            configurations
                                    .compile
                                    .resolvedConfiguration
                                    .resolvedArtifacts)

            val externalLibraries =
                    configurations.compile.resolvedConfiguration
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
