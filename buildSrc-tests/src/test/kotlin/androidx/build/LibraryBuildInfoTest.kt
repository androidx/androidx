/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.build.LibraryBuildInfoTestContext.Companion.buildInfoTest
import androidx.build.buildInfo.CreateAggregateLibraryBuildInfoFileTask
import androidx.build.buildInfo.CreateAggregateLibraryBuildInfoFileTask.Companion.CREATE_AGGREGATE_BUILD_INFO_FILES_TASK
import androidx.build.buildInfo.CreateLibraryBuildInfoFileTask
import androidx.build.buildInfo.ProjectPublishPlan
import androidx.build.buildInfo.ProjectPublishPlan.VariantPublishPlan
import androidx.build.buildInfo.addCreateLibraryBuildInfoFileTasksAfterEvaluate
import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.tasks.TaskProvider
import org.junit.Test

class LibraryBuildInfoTest {
    private val stubShaProvider = DefaultProvider { "stubSha" }

    @Test
    fun addTaskForSimpleSubProjectAndAddToAggregate() = buildInfoTest {
        val aggregateTask = createAggregateBuildInfoTask()

        with(AndroidXImplPlugin(stubComponentFactory())) {
            val info = myGroupReleaseInfo(subProject)
            subProject.addCreateLibraryBuildInfoFileTasksAfterEvaluate(info, stubShaProvider)
        }

        aggregateTask.dependencyNames().check { it.contains("createLibraryBuildInfoFiles") }
    }

    @Test
    fun addTaskForCompoundSubProjectAndAddToAggregate() = buildInfoTest {
        val aggregateTask = createAggregateBuildInfoTask()

        subProject.configurations.register("someOtherConfiguration") { config ->
            config.dependencies.add(
                DefaultExternalModuleDependency(
                    "androidx.specialGroup",
                    "specialArtifact",
                    "4.5.6"
                )
            )
        }

        with(AndroidXImplPlugin(stubComponentFactory())) {
            subProject.addCreateLibraryBuildInfoFileTasksAfterEvaluate(
                ProjectPublishPlan(
                    shouldRelease = true,
                    mavenGroup = LibraryGroup(group = "myGroup", atomicGroupVersion = null),
                    variants = listOf(
                        subProject.stubVariantPublishPlan,
                        VariantPublishPlan(
                            artifactId = "artifact-jvm",
                            taskSuffix = "Jvm",
                            configurationName = "someOtherConfiguration"
                        )
                    )
                ), stubShaProvider
            )
        }

        aggregateTask.dependencyNames().check {
            it.containsAll(
                listOf(
                    "createLibraryBuildInfoFiles", "createLibraryBuildInfoFilesJvm"
                )
            )
        }

        getBuildInfoTask(name = "createLibraryBuildInfoFiles").let { task ->
            task.artifactId.get().check { it == "subproject" }
            task.dependencyList.get().check { it.isEmpty() }
        }

        getBuildInfoTask(name = "createLibraryBuildInfoFilesJvm").let { task ->
            task.artifactId.get().check { it == "artifact-jvm" }
            task.dependencyList.get().single().let { dep ->
                dep.check { it.groupId == "androidx.specialGroup" }
                dep.check { it.artifactId == "specialArtifact" }
                dep.check { it.version == "4.5.6" }
            }
        }
    }

    private fun LibraryBuildInfoTestContext.getBuildInfoTask(name: String) =
        subProject.tasks.findByName(name) as CreateLibraryBuildInfoFileTask

    private fun TaskProvider<*>.dependencyNames() =
        get().dependsOn.map { it as TaskProvider<*> }.map { it.get().name }

    private fun myGroupReleaseInfo(project: Project) = ProjectPublishPlan(
        shouldRelease = true,
        mavenGroup = LibraryGroup(group = "myGroup", atomicGroupVersion = null),
        variants = listOf(project.stubVariantPublishPlan)
    )

    private val Project.stubVariantPublishPlan: VariantPublishPlan
        get() = VariantPublishPlan(artifactId = name)

    private fun stubComponentFactory(): (String) -> AdhocComponentWithVariants =
        { TODO("Should never be called in these tests") }

    private fun LibraryBuildInfoTestContext.createAggregateBuildInfoTask() =
        subProject.tasks.register(
            CREATE_AGGREGATE_BUILD_INFO_FILES_TASK,
            CreateAggregateLibraryBuildInfoFileTask::class.java
        )
}