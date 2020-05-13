/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Objects;

final class DatabaseExtensions {
    private static final String sInMemoryDatabasePath = ":memory:";

    /** Placeholder {@code %x} is for database's hashcode */
    private static final String sInMemoryDatabaseNameFormat =
            sInMemoryDatabasePath + " {hashcode=0x%x}";

    private DatabaseExtensions() { }

    /** Thread-safe as {@link SQLiteDatabase#getPath} and {@link Object#hashCode) are thread-safe.*/
    static String pathForDatabase(@NonNull SQLiteDatabase database) {
        return isInMemoryDatabase(database)
                ? String.format(sInMemoryDatabaseNameFormat, database.hashCode())
                : new File(database.getPath()).getAbsolutePath();
    }

    /** Thread-safe as {@link SQLiteDatabase#getPath} is thread-safe. */
    static boolean isInMemoryDatabase(@NonNull SQLiteDatabase database) {
        return Objects.equals(sInMemoryDatabasePath, database.getPath());
    }

    /**
     * Attempts to call {@link SQLiteDatabase#acquireReference} on the provided object.
     *
     * @return true if the operation was successful; false if unsuccessful because the database
     * was already closed; otherwise re-throws the exception thrown by
     * {@link SQLiteDatabase#acquireReference}.
     */
    static boolean tryAcquireReference(@NonNull SQLiteDatabase database) {
        if (!database.isOpen()) {
            return false;
        }

        try {
            database.acquireReference();
            return true; // success
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if (message != null
                    && message.contains("attempt to re-open an already-closed object")) {
                return false; // too late to secure a reference
            }
            throw e; // unexpected exception
        }
    }
}
