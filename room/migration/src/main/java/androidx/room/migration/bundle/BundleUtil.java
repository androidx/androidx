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

package androidx.room.migration.bundle;

import androidx.annotation.RestrictTo;

/**
 * Utility functions for bundling.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BundleUtil {
    /**
     * Placeholder for table names in queries.
     */
    public static final String TABLE_NAME_PLACEHOLDER = "${TABLE_NAME}";

    static String replaceTableName(String contents, String tableName) {
        return contents.replace(TABLE_NAME_PLACEHOLDER, tableName);
    }

    private BundleUtil() {
    }
}
