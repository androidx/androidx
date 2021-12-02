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
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteCompat;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database utilities for Room
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
     *
     * @deprecated This is only used in the generated code and shouldn't be called directly.
     */
    @Deprecated
    @NonNull
    public static Cursor query(RoomDatabase db, SupportSQLiteQuery sqLiteQuery, boolean maybeCopy) {
        return query(db, sqLiteQuery, maybeCopy, null);
    }

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
     * @param signal      The cancellation signal to be attached to the query.
     * @return Result of the query.
     */
    @NonNull
    public static Cursor query(@NonNull RoomDatabase db, @NonNull SupportSQLiteQuery sqLiteQuery,
            boolean maybeCopy, @Nullable CancellationSignal signal) {
        final Cursor cursor = db.query(sqLiteQuery, signal);
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

    /**
     * Drops all FTS content sync triggers created by Room.
     * <p>
     * FTS content sync triggers created by Room are those that are found in the sqlite_master table
     * who's names start with 'room_fts_content_sync_'.
     *
     * @param db The database.
     */
    public static void dropFtsSyncTriggers(SupportSQLiteDatabase db) {
        List<String> existingTriggers = new ArrayList<>();
        Cursor cursor = db.query("SELECT name FROM sqlite_master WHERE type = 'trigger'");
        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (cursor.moveToNext()) {
                existingTriggers.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }

        for (String triggerName : existingTriggers) {
            if (triggerName.startsWith("room_fts_content_sync_")) {
                db.execSQL("DROP TRIGGER IF EXISTS " + triggerName);
            }
        }
    }

    /**
     * Checks for foreign key violations by executing a PRAGMA foreign_key_check.
     */
    public static void foreignKeyCheck(@NonNull SupportSQLiteDatabase db,
            @NonNull String tableName) {
        Cursor cursor = db.query("PRAGMA foreign_key_check(`" + tableName + "`)");
        try {
            if (cursor.getCount() > 0) {
                String errorMsg = processForeignKeyCheckFailure(cursor);
                throw new IllegalStateException(errorMsg);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Reads the user version number out of the database header from the given file.
     *
     * @param databaseFile the database file.
     * @return the database version
     * @throws IOException if something goes wrong reading the file, such as bad database header or
     * missing permissions.
     *
     * @see <a href="https://www.sqlite.org/fileformat.html#user_version_number">User Version
     * Number</a>.
     */
    public static int readVersion(@NonNull File databaseFile) throws IOException {
        FileChannel input = null;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            input = new FileInputStream(databaseFile).getChannel();
            input.tryLock(60, 4, true);
            input.position(60);
            int read = input.read(buffer);
            if (read != 4) {
                throw new IOException("Bad database header, unable to read 4 bytes at offset 60");
            }
            buffer.rewind();
            return buffer.getInt(); // ByteBuffer is big-endian by default
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * CancellationSignal is only available from API 16 on. This function will create a new
     * instance of the Cancellation signal only if the current API > 16.
     *
     * @return A new instance of CancellationSignal or null.
     */
    @Nullable
    public static CancellationSignal createCancellationSignal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return SupportSQLiteCompat.Api16Impl.createCancellationSignal();
        }
        return null;
    }


    /**
     * Converts the {@link Cursor} returned in case of a foreign key violation into a detailed
     * error message for debugging.
     * <p>
     * The foreign_key_check pragma returns one row output for each foreign key violation.
     * <p>
     * The cursor received has four columns for each row output. The first column is the name of
     * the child table. The second column is the rowId of the row that contains the foreign key
     * violation (or NULL if the child table is a WITHOUT ROWID table). The third column is the
     * name of the parent table. The fourth column is the index of the specific foreign key
     * constraint that failed.
     *
     * @param cursor Cursor containing information regarding the FK violation
     * @return Error message generated containing debugging information
     */
    private static String processForeignKeyCheckFailure(Cursor cursor) {
        int rowCount = cursor.getCount();
        String childTableName = null;
        Map<String, String> fkParentTables = new HashMap<>();

        while (cursor.moveToNext()) {
            if (childTableName == null) {
                childTableName = cursor.getString(0);
            }
            String constraintIndex = cursor.getString(3);
            if (!fkParentTables.containsKey(constraintIndex)) {
                fkParentTables.put(constraintIndex, cursor.getString(2));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Foreign key violation(s) detected in '")
                .append(childTableName).append("'.\n");
        sb.append("Number of different violations discovered: ")
                .append(fkParentTables.keySet().size()).append("\n");
        sb.append("Number of rows in violation: ")
                .append(rowCount).append("\n");
        sb.append("Violation(s) detected in the following constraint(s):\n");

        for (Map.Entry<String, String> entry : fkParentTables.entrySet()) {
            sb.append("\tParent Table = ")
                    .append(entry.getValue());
            sb.append(", Foreign Key Constraint Index = ")
                    .append(entry.getKey()).append("\n");
        }
        return sb.toString();
    }

    private DBUtil() {
    }
}
