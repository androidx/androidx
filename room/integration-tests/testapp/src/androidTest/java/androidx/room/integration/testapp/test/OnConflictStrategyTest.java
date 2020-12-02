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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class OnConflictStrategyTest {

    @Database(version = 1, entities = {Animal.class}, exportSchema = false)
    public abstract static class OnConflictStrategyDatabase extends RoomDatabase {
        abstract AnimalDao animal();
    }

    @Entity
    public static class Animal {
        @PrimaryKey
        public final long id;
        public final String name;

        Animal(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Dao
    public interface AnimalDao {

        @Insert(/* onConflict = OnConflictStrategy.ABORT */)
        void insertOrAbort(Iterable<Animal> animals);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertOrReplace(Iterable<Animal> animals);

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        void insertOrIgnore(Iterable<Animal> animals);

        @Query("SELECT name FROM Animal")
        List<String> allNames();
    }

    @Test
    public void insertOrAbort() {
        final OnConflictStrategyDatabase db = openDatabase();
        try {
            db.animal().insertOrAbort(Arrays.asList(
                    new Animal(1, "Dog"),
                    new Animal(2, "Cat"),
                    new Animal(2, "Bird"),
                    new Animal(3, "Monkey")));
            fail("Was expecting an exception");
        } catch (SQLiteConstraintException e) {
            assertThat(e.getMessage(), is(notNullValue()));
        }
        assertThat(db.animal().allNames(), are(/* empty */));
    }

    @Test
    public void insertOrReplace() {
        final OnConflictStrategyDatabase db = openDatabase();
        db.animal().insertOrReplace(Arrays.asList(
                new Animal(1, "Dog"),
                new Animal(2, "Cat"),
                new Animal(2, "Bird"),
                new Animal(3, "Monkey")));
        assertThat(db.animal().allNames(), are("Dog", "Bird", "Monkey"));
    }

    @Test
    public void insertOrIgnore() {
        final OnConflictStrategyDatabase db = openDatabase();
        db.animal().insertOrIgnore(Arrays.asList(
                new Animal(1, "Dog"),
                new Animal(2, "Cat"),
                new Animal(2, "Bird"),
                new Animal(3, "Monkey")));
        assertThat(db.animal().allNames(), are("Dog", "Cat", "Monkey"));
    }

    @SuppressWarnings("unchecked")
    private <E> Matcher<Collection<E>> are(E... args) {
        return allOf((Matcher<? super Collection<E>>) hasSize(args.length), hasItems(args));
    }

    private OnConflictStrategyDatabase openDatabase() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return Room.inMemoryDatabaseBuilder(context, OnConflictStrategyDatabase.class).build();
    }

}
