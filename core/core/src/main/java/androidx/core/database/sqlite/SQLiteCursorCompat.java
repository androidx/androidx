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

package androidx.core.database.sqlite;

import android.database.AbstractWindowedCursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper for accessing features in {@link AbstractWindowedCursor}
 */
public final class SQLiteCursorCompat {

    private SQLiteCursorCompat() {
        // This class is not instantiable.
    }

    /**
     * Controls whether the cursor is filled starting at the position passed to
     * {@link SQLiteCursor#moveToPosition(int)}.
     * <p>
     * By default, SQLiteCursor will optimize for accesses around the requested row index by loading
     * data on either side of it. Pass true to this method to disable that behavior, useful to
     * optimize multi-window, continuous reads.
     * <p>
     * Prior to Android P, this method will do nothing.
     */
    public static void setFillWindowForwardOnly(
            @NonNull SQLiteCursor cursor, boolean fillWindowForwardOnly) {
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.setFillWindowForwardOnly(cursor, fillWindowForwardOnly);
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setFillWindowForwardOnly(SQLiteCursor cursor, boolean fillWindowForwardOnly) {
            cursor.setFillWindowForwardOnly(fillWindowForwardOnly);
        }
    }
}
