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
import android.support.annotation.NonNull;
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
public class GenericEntityTest {
    private GenericDb mDb;
    private GenericDao mDao;

    @Before
    public void init() {
        mDb = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getTargetContext(),
                GenericDb.class
        ).build();
        mDao = mDb.getDao();
    }

    @After
    public void close() {
        mDb.close();
    }

    @Test
    public void readWriteEntity() {
        EntityItem item = new EntityItem("abc", "def");
        mDao.insert(item);
        EntityItem received = mDao.get("abc");
        assertThat(received, is(item));
    }

    @Test
    public void readPojo() {
        EntityItem item = new EntityItem("abc", "def");
        mDao.insert(item);
        PojoItem received = mDao.getPojo("abc");
        assertThat(received.id, is("abc"));
    }

    static class Item<P, F> {
        @NonNull
        @PrimaryKey
        public final P id;
        private F mField;

        Item(@NonNull P id) {
            this.id = id;
        }

        public F getField() {
            return mField;
        }

        public void setField(F field) {
            mField = field;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item<?, ?> item = (Item<?, ?>) o;
            return Objects.equals(id, item.id)
                    && Objects.equals(mField, item.mField);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id, mField);
        }
    }

    static class PojoItem extends Item<String, Integer> {
        PojoItem(String id) {
            super(id);
        }
    }

    @Entity
    static class EntityItem extends Item<String, Integer> {
        public final String name;

        EntityItem(String id, String name) {
            super(id);
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityItem item = (EntityItem) o;
            return Objects.equals(name, item.name);
        }

        @Override
        public int hashCode() {

            return Objects.hash(name);
        }
    }

    @Dao
    public interface GenericDao {
        @Insert
        void insert(EntityItem... items);

        @Query("SELECT * FROM EntityItem WHERE id = :id")
        EntityItem get(String id);

        @Query("SELECT * FROM EntityItem WHERE id = :id")
        PojoItem getPojo(String id);
    }

    @Database(version = 1, entities = {EntityItem.class}, exportSchema = false)
    public abstract static class GenericDb extends RoomDatabase {
        abstract GenericDao getDao();
    }
}
