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

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("SqlNoDataSourceInspection")
@SmallTest
public class ConstructorTest {
    @Database(version = 1, entities = {FullConstructor.class, PartialConstructor.class,
            EntityWithAnnotations.class},
            exportSchema = false)
    abstract static class MyDb extends RoomDatabase {
        abstract MyDao dao();
    }

    @Dao
    interface MyDao {
        @Insert
        void insertFull(FullConstructor... full);

        @Query("SELECT * FROM fc WHERE a = :a")
        FullConstructor loadFull(int a);

        @Insert
        void insertPartial(PartialConstructor... partial);

        @Query("SELECT * FROM pc WHERE a = :a")
        PartialConstructor loadPartial(int a);

        @Insert
        void insertEntityWithAnnotations(EntityWithAnnotations... items);

        @Query("SELECT * FROM EntityWithAnnotations")
        EntityWithAnnotations getEntitiWithAnnotations();
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(tableName = "fc")
    static class FullConstructor {
        @PrimaryKey
        public final int a;
        public final int b;
        @Embedded
        public final MyEmbedded embedded;

        FullConstructor(int a, int b, MyEmbedded embedded) {
            this.a = a;
            this.b = b;
            this.embedded = embedded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FullConstructor that = (FullConstructor) o;

            if (a != that.a) return false;
            //noinspection SimplifiableIfStatement
            if (b != that.b) return false;
            return embedded != null ? embedded.equals(that.embedded)
                    : that.embedded == null;
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + b;
            result = 31 * result + (embedded != null ? embedded.hashCode() : 0);
            return result;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(tableName = "pc")
    static class PartialConstructor {
        @PrimaryKey
        public final int a;
        public int b;
        @Embedded
        private MyEmbedded mEmbedded;

        PartialConstructor(int a) {
            this.a = a;
        }

        public MyEmbedded getEmbedded() {
            return mEmbedded;
        }

        public void setEmbedded(MyEmbedded embedded) {
            mEmbedded = embedded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PartialConstructor that = (PartialConstructor) o;

            if (a != that.a) return false;
            //noinspection SimplifiableIfStatement
            if (b != that.b) return false;
            return mEmbedded != null ? mEmbedded.equals(that.mEmbedded)
                    : that.mEmbedded == null;
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + b;
            result = 31 * result + (mEmbedded != null ? mEmbedded.hashCode() : 0);
            return result;
        }
    }

    @SuppressWarnings("WeakerAccess")
    static class MyEmbedded {
        public final String text;

        MyEmbedded(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyEmbedded that = (MyEmbedded) o;

            return text != null ? text.equals(that.text) : that.text == null;
        }

        @Override
        public int hashCode() {
            return text != null ? text.hashCode() : 0;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity
    static class EntityWithAnnotations {
        @PrimaryKey
        @NonNull
        public final String id;

        @NonNull
        public final String username;

        @Nullable
        public final String displayName;

        EntityWithAnnotations(
                @NonNull String id,
                @NonNull String username,
                @Nullable String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EntityWithAnnotations that = (EntityWithAnnotations) o;

            if (!id.equals(that.id)) return false;
            if (!username.equals(that.username)) return false;
            return displayName != null ? displayName.equals(that.displayName)
                    : that.displayName == null;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + username.hashCode();
            result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
            return result;
        }
    }

    private MyDb mDb;
    private MyDao mDao;

    @Before
    public void init() {
        mDb = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getTargetContext(), MyDb.class)
                .build();
        mDao = mDb.dao();
    }

    @Test
    public void insertAndReadFullConstructor() {
        FullConstructor inserted = new FullConstructor(1, 2, null);
        mDao.insertFull(inserted);
        final FullConstructor load = mDao.loadFull(1);
        assertThat(load, is(inserted));
    }

    @Test
    public void insertAndReadPartial() {
        PartialConstructor item = new PartialConstructor(3);
        item.b = 7;
        mDao.insertPartial(item);
        PartialConstructor load = mDao.loadPartial(3);
        assertThat(load, is(item));
    }

    @Test // for bug b/69562125
    public void entityWithAnnotations() {
        EntityWithAnnotations item = new EntityWithAnnotations("a", "b", null);
        mDao.insertEntityWithAnnotations(item);
        assertThat(mDao.getEntitiWithAnnotations(), is(item));
    }
}
