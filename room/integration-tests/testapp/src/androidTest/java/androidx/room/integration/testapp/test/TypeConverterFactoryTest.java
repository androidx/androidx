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

import static org.hamcrest.CoreMatchers.instanceOf;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverterFactory;
import androidx.room.TypeConverters;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetWithUser;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class TypeConverterFactoryTest {

    @Test
    public void testTypeConverterFactory() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("test");
        TestDatabaseWithConverter db = null;
        Throwable throwable = null;
        try {
            db = Room.databaseBuilder(context, TestDatabaseWithConverter.class, "test")
                    .addTypeConverterFactory(new TimeStampConverterFactory())
                    .build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);
        } catch (Throwable t) {
            throwable = t;
        } finally {
            if (db != null) {
                db.close();
            }
        }
        Assert.assertNull(throwable);
    }

    @Test
    public void testMissingTypeConverterFactoryInstance() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("test");
        TestDatabaseWithConverter db = null;
        Throwable throwable = null;
        try {
            db = Room.databaseBuilder(context, TestDatabaseWithConverter.class, "test")
                    .build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);
        } catch (Throwable t) {
            throwable = t;
        } finally {
            if (db != null) {
                db.close();
            }
        }
        Assert.assertThat(throwable, instanceOf(IllegalArgumentException.class));
    }

    @Test
    public void testMissingTypeConverterFactoryAnnotation() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("test");
        TestDatabase db = null;
        Throwable throwable = null;
        try {
            db = Room.databaseBuilder(context, TestDatabase.class, "test")
                    .addTypeConverterFactory(new TimeStampConverterFactory())
                    .build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);
        } catch (Throwable t) {
            throwable = t;
        } finally {
            if (db != null) {
                db.close();
            }
        }
        Assert.assertThat(throwable, instanceOf(IllegalArgumentException.class));
    }

    @Database(entities = {Pet.class, Toy.class, User.class},
            views = {PetWithUser.class},
            version = 1, exportSchema = false)
    @TypeConverters(TimeStampConverter.class)
    abstract static class TestDatabaseWithConverter extends RoomDatabase {
        public abstract PetDao getPetDao();
    }

    @TypeConverterFactory(TimeStampConverterFactory.class)
    public static class TimeStampConverter {
        @TypeConverter
        public Date fromTimestamp(Long value) {
            return value == null ? null : new Date(value);
        }

        @TypeConverter
        public Long dateToTimestamp(Date date) {
            if (date == null) {
                return null;
            } else {
                return date.getTime();
            }
        }
    }

    public static class TimeStampConverterFactory implements RoomDatabase.TypeConverterFactory {

        @NonNull
        @Override
        public <T> T create(@NonNull Class<T> converterClass) {
            return (T) new TimeStampConverter();
        }
    }
}
