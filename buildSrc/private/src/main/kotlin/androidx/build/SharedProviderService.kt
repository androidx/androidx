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

package androidx.build

import androidx.build.gitclient.createHeadShaProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * This build service allows us to share a single instance of a provider across projects if we know
 * that the value of it will be **identical** for all projects.
 */
abstract class SharedProviderService : BuildService<SharedProviderService.Parameters> {
    interface Parameters : BuildServiceParameters {
        var headShaProvider: Provider<String>
    }

    fun getHeadShaProvider(): Provider<String> = parameters.headShaProvider

    companion object {
        internal fun registerOrGet(project: Project): SharedProviderService {
            return project.gradle.sharedServices
                .registerIfAbsent("sharedProviderService", SharedProviderService::class.java) { spec
                    ->
                    spec.parameters.headShaProvider = project.createHeadShaProvider()
                }
                .get()
        }
    }
}
