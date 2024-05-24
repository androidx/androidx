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

package androidx.benchmark.darwin.gradle

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * A service that manages simulators / devices. Also manages booting up and tearing down instances
 */
interface XCodeBuildService : BuildService<BuildServiceParameters.None> {
    companion object {
        const val XCODE_BUILD_SERVICE_NAME = "DarwinXCodeBuildService"
    }
}

/** Register the [XCodeBuildService] as a shared gradle service. */
fun Project.configureXCodeBuildService() {
    gradle.sharedServices.registerIfAbsent(
        XCodeBuildService.XCODE_BUILD_SERVICE_NAME,
        XCodeBuildService::class.java
    ) { spec ->
        // Run one xcodebuild at a time.
        spec.maxParallelUsages.set(1)
    }
}
