/*
 * Copyright 2026 The Android Open Source Project
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

package org.jetbrains.androidx.build

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * A lock to prevent parallel access to "xcodebuild", because it causes a race:
 * "error: unable to attach DB: unable to initialize database (database is locked)"
 *
 * Works as a service with max usages = 1
 *
 * Usage inside a task:
 * ```
 * usesService(XcodeBuildLock.instance(project))
 * ```
 */
abstract class XcodeBuildLock : BuildService<BuildServiceParameters.None> {
    companion object {
        @JvmStatic
        fun instance(project: Project): Provider<XcodeBuildLock> =
            project.gradle.sharedServices.registerIfAbsent("xcodeBuildLock", XcodeBuildLock::class.java) {
                it.maxParallelUsages.set(1)
            }
    }
}
