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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("WeakerAccess")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ClearAllTablesTest {

    @Database(version = 1, entities = {Parent.class, Child.class}, exportSchema = false)
    public abstract static class ClearAllTablesDatabase extends RoomDatabase {
        abstract ClearAllTablesDao dao();
    }

    @Entity
    public static class Parent {
        @PrimaryKey
        public long id;
        public String name;

        public Parent(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity(foreignKeys = {
            @ForeignKey(entity = Parent.class, parentColumns = "id", childColumns = "parentId")})
    public static class Child {
        @PrimaryKey
        public long id;
        public String name;
        public long parentId;

        public Child(long id, String name, long parentId) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
        }
    }

    @Dao
    public interface ClearAllTablesDao {
        @Insert
        void insertParent(Parent parent);

        @Insert
        void insertChild(Child child);

        @Query("SELECT COUNT(*) FROM Parent")
        int countParent();

        @Query("SELECT COUNT(*) FROM Child")
        int countChild();
    }

    private ClearAllTablesDatabase mDatabase;
    private ClearAllTablesDao mDao;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mDatabase = Room.inMemoryDatabaseBuilder(context, ClearAllTablesDatabase.class).build();
        mDao = mDatabase.dao();
    }

    @After
    public void closeDatabase() {
        mDatabase.close();
    }

    @Test
    public void simple() {
        mDao.insertParent(new Parent(1, "A"));
        assertThat(mDao.countParent(), is(1));
        mDatabase.clearAllTables();
        assertThat(mDao.countParent(), is(0));
    }

    @Test
    public void foreignKey() {
        mDao.insertParent(new Parent(1, "A"));
        mDao.insertChild(new Child(1, "a", 1));
        assertThat(mDao.countParent(), is(1));
        assertThat(mDao.countChild(), is(1));
        mDatabase.clearAllTables();
        assertThat(mDao.countParent(), is(0));
        assertThat(mDao.countChild(), is(0));
    }

    @Test
    public void observer() throws InterruptedException {
        mDao.insertParent(new Parent(1, "A"));
        assertThat(mDao.countParent(), is(1));
        final CountDownLatch latch = new CountDownLatch(1);
        mDatabase.getInvalidationTracker().addObserver(new InvalidationTracker.Observer("Parent") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                assertThat(tables, hasSize(1));
                assertThat(tables, hasItem("Parent"));
                assertThat(mDao.countParent(), is(0));
                latch.countDown();
            }
        });
        mDatabase.clearAllTables();
        assertThat(latch.await(3000, TimeUnit.MILLISECONDS), is(true));
    }
}
