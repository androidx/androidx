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

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 24)
public class TypeConverterPriorityTest {

    private TestDatabase mDB;

    @Before
    public void setup() {
        mDB = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                TestDatabase.class
        ).build();
    }

    @Test
    public void testConverterMultiParam() {
        mDB.getDao().insert(new TestEntity("1", List.of("a", "b", "c")));
        mDB.getDao().insert(new TestEntity("2", List.of("d", "e", "f")));
        mDB.getDao().insert(new TestEntity("3", List.of("g", "h", "i")));
        mDB.getDao().delete(List.of("2", "3"));
        assertThat(mDB.getDao().getAll().size()).isEqualTo(1);
    }

    @Test
    public void testConverterSingleParam() {
        mDB.getDao().insert(new TestEntity("1", List.of("a", "b", "c")));
        mDB.getDao().update("1", List.of("d", "e", "f"));
        assertThat(mDB.getDao().getAll().size()).isEqualTo(1);
        assertThat(mDB.getDao().getAll().get(0).data).isEqualTo(List.of("d", "e", "f"));
    }

    @Database(entities = {TestEntity.class}, version = 1, exportSchema = false)
    @TypeConverters(Converters.class)
    abstract static class TestDatabase extends RoomDatabase {
        abstract TestDao getDao();
    }

    @Dao
    interface TestDao {
        @Insert
        void insert(TestEntity entity);

        @Query("SELECT * FROM TestEntity")
        List<TestEntity> getAll();

        @Query("DELETE FROM TestEntity WHERE id IN (:ids)")
        void delete(List<String> ids);

        @Query("UPDATE TestEntity SET data = :csv WHERE id = :id")
        void update(String id, List<String> csv);
    }

    @Entity
    static final class TestEntity {
        @NonNull
        @PrimaryKey
        public String id;
        public List<String> data;

        TestEntity(@NonNull String id, List<String> data) {
            this.id = id;
            this.data = data;
        }
    }

    static final class Converters {
        @TypeConverter
        public static String fromData(List<String> list) {
            return String.join(",", list);
        }

        @TypeConverter
        public static List<String> toData(String string) {
            return Stream.of(string.split(",")).collect(Collectors.toList());
        }
    }
}
