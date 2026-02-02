/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.test;

import static androidx.kruth.Kruth.assertThat;

import androidx.room3.Dao;
import androidx.room3.Database;
import androidx.room3.Entity;
import androidx.room3.Insert;
import androidx.room3.OnConflictStrategy;
import androidx.room3.PrimaryKey;
import androidx.room3.Query;
import androidx.room3.Room;
import androidx.room3.RoomDatabase;
import androidx.room3.Transaction;
import androidx.room3.Update;
import androidx.sqlite.driver.AndroidSQLiteDriver;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DefaultDaoMethodsTest {
    interface BaseDefaultDao<T> {
        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long insert(T obj);
        @Update
        void update(T obj);

        @Transaction
        default void upsert(T obj) {
            long id = insert(obj);
            if (id == -1L) {
                update(obj);
            }
        }
    }

    @Dao
    public abstract static class AbstractEntityDao implements BaseDefaultDao<DefaultEntity> {
        @Query("SELECT COUNT(*) FROM DefaultEntity")
        abstract int count();
        @Query("SELECT * FROM DefaultEntity WHERE id = :id")
        abstract DefaultEntity load(long id);
    }

    @Dao
    public interface InterfaceEntityDao extends BaseDefaultDao<DefaultEntity> {
        @Query("SELECT COUNT(*) FROM DefaultEntity")
        int count();
        @Query("SELECT * FROM DefaultEntity WHERE id = :id")
        DefaultEntity load(long id);
    }

    @Entity
    public static class DefaultEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        public String value;

        DefaultEntity(long id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DefaultEntity)) return false;
            DefaultEntity that = (DefaultEntity) o;
            return id == that.id && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return (int) id;
        }
    }

    @Database(
            version = 1,
            exportSchema = false,
            entities = {DefaultEntity.class}
    )
    public abstract static class DefaultsDb extends RoomDatabase {
        abstract AbstractEntityDao abstractDao();
        abstract InterfaceEntityDao interfaceDao();
    }

    private DefaultsDb mDb;

    @Before
    public void init() {
        mDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                DefaultsDb.class
        ).setDriver(new AndroidSQLiteDriver())
                .build();
    }

    @After
    public void teardown() {
        mDb.close();
    }

    @Test
    public void abstractDao() {
        DefaultEntity entity = new DefaultEntity(0, "v1");
        mDb.abstractDao().insert(entity);
        entity = mDb.abstractDao().load(1);
        assertThat(entity).isNotNull();
        entity.value = "v2";
        mDb.abstractDao().upsert(entity);
        assertThat(mDb.abstractDao().count()).isEqualTo(1);
        assertThat(
                mDb.abstractDao().load(1)
        ).isEqualTo(
                new DefaultEntity(1, "v2")
        );
    }

    @Test
    public void interfaceDao() {
        DefaultEntity entity = new DefaultEntity(0, "v1");
        mDb.interfaceDao().insert(entity);
        entity = mDb.interfaceDao().load(1);
        assertThat(entity).isNotNull();
        entity.value = "v2";
        mDb.interfaceDao().upsert(entity);
        assertThat(mDb.interfaceDao().count()).isEqualTo(1);
        assertThat(
                mDb.interfaceDao().load(1)
        ).isEqualTo(
                new DefaultEntity(1, "v2")
        );
    }
}
