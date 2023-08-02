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

import static org.junit.Assert.assertTrue;

import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SQLiteCursorCompatTest {
    @Test
    public void setFillWindowForwardOnly() {
        final Boolean[] calledSetter = { false };
        SQLiteDatabase db = SQLiteDatabase.create((db1, primaryQuery, editTable, query) -> {
            SQLiteCursor cursor = new SQLiteCursor(primaryQuery, editTable, query);
            SQLiteCursorCompat.setFillWindowForwardOnly(cursor, true);

            // no easy way to read whether setter worked, so
            // we just validate it can be called successfully
            calledSetter[0] = true;
            return cursor;
        });
        db.execSQL("CREATE TABLE foo (num INTEGER);");
        db.query("foo", new String[] {"*"}, null, null, null, null, null);
        db.close();
        assertTrue(calledSetter[0]);
    }
}
