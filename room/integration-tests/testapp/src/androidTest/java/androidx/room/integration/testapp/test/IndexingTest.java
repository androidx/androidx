/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class IndexingTest {
    @Entity(
            tableName = "foo_table",
            indices = {
                    @Index({"field1", "field2"}),
                    @Index(value = {"field2", "mId"}, unique = true),
                    @Index(value = {"field2"}, unique = true, name = "customIndex"),
            })
    static class Entity1 {
        @PrimaryKey
        public int mId;
        public String field1;
        public String field2;
        @ColumnInfo(index = true, name = "my_field")
        public String field3;
    }

    static class IndexInfo {
        public String name;
        @ColumnInfo(name = "tbl_name")
        public String tableName;
        public String sql;
    }

    @Dao
    public interface SqlMasterDao {
        @Query("SELECT * FROM sqlite_master WHERE type = 'index'")
        List<IndexInfo> loadIndices();
    }

    @Database(entities = {Entity1.class}, version = 1, exportSchema = false)
    abstract static class IndexingDb extends RoomDatabase {
        abstract SqlMasterDao sqlMasterDao();
    }

    @Test
    public void verifyIndices() {
        Context context = InstrumentationRegistry.getTargetContext();
        IndexingDb db = Room.inMemoryDatabaseBuilder(context, IndexingDb.class).build();
        List<IndexInfo> indices = db.sqlMasterDao().loadIndices();
        assertThat(indices.size(), is(4));
        for (IndexInfo info : indices) {
            assertThat(info.tableName, is("foo_table"));
        }
        assertThat(indices.get(0).sql, is("CREATE INDEX `index_foo_table_field1_field2`"
                + " ON `foo_table` (`field1`, `field2`)"));
        assertThat(indices.get(1).sql, is("CREATE UNIQUE INDEX `index_foo_table_field2_mId`"
                + " ON `foo_table` (`field2`, `mId`)"));
        assertThat(indices.get(2).sql, is("CREATE UNIQUE INDEX `customIndex`"
                + " ON `foo_table` (`field2`)"));
        assertThat(indices.get(3).sql, is("CREATE INDEX `index_foo_table_my_field`"
                + " ON `foo_table` (`my_field`)"));
    }
}
