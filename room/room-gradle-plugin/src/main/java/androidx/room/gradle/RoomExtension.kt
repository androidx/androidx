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

package androidx.room.gradle

import javax.inject.Inject
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider

open class RoomExtension @Inject constructor(private val providers: ProviderFactory) {
    // TODO(b/279748243): Consider adding overloads that takes `org.gradle.api.file.Directory`.
    // User provided variant / target match pattern to schema location
    internal val schemaDirectories = mutableMapOf<MatchName, Provider<String>>()
    // Used variant / target match pattern to its copy task. Multiple variant compile tasks can be
    // finalized by the same copy task.
    internal val copyTasks = mutableMapOf<MatchName, TaskProvider<RoomSchemaCopyTask>>()

    /**
     * Sets the schema location where Room will output exported schema files.
     *
     * The location specified will be used for all Android variants and Kotlin targets, if
     * per-variant / per-target schema locations are needed use the overloaded version of this
     * function that takes in a `matchName`.
     *
     * See
     * [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(path: String) {
        schemaDirectory(providers.provider { path })
    }

    /**
     * Sets the schema location where Room will output exported schema files.
     *
     * The location specified will be used for all Android variants and Kotlin targets, if
     * per-variant / per-target schema locations are needed use the overloaded version of this
     * function that takes in a `matchName`.
     *
     * See
     * [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(path: Provider<String>) {
        schemaDirectories[ALL_MATCH] = path
    }

    /**
     * Sets the schema location for an Android variant, flavor or build type or a Kotlin target
     * where Room will output exported schema files.
     *
     * The location specified will be used for matching Android variants or Kotlin targets based on
     * the provided [matchName] where it can be one of the following:
     * * A full Android variant name
     * * An Android product flavor name
     * * An Android build type name
     * * A Kotlin target
     *
     * For example, assuming two build flavors: ‘demo’ and ‘full’ and the two default build types
     * ‘debug’ and ‘release’, along with a 'native' KMP target, then the following are valid
     * configurations:
     * ```
     * room {
     *   // Applies to 'demoDebug' only
     *   schemaLocation("androidDemoDebug", "$projectDir/schemas/androidDemoDebug")
     *
     *   // Applies to 'demoDebug' and 'demoRelease'
     *   schemaLocation("androidDemo", "$projectDir/schemas/androidDemo")
     *
     *   // Applies to 'demoDebug' and 'fullDebug'
     *   schemaLocation("androidDebug", "$projectDir/schemas/androidDebug")
     *
     *   // Applies to 'native' only
     *   schemaLocation("native", "$projectDir/schemas/native")
     * }
     * ```
     *
     * If the project is not a Kotlin Multiplatform project, then the 'android' prefix can be
     * omitted for variant matching, i.e. 'demoDebug', 'demo' and 'debug' are valid match names for
     * the example configuration.
     *
     * If per-variant / per-target schema locations are not necessary due to all variants / targets
     * containing the same schema, then use the overloaded version of this function that does not
     * take in a `matchName`.
     *
     * See
     * [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(matchName: String, path: String) {
        schemaDirectory(matchName, providers.provider { path })
    }

    /**
     * Sets the schema location for an Android variant, flavor or build type or a Kotlin target
     * where Room will output exported schema files.
     *
     * The location specified will be used for matching Android variants or Kotlin targets based on
     * the provided [matchName] where it can be one of the following:
     * * A full Android variant name
     * * An Android product flavor name
     * * An Android build type name
     * * A Kotlin target
     *
     * For example, assuming two build flavors: ‘demo’ and ‘full’ and the two default build types
     * ‘debug’ and ‘release’, along with a 'native' KMP target, then the following are valid
     * configurations:
     * ```
     * room {
     *   // Applies to 'demoDebug' only
     *   schemaLocation("androidDemoDebug", "$projectDir/schemas/androidDemoDebug")
     *
     *   // Applies to 'demoDebug' and 'demoRelease'
     *   schemaLocation("androidDemo", "$projectDir/schemas/androidDemo")
     *
     *   // Applies to 'demoDebug' and 'fullDebug'
     *   schemaLocation("androidDebug", "$projectDir/schemas/androidDebug")
     *
     *   // Applies to 'native' only
     *   schemaLocation("native", "$projectDir/schemas/native")
     * }
     * ```
     *
     * If the project is not a Kotlin Multiplatform project, then the 'android' prefix can be
     * omitted for variant matching, i.e. 'demoDebug', 'demo' and 'debug' are valid match names for
     * the example configuration.
     *
     * If per-variant / per-target schema locations are not necessary due to all variants / targets
     * containing the same schema, then use the overloaded version of this function that does not
     * take in a `matchName`.
     *
     * See
     * [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(matchName: String, path: Provider<String>) {
        check(matchName.isNotEmpty()) { "variantMatchName must not be empty." }
        schemaDirectories[MatchName(matchName)] = path
    }

    /** Causes Room annotation processor to generate Kotlin code instead of Java. */
    open var generateKotlin: Boolean? = null

    /**
     * Represent a full Android variant name (demoDebug), flavor name (demo), build type name
     * (debug) or a target name (linux64, native, etc).
     */
    @JvmInline internal value class MatchName(val actual: String)

    companion object {
        internal val ALL_MATCH = MatchName("")

        internal fun <V> Map<MatchName, V>.findPair(key: String) =
            MatchName(key).let { if (containsKey(it)) it to getValue(it) else null }
    }
}
