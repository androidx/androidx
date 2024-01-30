/*
 * Copyright (C) 2017 The Android Open Source Project
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
@file:JvmName("BundleUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo

/**
 * Utility functions for bundling.
 *
 */

/**
 * Placeholder for table names in queries.
 */
public const val TABLE_NAME_PLACEHOLDER: String = "\${TABLE_NAME}"

/**
 * Placeholder for view names in queries.
 */
public const val VIEW_NAME_PLACEHOLDER: String = "\${VIEW_NAME}"

public fun replaceTableName(contents: String, tableName: String): String {
    return contents.replace(TABLE_NAME_PLACEHOLDER, tableName)
}

public fun replaceViewName(contents: String, viewName: String): String {
    return contents.replace(VIEW_NAME_PLACEHOLDER, viewName)
}
