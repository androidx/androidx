/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal

@CacheableTask
open class ComposePublishingTask : DefaultTask() {
    @get:Internal
    lateinit var repository: String

    @get:Internal
    lateinit var composeProperties: ComposeProperties

    private val targetPlatforms: Set<ComposePlatforms> by lazy {
        composeProperties.targetPlatforms
    }

    fun dependsOnComposeTask(task: String) {
        dependsOn(task)
    }

    fun publish(rootProject: Project, component: ComposeComponent) {
        if (component.customTasks.isNotEmpty()) {
            publish(
                component.path,
                onlyWithPlatforms = component.supportedPlatforms,
                publications = component.customTasks
            )
        } else {
            publishMultiplatform(rootProject, component)
        }
    }

    private fun publish(
        project: String,
        publications: Collection<String>
    ) {
        for (publication in publications) {
            dependsOnComposeTask("$project:publish${publication}PublicationTo$repository")
        }
        dependsOnComposeTask("$project:jbVerifyDependencyVersions")
    }

    private fun publish(
        project: String,
        publications: Collection<String>,
        onlyWithPlatforms: Set<ComposePlatforms>
    ) {
        if (onlyWithPlatforms.any { it in targetPlatforms }) {
            publish( project, publications)
        }
    }

    private fun publishMultiplatform(rootProject: Project, component: ComposeComponent) {
        val project = rootProject.findProject(component.path) ?:
            throw IllegalArgumentException("Cannot find project ${component.path}")

        dependsOnComposeTask("${component.path}:publish${ComposePlatforms.KotlinMultiplatform.name}PublicationTo$repository")

        for (platform in component.supportedPlatforms) {
            if (platform !in targetPlatforms) continue

            // Fall back to a platform's alternative names if the primary task doesn't exist.
            // Some canonical stubs declare `jvm()` instead of `desktop()` (e.g. annotation,
            // collection, lifecycle-common); their publish task is then
            // `publishJvmPublicationToMavenLocal`, not `publishDesktopPublicationToMavenLocal`.
            val publicationName = resolvePublicationName(project, platform, repository)
            dependsOnComposeTask("${component.path}:publish${publicationName}PublicationTo$repository")
        }
        dependsOnComposeTask("${component.path}:jbVerifyDependencyVersions")
    }

    private fun resolvePublicationName(
        project: Project,
        platform: ComposePlatforms,
        repository: String,
    ): String {
        val candidates = listOf(platform.name) + platform.alternativeNames
        for (name in candidates) {
            if (project.tasks.findByName("publish${name}PublicationTo$repository") != null) {
                return name
            }
        }
        return platform.name
    }
}