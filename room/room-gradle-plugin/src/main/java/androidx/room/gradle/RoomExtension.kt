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

import androidx.room.gradle.RoomGradlePlugin.Companion.capitalize
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

open class RoomExtension @Inject constructor(private val project: Project) {
    // User variant / target match pattern and its copy task. Multiple variant / target annotation
    // processing tasks can be finalized by the same copy task.
    internal val schemaConfigurations =
        project.objects.domainObjectSet(SchemaConfiguration::class.java)

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
    fun schemaDirectory(path: String) {
        schemaDirectory(project.providers.provider { path })
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
    fun schemaDirectory(path: Directory) {
        schemaDirectory(project.providers.provider { path })
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
    fun schemaDirectory(path: Provider<String>) {
        addSchemaConfiguration(ALL_MATCH, project.layout.dir(path.map { project.file(it) }))
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
    @JvmName("schemaDirectoryProvider") // To avoid JVM signature conflict due to erasure
    fun schemaDirectory(path: Provider<Directory>) {
        addSchemaConfiguration(ALL_MATCH, path)
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
    fun schemaDirectory(matchName: String, path: String) {
        schemaDirectory(matchName, project.providers.provider { path })
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
    fun schemaDirectory(matchName: String, path: Directory) {
        schemaDirectory(matchName, project.providers.provider { path })
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
    fun schemaDirectory(matchName: String, path: Provider<String>) {
        schemaDirectory(matchName, project.layout.dir(path.map { project.file(it) }))
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
    @JvmName("schemaDirectoryProvider") // To avoid JVM signature conflict due to erasure
    fun schemaDirectory(matchName: String, path: Provider<Directory>) {
        check(matchName.isNotEmpty()) { "variantMatchName must not be empty." }
        val key = MatchName(matchName)
        addSchemaConfiguration(key, path)
    }

    /** Causes Room annotation processor to generate Kotlin code instead of Java. */
    var generateKotlin: Boolean? = null

    private fun addSchemaConfiguration(matchName: MatchName, directory: Provider<Directory>) {
        val config =
            SchemaConfiguration(
                name = matchName,
                copyTask =
                    project.tasks.register(
                        "copyRoomSchemas${matchName.actual.capitalize()}",
                        RoomSchemaCopyTask::class.java
                    ) { task ->
                        task.schemaDirectory.set(directory)
                    }
            )
        schemaConfigurations.add(config)
    }

    /**
     * Represent a full Android variant name (demoDebug), flavor name (demo), build type name
     * (debug) or a target name (linux64, native, etc).
     */
    @JvmInline internal value class MatchName(val actual: String)

    /**
     * Represents a Room schema directory configuration. Storing the copy task that will gather
     * annotation processing generated schemas and move them to the user configured location.
     */
    internal class SchemaConfiguration(
        val name: MatchName,
        val copyTask: TaskProvider<RoomSchemaCopyTask>
    ) {
        fun matches(other: String?): Boolean {
            if (other == null) return false
            return this.name.actual == other
        }

        fun matches(other: MatchName?): Boolean {
            if (other == null) return false
            return this.name == other
        }

        override fun equals(other: Any?): Boolean {
            if (other !is SchemaConfiguration) return false
            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    companion object {
        internal val ALL_MATCH = MatchName("")
    }
}
