/*
 * Copyright 2025 The Android Open Source Project
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
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

/** Name of the service we use to limit the number of concurrent yarn tasks */
public const val YARN_LOCK_SERVICE = "androidxYarnLockService"

interface YarnLockService : BuildService<BuildServiceParameters.None>

fun Project.configureRootProjectForYarn() {
    project.gradle.sharedServices.registerIfAbsent(
        YARN_LOCK_SERVICE,
        YarnLockService::class.java,
        { spec -> spec.maxParallelUsages.set(1) },
    )
    project.tasks.withType(KotlinNpmInstallTask::class.java).configureEach { task ->
        task.usesService(
            task.project.gradle.sharedServices.registrations.getByName(YARN_LOCK_SERVICE).service
        )
    }
}
