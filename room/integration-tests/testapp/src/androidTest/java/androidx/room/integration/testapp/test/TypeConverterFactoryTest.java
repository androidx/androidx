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
import static org.junit.Assert.fail;

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
import androidx.room.integration.testapp.dao.RobotsDao;
import androidx.room.integration.testapp.vo.Hivemind;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetWithUser;
import androidx.room.integration.testapp.vo.Robot;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class TypeConverterFactoryTest {

    @Test
    public void testTypeConverterFactory() {
        Context context = ApplicationProvider.getApplicationContext();
        TestDatabaseWithConverter db =
                Room.inMemoryDatabaseBuilder(context, TestDatabaseWithConverter.class)
                    .addTypeConverterFactory(new TimeStampConverterFactory())
                    .build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);

            Robot robot = new Robot(UUID.randomUUID(), UUID.randomUUID());
            db.getRobotsDao().putRobot(robot);
            db.close();
    }

    @Test
    public void testMissingTypeConverterFactoryInstance() {
        Context context = ApplicationProvider.getApplicationContext();
        try {
            TestDatabaseWithConverter db =
                    Room.inMemoryDatabaseBuilder(context, TestDatabaseWithConverter.class).build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);
            fail("Show have thrown an IllegalArgumentException");
        } catch (Throwable throwable) {
            Assert.assertThat(throwable, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void testMissingTypeConverterFactoryAnnotation() {
        Context context = ApplicationProvider.getApplicationContext();
        try {
            TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                    .addTypeConverterFactory(new TimeStampConverterFactory())
                    .build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);
            fail("Show have thrown an IllegalArgumentException");
        } catch (Throwable throwable) {
            Assert.assertThat(throwable, instanceOf(IllegalArgumentException.class));
        }
    }

    @Database(entities = {Pet.class, Toy.class, User.class, Robot.class, Hivemind.class},
            views = {PetWithUser.class},
            version = 1, exportSchema = false)
    @TypeConverters({TimeStampConverter.class, UUIDConverter.class})
    abstract static class TestDatabaseWithConverter extends RoomDatabase {
        public abstract PetDao getPetDao();
        public abstract RobotsDao getRobotsDao();
    }

    @TypeConverter.Factory(TimeStampConverterFactory.class)
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

    @TypeConverter.Factory(TimeStampConverterFactory.class)
    public static class UUIDConverter {
        @TypeConverter
        public UUID asUuid(byte[] bytes) {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            long firstLong = bb.getLong();
            long secondLong = bb.getLong();
            return new UUID(firstLong, secondLong);
        }

        @TypeConverter
        public byte[] asBytes(UUID uuid) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }
    }

    public static final class TimeStampConverterFactory implements TypeConverterFactory {

        @NonNull
        @Override
        public <T> T create(@NonNull Class<T> converterClass) {
            if (converterClass.isAssignableFrom(TimeStampConverter.class)) {
                return (T) new TimeStampConverter();
            } else if(converterClass.isAssignableFrom(UUIDConverter.class)) {
                return (T) new UUIDConverter();
            } else {
                throw new IllegalStateException("Requested unknown converter");
            }
        }
    }
}
