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
    // User provided variant match pattern to schema location
    internal val schemaDirectories = mutableMapOf<VariantMatchName, Provider<String>>()
    // Used variant match pattern to its copy task. Multiple variant compile tasks can be finalized
    // by the same copy task.
    internal val copyTasks =
        mutableMapOf<VariantMatchName, TaskProvider<RoomGradlePlugin.RoomSchemaCopyTask>>()

    /**
     * Sets the schema location where Room will output exported schema files.
     *
     * The location specified will be used for all variants if per-variant schema locations are
     * needed use the overloaded version of this function that takes in a `variantMatchName`.
     *
     * See [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(path: String) {
        schemaDirectory(providers.provider { path })
    }

    /**
     * Sets the schema location where Room will output exported schema files.
     *
     * The location specified will be used for all variants if per-variant schema locations are
     * needed use the overloaded version of this function that takes in a `variantMatchName`.
     *
     * See [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(path: Provider<String>) {
        schemaDirectories[ALL_VARIANTS] = path
    }

    /**
     * Sets the schema location for a variant, flavor or build type where Room will output exported
     * schema files.
     *
     * The location specified will be used for a matching variants based on the provided
     * [variantMatchName] where it can either be a full variant name, a product flavor name or a
     * build type name.
     *
     * For example, assuming two build flavors: ‘demo’ and ‘full’ and the two default build types
     * ‘debug’ and ‘release’, then the following are valid configurations:
     * ```
     * room {
     *   // Applies 'demoDebug' only
     *   schemaLocation("demoDebug", ("$projectDir/schemas/demoDebug")
     *
     *   // Applies to 'demoDebug' and 'demoRelease'
     *   schemaLocation("demo", ("$projectDir/schemas/demo")
     *
     *   // Applies to 'demoDebug' and 'fullDebug'
     *   schemaLocation("debug", ("$projectDir/schemas/debug")
     * }
     * ```
     *
     * If per-variant schema locations are not necessary due to all variants containing the same
     * schema, then use the overloaded version of this function that does not take in a
     * `variantMatchName`.
     *
     * See [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(variantMatchName: String, path: String) {
        schemaDirectory(variantMatchName, providers.provider { path })
    }

    /**
     * Sets the schema location for a variant, flavor or build type where Room will output exported
     * schema files.
     *
     * The location specified will be used for a matching variants based on the provided
     * [variantMatchName] where it can either be a full variant name, a product flavor name or a
     * build type name.
     *
     * For example, assuming two build flavors: ‘demo’ and ‘full’ and the two default build types
     * ‘debug’ and ‘release’, then the following are valid configurations:
     * ```
     * room {
     *   // Applies 'demoDebug' only
     *   schemaLocation("demoDebug", ("$projectDir/schemas/demoDebug")
     *
     *   // Applies to 'demoDebug' and 'demoRelease'
     *   schemaLocation("demo", ("$projectDir/schemas/demo")
     *
     *   // Applies to 'demoDebug' and 'fullDebug'
     *   schemaLocation("debug", ("$projectDir/schemas/debug")
     * }
     * ```
     *
     * If per-variant schema locations are not necessary due to all variants containing the same
     * schema, then use the overloaded version of this function that does not take in a
     * `variantMatchName`.
     *
     * See [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(variantMatchName: String, path: Provider<String>) {
        check(variantMatchName.isNotEmpty()) { "variantMatchName must not be empty." }
        schemaDirectories[VariantMatchName(variantMatchName)] = path
    }

    /**
     * Represent a full variant name (demoDebug), flavor name (demo) or build type name (debug).
     */
    @JvmInline
    internal value class VariantMatchName(val actual: String)

    companion object {
        internal val ALL_VARIANTS = VariantMatchName("")
    }
}
