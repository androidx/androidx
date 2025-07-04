/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.build.gradle.internal.fixtures

import com.android.build.gradle.internal.profile.AnalyticsService
import com.android.build.gradle.internal.profile.ProjectData
import com.android.build.gradle.internal.profile.TaskMetadata
import com.android.build.gradle.internal.profile.TaskProfilingRecord
import com.android.builder.profile.NameAnonymizer
import com.android.builder.profile.NameAnonymizerSerializer
import com.android.builder.profile.Recorder
import com.google.wireless.android.sdk.stats.GradleBuildMemorySample
import com.google.wireless.android.sdk.stats.GradleBuildProfile
import com.google.wireless.android.sdk.stats.GradleBuildProfileSpan
import com.google.wireless.android.sdk.stats.GradleBuildProject
import com.google.wireless.android.sdk.stats.GradleBuildVariant
import com.google.wireless.android.sdk.stats.GradleTransformExecution
import java.io.File
import java.util.Base64
import java.util.concurrent.ConcurrentLinkedQueue
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.tooling.events.FinishEvent

/** A no-operation implementation of [AnalyticsService] for unit tests. */
class FakeNoOpAnalyticsService : AnalyticsService() {

    override fun getParameters(): Params {
        return object : Params {
            override val profile: Property<String>
                get() {
                    val profile = GradleBuildProfile.newBuilder().build().toByteArray()
                    return FakeGradleProperty(Base64.getEncoder().encodeToString(profile))
                }

            override val anonymizer: Property<String>
                get() = FakeGradleProperty(NameAnonymizerSerializer().toJson(NameAnonymizer()))

            override val projects: MapProperty<String, ProjectData>
                get() =
                    FakeObjectFactory.factory.mapProperty(
                        String::class.java,
                        ProjectData::class.java,
                    )

            override val enableProfileJson: Property<Boolean>
                get() = FakeGradleProperty(true)

            override val profileDir: Property<File?>
                get() = FakeGradleProperty()

            override val taskMetadata: MapProperty<String, TaskMetadata>
                get() =
                    FakeObjectFactory.factory.mapProperty(
                        String::class.java,
                        TaskMetadata::class.java,
                    )

            override val rootProjectPath: Property<String>
                get() = FakeGradleProperty("/path")

            override val applicationId: SetProperty<String>
                get() = FakeObjectFactory.factory.setProperty(String::class.java)
        }
    }

    override fun workerAdded(taskPath: String, workerKey: String) {}

    override fun workerStarted(taskPath: String, workerKey: String) {}

    override fun workerFinished(taskPath: String, workerKey: String) {}

    override fun registerSpan(taskPath: String, builder: GradleBuildProfileSpan.Builder) {}

    override fun getProjectBuillder(projectPath: String): GradleBuildProject.Builder {
        return GradleBuildProject.newBuilder()
    }

    override fun getVariantBuilder(
        projectPath: String,
        variantName: String,
    ): GradleBuildVariant.Builder {
        return GradleBuildVariant.newBuilder()
    }

    override fun getTaskRecord(taskPath: String): TaskProfilingRecord? {
        return null
    }

    override fun recordBlock(
        executionType: GradleBuildProfileSpan.ExecutionType,
        transform: GradleTransformExecution?,
        projectPath: String,
        variantName: String,
        block: Recorder.VoidBlock,
    ) {
        block.call()
    }

    override fun setConfigurationSpans(spans: ConcurrentLinkedQueue<GradleBuildProfileSpan>) {}

    override fun setInitialMemorySampleForConfiguration(sample: GradleBuildMemorySample) {}

    override fun close() {}

    override fun onFinish(finishEvent: FinishEvent?) {}
}
