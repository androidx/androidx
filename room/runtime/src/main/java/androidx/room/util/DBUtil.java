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
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

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
            input.lock(60, 4, true);
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

    private DBUtil() {
    }
}
