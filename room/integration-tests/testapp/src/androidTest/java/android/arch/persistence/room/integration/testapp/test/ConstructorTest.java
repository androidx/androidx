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

package android.arch.persistence.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Decompose;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("SqlNoDataSourceInspection")
@SmallTest
public class ConstructorTest {
    @Database(version = 1, entities = {FullConstructor.class, PartialConstructor.class},
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
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(tableName = "fc")
    static class FullConstructor {
        @PrimaryKey
        public final int a;
        public final int b;
        @Decompose
        public final MyDecomposed decomposed;

        FullConstructor(int a, int b, MyDecomposed decomposed) {
            this.a = a;
            this.b = b;
            this.decomposed = decomposed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FullConstructor that = (FullConstructor) o;

            if (a != that.a) return false;
            //noinspection SimplifiableIfStatement
            if (b != that.b) return false;
            return decomposed != null ? decomposed.equals(that.decomposed)
                    : that.decomposed == null;
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + b;
            result = 31 * result + (decomposed != null ? decomposed.hashCode() : 0);
            return result;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(tableName = "pc")
    static class PartialConstructor {
        @PrimaryKey
        public final int a;
        public int b;
        @Decompose
        private MyDecomposed mDecomposed;

        PartialConstructor(int a) {
            this.a = a;
        }

        public MyDecomposed getDecomposed() {
            return mDecomposed;
        }

        public void setDecomposed(MyDecomposed decomposed) {
            mDecomposed = decomposed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PartialConstructor that = (PartialConstructor) o;

            if (a != that.a) return false;
            //noinspection SimplifiableIfStatement
            if (b != that.b) return false;
            return mDecomposed != null ? mDecomposed.equals(that.mDecomposed)
                    : that.mDecomposed == null;
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + b;
            result = 31 * result + (mDecomposed != null ? mDecomposed.hashCode() : 0);
            return result;
        }
    }

    @SuppressWarnings("WeakerAccess")
    static class MyDecomposed {
        public final String text;

        MyDecomposed(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyDecomposed that = (MyDecomposed) o;

            return text != null ? text.equals(that.text) : that.text == null;
        }

        @Override
        public int hashCode() {
            return text != null ? text.hashCode() : 0;
        }
    }

    private MyDb mDb;
    private MyDao mDao;

    @Before
    public void init() {
        mDb = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(), MyDb.class)
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
}
