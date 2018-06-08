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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
            //noinspection SimplifiableIfStatement
            if (!id.equals(item.id)) return false;
            return mField != null ? mField.equals(item.mField) : item.mField == null;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (mField != null ? mField.hashCode() : 0);
            return result;
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
            if (!super.equals(o)) return false;
            EntityItem that = (EntityItem) o;
            return name != null ? name.equals(that.name) : that.name == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
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
