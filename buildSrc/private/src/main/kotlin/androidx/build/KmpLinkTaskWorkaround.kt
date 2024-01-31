/*
 * Copyright 2023 The Android Open Source Project
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

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

/**
 * Name of the service we use to limit the number of concurrent kmp link tasks
 */
public const val KMP_LINK_SERVICE_NAME = "androidxKmpLinkService"

// service for limiting the number of concurrent kmp link tasks b/309990481
interface AndroidXKmpLinkService : BuildService<BuildServiceParameters.None>

fun Project.configureRootProjectForKmpLink() {
    project.gradle.sharedServices.registerIfAbsent(
        KMP_LINK_SERVICE_NAME,
        AndroidXKmpLinkService::class.java,
        { spec ->
            spec.maxParallelUsages.set(1)
        }
    )
}

object KmpLinkTaskWorkaround {
    // b/309990481
    fun serializeLinkTasks(
        project: Project
    ) {
        project.tasks.withType(
            KotlinNativeLink::class.java
        ).configureEach { task ->
            task.usesService(
                task.project.gradle.sharedServices.registrations
                    .getByName(KMP_LINK_SERVICE_NAME).service
            )
        }
    }
}
