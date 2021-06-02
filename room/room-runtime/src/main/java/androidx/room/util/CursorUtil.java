/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.room.util;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.Arrays;

/**
 * Cursor utilities for Room
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CursorUtil {

    /**
     * Copies the given cursor into a in-memory cursor and then closes it.
     * <p>
     * This is useful for iterating over a cursor multiple times without the cost of JNI while
     * reading or IO while filling the window at the expense of memory consumption.
     *
     * @param c the cursor to copy.
     * @return a new cursor containing the same data as the given cursor.
     */
    @NonNull
    public static Cursor copyAndClose(@NonNull Cursor c) {
        final MatrixCursor matrixCursor;
        try {
            matrixCursor = new MatrixCursor(c.getColumnNames(), c.getCount());
            while (c.moveToNext()) {
                final Object[] row = new Object[c.getColumnCount()];
                for (int i = 0; i < c.getColumnCount(); i++) {
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            row[i] = null;
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            row[i] = c.getLong(i);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            row[i] = c.getDouble(i);
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row[i] = c.getString(i);
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            row[i] = c.getBlob(i);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }
                matrixCursor.addRow(row);
            }
        } finally {
            c.close();
        }
        return matrixCursor;
    }

    /**
     * Patches {@link Cursor#getColumnIndex(String)} to work around issues on older devices.
     * If the column is not found, it retries with the specified name surrounded by backticks.
     *
     * @param c    The cursor.
     * @param name The name of the target column.
     * @return The index of the column, or -1 if not found.
     */
    public static int getColumnIndex(@NonNull Cursor c, @NonNull String name) {
        int index = c.getColumnIndex(name);
        if (index >= 0) {
            return index;
        }
        index = c.getColumnIndex("`" + name + "`");
        if (index >= 0) {
            return index;
        }
        return findColumnIndexBySuffix(c, name);
    }

    /**
     * Patches {@link Cursor#getColumnIndexOrThrow(String)} to work around issues on older devices.
     * If the column is not found, it retries with the specified name surrounded by backticks.
     *
     * @param c    The cursor.
     * @param name The name of the target column.
     * @return The index of the column.
     * @throws IllegalArgumentException if the column does not exist.
     */
    public static int getColumnIndexOrThrow(@NonNull Cursor c, @NonNull String name) {
        final int index = getColumnIndex(c, name);
        if (index >= 0) {
            return index;
        }
        String availableColumns = "";
        try {
            availableColumns = Arrays.toString(c.getColumnNames());
        } catch (Exception e) {
            Log.d("RoomCursorUtil", "Cannot collect column names for debug purposes", e);
        }
        throw new IllegalArgumentException("column '" + name
                + "' does not exist. Available columns: " + availableColumns);
    }

    /**
     * Finds a column by name by appending `.` in front of it and checking by suffix match.
     * Also checks for the version wrapped with `` (backticks).
     * workaround for b/157261134 for API levels 25 and below
     *
     * e.g. "foo" will match "any.foo" and "`any.foo`"
     */
    private static int findColumnIndexBySuffix(@NonNull Cursor cursor, @NonNull String name) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            // we need this workaround only on APIs < 26. So just return not found on newer APIs
            return -1;
        }
        if (name.length() == 0) {
            return -1;
        }
        final String[] columnNames = cursor.getColumnNames();
        return findColumnIndexBySuffix(columnNames, name);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static int findColumnIndexBySuffix(String[] columnNames, String name) {
        String dotSuffix = "." + name;
        String backtickSuffix = "." + name + "`";
        for (int index = 0; index < columnNames.length; index++) {
            String columnName = columnNames[index];
            // do not check if column name is not long enough. 1 char for table name, 1 char for '.'
            if (columnName.length() >= name.length() + 2) {
                if (columnName.endsWith(dotSuffix)) {
                    return index;
                } else if (columnName.charAt(0) == '`'
                        && columnName.endsWith(backtickSuffix)) {
                    return index;
                }
            }
        }
        return -1;
    }

    private CursorUtil() {
    }
}
