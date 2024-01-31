/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmMultifileClass
@file:JvmName("SQLiteStatementUtil")

package androidx.room.util

import android.os.Build
import androidx.sqlite.SQLiteStatement

/**
 * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
 *
 * The implementation also contains Android-specific patches to workaround issues on older devices.
 */
internal actual fun SQLiteStatement.getColumnIndex(name: String): Int {
    var index = this.columnIndexOf(name)
    if (index >= 0) {
        return index
    }
    index = this.columnIndexOf("`$name`")
    return if (index >= 0) {
        index
    } else {
        this.findColumnIndexBySuffix(name)
    }
}

/**
 * Finds a column by name by appending `.` in front of it and checking by suffix match. Also checks
 * for the version wrapped with `` (backticks)m a workaround for b/157261134 for API levels 25 and
 * below e.g. "foo" will match "any.foo" and "`any.foo`".
 */
private fun SQLiteStatement.findColumnIndexBySuffix(name: String): Int {
    // This workaround is only on APIs < 26. So just return not found on newer APIs
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 || name.isEmpty()) {
        return -1
    }
    val columnCount = getColumnCount()
    val dotSuffix = ".$name"
    val backtickSuffix = ".$name`"
    for (i in 0 until columnCount) {
        val columnName = getColumnName(i)
        // Do not check if column name is not long enough. 1 char for table name, 1 char for '.'
        if (columnName.length >= name.length + 2) {
            if (columnName.endsWith(dotSuffix)) {
                return i
            } else if (columnName[0] == '`' && columnName.endsWith(backtickSuffix)) {
                return i
            }
        }
    }
    return -1
}
