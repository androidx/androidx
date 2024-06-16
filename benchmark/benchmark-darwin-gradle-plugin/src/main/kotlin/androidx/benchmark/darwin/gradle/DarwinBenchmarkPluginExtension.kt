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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional

/** The [DarwinBenchmarkPlugin] extension. */
abstract class DarwinBenchmarkPluginExtension {
    /** The path to the YAML file that can be used to generate the XCode project. */
    abstract val xcodeGenConfigFile: RegularFileProperty

    /** The project name as defined in the YAML file. */
    abstract val xcodeProjectName: Property<String>

    /** The project scheme as defined in the YAML file that is the unit test target. */
    abstract val scheme: Property<String>

    /**
     * The benchmark device `id`. This is typically discovered by using `xcrun xctrace list
     * devices`.
     */
    abstract val destination: Property<String>

    /**
     * The reference sha for the source code being benchmarked. This can be useful when tracking
     * regressions.
     */
    @get:Optional abstract val referenceSha: Property<String>
}
