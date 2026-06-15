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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appfunctions.metadata.AppFunctionName

/**
 * Defines the specifications for filtering and searching app function snapshots.
 *
 * A search will be performed using a logical AND operation across all provided criteria.
 *
 * @property packageNames The set of package names to filter by, or null if this filter is skipped.
 *
 *   The calling app can only search metadata for functions in packages that it is allowed to query
 *   via [android.content.pm.PackageManager.canPackageQuery]. If a package is not queryable by the
 *   calling app, its functions' metadata will not be visible.
 *
 * @property schemaCategory The schema category to filter by, or null if this filter is skipped.
 * @property schemaName The schema name to filter by, or null if this filter is skipped.
 * @property minSchemaVersion The minimum schema version to filter by, or 0 if this filter is
 *   skipped.
 * @property functionNames The set of [AppFunctionName] to filter by, or null if this filter is
 *   skipped.
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
    @get:Suppress(
        // Null value is used to specify that the value was not set by the caller to be consistent
        // with other string fields.
        "NullableCollection"
    )
    public val functionNames: Set<AppFunctionName>? = null,
) {
    init {
        require(minSchemaVersion >= 0) {
            "The minimum schema version must be a non-negative integer."
        }
        require(packageNames == null || packageNames.isNotEmpty()) {
            "Cannot filter by empty set of package names."
        }
        require(functionNames == null || functionNames.isNotEmpty()) {
            "Cannot filter by empty set of function names."
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

    /**
     * Converts [androidx.appfunctions.AppFunctionSearchSpec] to
     * [android.app.appfunctions.AppFunctionSearchSpec].
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toPlatformSearchSpec(): android.app.appfunctions.AppFunctionSearchSpec {
        return android.app.appfunctions.AppFunctionSearchSpec.Builder()
            .setSchemaCategory(schemaCategory)
            .setSchemaName(schemaName)
            .setMinSchemaVersion(minSchemaVersion.toLong())
            .setPackageNames(packageNames)
            .apply {
                if (functionNames != null) {
                    setFunctionNames(
                        functionNames
                            .map {
                                android.app.appfunctions.AppFunctionName(
                                    it.packageName,
                                    it.functionIdentifier,
                                )
                            }
                            .toSet()
                    )
                }
            }
            .build()
    }
}
