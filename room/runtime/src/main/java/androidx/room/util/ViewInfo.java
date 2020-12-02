/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * A data class that holds the information about a view.
 * <p>
 * This derives information from sqlite_master.
 * <p>
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ViewInfo {

    /**
     * The view name
     */
    public final String name;

    /**
     * The SQL of CREATE VIEW.
     */
    public final String sql;

    public ViewInfo(String name, String sql) {
        this.name = name;
        this.sql = sql;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViewInfo)) return false;
        ViewInfo viewInfo = (ViewInfo) o;
        return (name != null ? name.equals(viewInfo.name) : viewInfo.name == null)
                && (sql != null ? sql.equals(viewInfo.sql) : viewInfo.sql == null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (sql != null ? sql.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ViewInfo{"
                + "name='" + name + '\''
                + ", sql='" + sql + '\''
                + '}';
    }

    /**
     * Reads the view information from the given database.
     *
     * @param database The database to read the information from.
     * @param viewName The view name.
     * @return A ViewInfo containing the schema information for the provided view name.
     */
    @SuppressWarnings("SameParameterValue")
    public static ViewInfo read(SupportSQLiteDatabase database, String viewName) {
        Cursor cursor = database.query("SELECT name, sql FROM sqlite_master "
                + "WHERE type = 'view' AND name = '" + viewName + "'");
        //noinspection TryFinallyCanBeTryWithResources
        try {
            if (cursor.moveToFirst()) {
                return new ViewInfo(cursor.getString(0), cursor.getString(1));
            } else {
                return new ViewInfo(viewName, null);
            }
        } finally {
            cursor.close();
        }
    }
}
