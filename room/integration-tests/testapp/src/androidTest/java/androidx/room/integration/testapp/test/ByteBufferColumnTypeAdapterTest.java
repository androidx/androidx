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

package androidx.room.integration.testapp.test;


import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ByteBufferColumnTypeAdapterTest {

    private ByteBufferColumnTypeAdapterDatabase mDb;

    @Entity(tableName = "byteBufferFoo")
    public static class ByteBufferFoo {
        @PrimaryKey @NonNull public String id;
        public ByteBuffer buffer;

        public ByteBufferFoo(@NotNull String id, ByteBuffer buffer) {
            this.id = id;
            this.buffer = buffer;
        }
    }

    @Dao
    public interface ByteBufferFooDao {
        @Query("select * from ByteBufferFoo where id = :id")
        ByteBufferFoo getItem(String id);

        @Insert
        void insert(ByteBufferFoo item);
    }

    @Database(
            version = 1,
            entities = {
                    ByteBufferFoo.class
            },
            exportSchema = false
    )
    public abstract static class  ByteBufferColumnTypeAdapterDatabase extends RoomDatabase {
        public abstract ByteBufferFooDao byteBufferFooDao();
    }

    @Test
    public void testByteBufferFooDao() {
        Context context = ApplicationProvider.getApplicationContext();
        mDb = Room.inMemoryDatabaseBuilder(
                context,
                ByteBufferColumnTypeAdapterDatabase.class)
                .build();

        mDb.byteBufferFooDao().insert(new ByteBufferFoo("Key1", null));
        assertThat(mDb.byteBufferFooDao().getItem("Key1").buffer).isEqualTo(null);
    }
}
