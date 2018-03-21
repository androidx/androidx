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

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import android.database.sqlite.SQLiteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ForeignKeyTest {
    @Database(version = 1, entities = {A.class, B.class, C.class, D.class, E.class},
            exportSchema = false)
    abstract static class ForeignKeyDb extends RoomDatabase {
        abstract FkDao dao();
    }

    @SuppressWarnings({"SqlNoDataSourceInspection", "SameParameterValue"})
    @Dao
    interface FkDao {
        @Insert
        void insert(A... a);

        @Insert
        void insert(B... b);

        @Insert
        void insert(C... c);

        @Insert
        void insert(D... d);

        @Query("SELECT * FROM A WHERE id = :id")
        A loadA(int id);

        @Query("SELECT * FROM B WHERE id = :id")
        B loadB(int id);

        @Query("SELECT * FROM C WHERE id = :id")
        C loadC(int id);

        @Query("SELECT * FROM D WHERE id = :id")
        D loadD(int id);

        @Query("SELECT * FROM E WHERE id = :id")
        E loadE(int id);

        @Delete
        void delete(A... a);

        @Delete
        void delete(B... b);

        @Delete
        void delete(C... c);

        @Query("UPDATE A SET name = :newName WHERE id = :id")
        void changeNameA(int id, String newName);

        @Insert
        void insert(E... e);


    }

    @Entity(indices = {@Index(value = "name", unique = true),
            @Index(value = {"name", "lastName"}, unique = true)})
    static class A {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String name;
        public String lastName;

        A(String name) {
            this.name = name;
        }

        @Ignore
        A(String name, String lastName) {
            this.name = name;
            this.lastName = lastName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(foreignKeys = {
            @ForeignKey(entity = A.class,
                    parentColumns = "name",
                    childColumns = "aName")})

    static class B {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String aName;

        B(String aName) {
            this.aName = aName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(foreignKeys = {
            @ForeignKey(entity = A.class,
                    parentColumns = "name",
                    childColumns = "aName",
                    deferred = true)})
    static class C {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String aName;

        C(String aName) {
            this.aName = aName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(foreignKeys = {
            @ForeignKey(entity = A.class,
                    parentColumns = "name",
                    childColumns = "aName",
                    onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.CASCADE)})
    static class D {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String aName;

        D(String aName) {
            this.aName = aName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Entity(foreignKeys = {
            @ForeignKey(entity = A.class,
                    parentColumns = {"name", "lastName"},
                    childColumns = {"aName", "aLastName"},
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE)})
    static class E {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public String aName;
        public String aLastName;

        E() {
        }

        @Ignore
        E(String aName, String aLastName) {
            this.aName = aName;
            this.aLastName = aLastName;
        }
    }


    private ForeignKeyDb mDb;
    private FkDao mDao;

    @Before
    public void openDb() {
        mDb = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getTargetContext(),
                ForeignKeyDb.class).build();
        mDao = mDb.dao();
    }

    @Test
    public void simpleForeignKeyFailure() {
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mDao.insert(new B("foo"));
            }
        });
        assertThat(t, instanceOf(SQLiteException.class));
        assertThat(t.getMessage().toUpperCase(Locale.US), is(foreignKeyErrorMessage()));
    }

    @Test
    public void simpleForeignKeyDeferredFailure() {
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mDao.insert(new C("foo"));
            }
        });
        assertThat(t, instanceOf(SQLiteException.class));
        assertThat(t.getMessage().toUpperCase(Locale.US), is(foreignKeyErrorMessage()));
    }

    @Test
    public void immediateForeignKeyFailure() {
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                try {
                    mDb.beginTransaction();
                    mDao.insert(new B("foo"));
                    mDao.insert(new A("foo"));
                    mDb.setTransactionSuccessful();
                } finally {
                    mDb.endTransaction();
                }
            }
        });
        assertThat(t, instanceOf(SQLiteException.class));
    }

    @Test
    public void deferredForeignKeySuccess() {
        try {
            mDb.beginTransaction();
            mDao.insert(new C("foo"));
            mDao.insert(new A("foo"));
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        assertThat(mDao.loadA(1), notNullValue());
        assertThat(mDao.loadC(1), notNullValue());
    }

    @Test
    public void onDelete_noAction() {
        mDao.insert(new A("a1"));
        final A a = mDao.loadA(1);
        mDao.insert(new B("a1"));
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mDao.delete(a);
            }
        });
        assertThat(t, instanceOf(SQLiteException.class));
        assertThat(t.getMessage().toUpperCase(Locale.US), is(foreignKeyErrorMessage()));
    }

    @Test
    public void onDelete_noAction_withTransaction() {
        mDao.insert(new A("a1"));
        final A a = mDao.loadA(1);
        mDao.insert(new B("a1"));
        final B b = mDao.loadB(1);
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                deleteInTransaction(a, b);
            }
        });
        assertThat(t, instanceOf(SQLiteException.class));
        assertThat(t.getMessage().toUpperCase(Locale.US), is(foreignKeyErrorMessage()));
    }

    @Test
    public void onDelete_noAction_deferred() {
        mDao.insert(new A("a1"));
        final A a = mDao.loadA(1);
        mDao.insert(new C("a1"));
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mDao.delete(a);
            }
        });
        assertThat(t, instanceOf(SQLiteException.class));
        assertThat(t.getMessage().toUpperCase(Locale.US), is(foreignKeyErrorMessage()));
    }

    @Test
    public void onDelete_noAction__deferredWithTransaction() {
        mDao.insert(new A("a1"));
        final A a = mDao.loadA(1);
        mDao.insert(new C("a1"));
        final C c = mDao.loadC(1);
        deleteInTransaction(a, c);
    }

    @Test
    public void onDelete_cascade() {
        mDao.insert(new A("a1"));
        final A a = mDao.loadA(1);
        mDao.insert(new D("a1"));
        final D d = mDao.loadD(1);
        assertThat("test sanity", d, notNullValue());
        mDao.delete(a);
        assertThat(mDao.loadD(1), nullValue());
    }

    @Test
    public void onUpdate_cascade() {
        mDao.insert(new A("a1"));
        mDao.insert(new D("a1"));
        final D d = mDao.loadD(1);
        assertThat("test sanity", d, notNullValue());
        mDao.changeNameA(1, "bla");
        assertThat(mDao.loadD(1).aName, equalTo("bla"));
        assertThat(mDao.loadA(1).name, equalTo("bla"));
    }

    @Test
    public void multipleReferences() {
        mDao.insert(new A("a1", "a2"));
        final A a = mDao.loadA(1);
        assertThat("test sanity", a, notNullValue());
        Throwable t = catchException(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mDao.insert(new E("a1", "dsa"));
            }
        });
        assertThat(t.getMessage().toUpperCase(Locale.US), is(foreignKeyErrorMessage()));
    }

    @Test
    public void onDelete_setNull_multipleReferences() {
        mDao.insert(new A("a1", "a2"));
        final A a = mDao.loadA(1);
        mDao.insert(new E("a1", "a2"));
        assertThat(mDao.loadE(1), notNullValue());
        mDao.delete(a);
        E e = mDao.loadE(1);
        assertThat(e, notNullValue());
        assertThat(e.aName, nullValue());
        assertThat(e.aLastName, nullValue());
    }

    @Test
    public void onUpdate_cascade_multipleReferences() {
        mDao.insert(new A("a1", "a2"));
        final A a = mDao.loadA(1);
        mDao.insert(new E("a1", "a2"));
        assertThat(mDao.loadE(1), notNullValue());
        mDao.changeNameA(1, "foo");
        assertThat(mDao.loadE(1), notNullValue());
        assertThat(mDao.loadE(1).aName, equalTo("foo"));
        assertThat(mDao.loadE(1).aLastName, equalTo("a2"));
    }

    private static Matcher<String> foreignKeyErrorMessage() {
        return either(containsString("FOREIGN KEY"))
                .or(both(containsString("CODE 19")).and(containsString("CONSTRAINT FAILED")));
    }

    @SuppressWarnings("Duplicates")
    private void deleteInTransaction(A a, B b) {
        mDb.beginTransaction();
        try {
            mDao.delete(a);
            mDao.delete(b);
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }

    @SuppressWarnings("Duplicates")
    private void deleteInTransaction(A a, C c) {
        mDb.beginTransaction();
        try {
            mDao.delete(a);
            mDao.delete(c);
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }

    private static Throwable catchException(ThrowingRunnable throwingRunnable) {
        try {
            throwingRunnable.run();
        } catch (Throwable t) {
            return t;
        }
        throw new RuntimeException("didn't throw an exception");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
