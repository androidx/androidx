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

package android.arch.persistence.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DaoNameConflictTest {
    private ConflictDatabase mDb;
    @Before
    public void init() {
        mDb = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getTargetContext(),
                ConflictDatabase.class
        ).build();
    }

    @After
    public void close() {
        mDb.close();
    }

    @Test
    public void readFromItem1() {
        Item1 item1 = new Item1(1, "a");
        mDb.item1Dao().insert(item1);
        Item2 item2 = new Item2(2, "b");
        mDb.item2Dao().insert(item2);
        assertThat(mDb.item1Dao().get(), is(item1));
        assertThat(mDb.item2Dao().get(), is(item2));
    }

    @Entity
    static class Item1 {
        @PrimaryKey
        public int id;
        public String name;

        Item1(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Dao
        public interface Store {
            @Query("SELECT * FROM Item1 LIMIT 1")
            Item1 get();
            @Insert
            void insert(Item1... items);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item1 item1 = (Item1) o;
            return id == item1.id
                    && Objects.equals(name, item1.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id, name);
        }
    }

    @Entity
    static class Item2 {
        @PrimaryKey
        public int id;
        public String name;

        Item2(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Dao
        public interface Store {
            @Query("SELECT * FROM Item2 LIMIT 1")
            Item2 get();
            @Insert
            void insert(Item2... items);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item2 item2 = (Item2) o;
            return id == item2.id
                    && Objects.equals(name, item2.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id, name);
        }
    }

    @Database(version = 1, exportSchema = false, entities = {Item1.class, Item2.class})
    public abstract static class ConflictDatabase extends RoomDatabase {
        public abstract Item1.Store item1Dao();
        public abstract Item2.Store item2Dao();
    }
}
