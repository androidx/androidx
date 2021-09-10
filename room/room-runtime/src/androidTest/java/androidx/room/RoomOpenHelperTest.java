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
package androidx.room;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RoomOpenHelperTest {
    private DatabaseConfiguration mConfiguration = mock(DatabaseConfiguration.class);
    private RoomOpenHelper.Delegate mDelegate = mock(RoomOpenHelper.Delegate.class);
    private SupportSQLiteDatabase mDb = mock(SupportSQLiteDatabase.class);
    private Cursor mCursor = mock(Cursor.class);

    @Before
    public void init() {
        when(mDb.query(anyString())).thenReturn(mCursor);
        when(mDb.query(any(SupportSQLiteQuery.class))).thenReturn(mCursor);
    }

    private void setExistingHash(String hash) {
        when(mCursor.moveToFirst()).thenReturn(true);
        // table existence check
        when(mCursor.getInt(anyInt())).thenReturn(1);
        when(mCursor.getString(0)).thenReturn(hash);
    }

    @Test
    public void room_v1_helper() {
        setExistingHash("foo");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "foo");
        helper.onOpen(mDb);
        verify(mDelegate).onOpen(mDb);
    }

    @Test
    public void room_v1_1_helper() {
        setExistingHash("bar");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        helper.onOpen(mDb);
        verify(mDelegate).onOpen(mDb);
    }

    @Test
    public void room_v1_1_helper_badHash() {
        setExistingHash("bad_hash");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        try {
            helper.onOpen(mDb);
            Assert.fail("should've thrown an exception");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void room_v1_1_helper_legacy() {
        setExistingHash("foo");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        helper.onOpen(mDb);
        verify(mDelegate).onOpen(mDb);
    }

    @Test
    public void room_v1_1_helper_legacy_badHash() {
        setExistingHash("bad_hash");
        RoomOpenHelper helper = new RoomOpenHelper(mConfiguration, mDelegate, "bar", "foo");
        try {
            helper.onOpen(mDb);
            Assert.fail("should've thrown an exception");
        } catch (IllegalStateException ignored) {
        }
    }
}
