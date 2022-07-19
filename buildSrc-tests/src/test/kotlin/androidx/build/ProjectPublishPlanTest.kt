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
import androidx.build.buildInfo.computePublishPlan
import java.io.File
import java.util.Date
import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskDependency
import org.gradle.kotlin.dsl.named
import org.junit.Test

class ProjectPublishPlanTest {
    @Test
    fun noReleaseWithoutPublish() = buildInfoTest {
        val extension = createAndroidXExtension()
        extension.computePublishPlan().check { !it.shouldRelease }
    }

    @Test
    fun yesRelease() = buildInfoTest {
        val extension = createAndroidXExtension()
        extension.publish = Publish.SNAPSHOT_AND_RELEASE
        extension.computePublishPlan().check { it.shouldRelease }
    }

    @Test
    fun avoidShadowConfigs() = buildInfoTest {
        val extension = createAndroidXExtension()
        extension.publish = Publish.SNAPSHOT_AND_RELEASE
        subProject.configurations.register("runtimeElements") {
            setPublishableAttributes(it, "realArtifact")
        }
        subProject.configurations.register("shadowRuntimeElements") {
            setPublishableAttributes(it, "shadowArtifact")
            it.attributes.attribute(
                Bundling.BUNDLING_ATTRIBUTE,
                subProject.objects.named<Bundling>(Bundling.SHADOWED)
            )
        }
        extension.computePublishPlan().variants.map { it.artifactId }
            .check { it == listOf("realArtifact") }
    }

    private fun LibraryBuildInfoTestContext.setPublishableAttributes(
        it: Configuration,
        artifactName: String
    ) {
        it.attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            project.objects.named<Usage>(Usage.JAVA_RUNTIME)
        )
        it.attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            project.objects.named<Category>(Category.LIBRARY)
        )
        it.artifacts.add(stubArtifact(artifactName))
    }

    private fun stubArtifact(artifactName: String) = object : PublishArtifact {
        override fun getName(): String {
            return artifactName
        }

        override fun getBuildDependencies(): TaskDependency {
            TODO("Not yet implemented")
        }

        override fun getExtension(): String {
            TODO("Not yet implemented")
        }

        override fun getType(): String {
            TODO("Not yet implemented")
        }

        override fun getClassifier(): String? {
            TODO("Not yet implemented")
        }

        override fun getFile(): File {
            TODO("Not yet implemented")
        }

        override fun getDate(): Date? {
            TODO("Not yet implemented")
        }
    }

    private fun LibraryBuildInfoTestContext.createAndroidXExtension(): AndroidXExtension {
        writeLibraryVersionsFile(subProject.getSupportRootFolder(), listOf())

        return subProject.extensions.create(
            AndroidXImplPlugin.EXTENSION_NAME,
            AndroidXExtension::class.java,
            subProject
        )
    }
}