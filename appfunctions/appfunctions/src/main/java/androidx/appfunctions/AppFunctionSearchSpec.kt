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

package androidx.appfunctions

import androidx.appfunctions.metadata.AppFunctionMetadataDocument

/**
 * Defines the specifications for filtering and searching app function snapshots.
 *
 * @property packageNames A set of package names to filter functions by. Only functions belonging to
 *   these packages will be considered. Defaults to null, which means this field is ignored when
 *   filtering.
 *
 *   The calling app can only search metadata for functions in packages that it is allowed to query
 *   via [android.content.pm.PackageManager.canPackageQuery]. If a package is not queryable by the
 *   calling app, its functions' metadata will not be visible.
 *
 * @property schemaCategory The category of the function's schema. Defaults to null, which means
 *   this field is ignored when filtering.
 * @property schemaName The name of the function's schema. Defaults to null, which means this field
 *   is ignored when filtering.
 * @property minSchemaVersion The minimum version of the function's schema. Functions with a schema
 *   version equal to or greater than this value will be included when filtering. Defaults to 0,
 *   which means this field is ignored when filtering. This value cannot be negative.
 * @constructor Creates a new instance of [AppFunctionSearchSpec].
 */
public class AppFunctionSearchSpec
@JvmOverloads
constructor(
    @get:Suppress(
        // Null value is used to specify that the value was not set by the caller to be consistent
        // with other string fields.
        "NullableCollection"
    )
    public val packageNames: Set<String>? = null,
    public val schemaCategory: String? = null,
    public val schemaName: String? = null,
    public val minSchemaVersion: Int = 0,
) {
    init {
        require(minSchemaVersion >= 0) {
            "The minimum schema version must be a non-negative integer."
        }
    }

    /** Creates a search query for searching [AppFunctionMetadataDocument] from App Search. */
    internal fun toStaticMetadataAppSearchQuery(): String =
        buildList<String> {
                if (packageNames != null) {
                    check(packageNames.isNotEmpty()) {
                        "Cannot filter by empty set of package names."
                    }
                    add("packageName:(${getOrQueryExpression(packageNames)})")
                }

                if (schemaName != null) {
                    add("schemaName:\"${schemaName}\"")
                }

                if (schemaCategory != null) {
                    add("schemaCategory:\"${schemaCategory}\"")
                }

                if (minSchemaVersion > 0) {
                    add("schemaVersion>=${minSchemaVersion}")
                }
            }
            .joinToString(" ")

    private fun getOrQueryExpression(elements: Set<String>) =
        elements.joinToString(" OR ") { "\"$it\"" }
}
