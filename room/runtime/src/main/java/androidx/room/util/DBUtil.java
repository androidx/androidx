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

import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;

/**
 * Database utilities for Room
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DBUtil {

    /**
     * Performs the SQLiteQuery on the given database.
     * <p>
     * This util method encapsulates copying the cursor if the {@code maybeCopy} parameter is
     * {@code true} and either the api level is below a certain threshold or the full result of the
     * query does not fit in a single window.
     *
     * @param db          The database to perform the query on.
     * @param sqLiteQuery The query to perform.
     * @param maybeCopy   True if the result cursor should maybe be copied, false otherwise.
     * @return Result of the query.
     */
    @NonNull
    public static Cursor query(RoomDatabase db, SupportSQLiteQuery sqLiteQuery, boolean maybeCopy) {
        final Cursor cursor = db.query(sqLiteQuery);
        if (maybeCopy && cursor instanceof AbstractWindowedCursor) {
            AbstractWindowedCursor windowedCursor = (AbstractWindowedCursor) cursor;
            int rowsInCursor = windowedCursor.getCount(); // Should fill the window.
            int rowsInWindow;
            if (windowedCursor.hasWindow()) {
                rowsInWindow = windowedCursor.getWindow().getNumRows();
            } else {
                rowsInWindow = rowsInCursor;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || rowsInWindow < rowsInCursor) {
                return CursorUtil.copyAndClose(windowedCursor);
            }
        }

        return cursor;
    }

    private DBUtil() {
    }
}
