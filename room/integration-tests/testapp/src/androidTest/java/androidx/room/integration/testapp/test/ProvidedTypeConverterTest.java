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

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.ProvidedTypeConverter;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
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
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unchecked")
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ProvidedTypeConverterTest {

    @Test
    public void testProvidedTypeConverter() {
        Context context = ApplicationProvider.getApplicationContext();
        TestDatabaseWithConverter db =
                Room.inMemoryDatabaseBuilder(context, TestDatabaseWithConverter.class)
                        .addTypeConverter(new TimeStampConverter())
                        .addTypeConverter(new UUIDConverter())
                        .build();
        Pet pet = TestUtil.createPet(3);
        pet.setName("pet");
        db.getPetDao().insertOrReplace(pet);

        Robot robot = new Robot(UUID.randomUUID(), UUID.randomUUID());
        db.getRobotsDao().putRobot(robot);
        db.close();
    }

    @Test
    public void testMissingProvidedTypeConverterInstance() {
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
    public void testMissingProvidedTypeConverterAnnotation() {
        Context context = ApplicationProvider.getApplicationContext();
        try {
            TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                    .addTypeConverter(new TimeStampConverter())
                    .build();
            Pet pet = TestUtil.createPet(3);
            pet.setName("pet");
            db.getPetDao().insertOrReplace(pet);
            fail("Show have thrown an IllegalArgumentException");
        } catch (Throwable throwable) {
            Assert.assertThat(throwable, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void differentSerializerForTheSameClassInDifferentDatabases() {
        Context context = ApplicationProvider.getApplicationContext();
        ProvidedTypeConverterNameLastNameDb db1 = Room
                .inMemoryDatabaseBuilder(context, ProvidedTypeConverterNameLastNameDb.class)
                .addTypeConverter(new NameLastNameSerializer())
                .build();
        ProvidedTypeConverterLastNameNameDb db2 = Room
                .inMemoryDatabaseBuilder(context, ProvidedTypeConverterLastNameNameDb.class)
                .addTypeConverter(new LastNameNameSerializer())
                .build();
        ProvidedTypeConverterEntity entity1 = new ProvidedTypeConverterEntity(1,
                new Username("foo1", "bar1"));
        ProvidedTypeConverterEntity entity2 = new ProvidedTypeConverterEntity(2,
                new Username("foo2", "bar2"));
        db1.getDao().insert(entity1);
        db2.getDao().insert(entity2);
        assertThat(db1.getDao().get(1)).isEqualTo(entity1);
        assertThat(db2.getDao().get(2)).isEqualTo(entity2);
        assertThat(db1.getDao().getRawUsername(1)).isEqualTo("foo1-bar1");
        assertThat(db2.getDao().getRawUsername(2)).isEqualTo("bar2-foo2");
    }

    @Database(entities = {Pet.class, Toy.class, User.class, Robot.class, Hivemind.class},
            views = {PetWithUser.class},
            version = 1, exportSchema = false)
    @TypeConverters({TimeStampConverter.class, UUIDConverter.class})
    abstract static class TestDatabaseWithConverter extends RoomDatabase {
        public abstract PetDao getPetDao();

        public abstract RobotsDao getRobotsDao();
    }



    @Database(entities = {ProvidedTypeConverterEntity.class}, version = 1, exportSchema = false)
    @TypeConverters(NameLastNameSerializer.class)
    abstract static class ProvidedTypeConverterNameLastNameDb extends
            ProvidedTypeConverterEntityDb {
    }

    @Database(entities = {ProvidedTypeConverterEntity.class}, version = 1, exportSchema = false)
    @TypeConverters(LastNameNameSerializer.class)
    abstract static class ProvidedTypeConverterLastNameNameDb extends
            ProvidedTypeConverterEntityDb {
    }

    abstract static class ProvidedTypeConverterEntityDb extends RoomDatabase {
        public abstract ProvidedTypeConverterEntity.Dao getDao();
    }

    @ProvidedTypeConverter
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

    @ProvidedTypeConverter
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

    @Entity
    public static class ProvidedTypeConverterEntity {
        @PrimaryKey
        private final int mId;
        private final Username mUsername;

        public ProvidedTypeConverterEntity(int id, Username username) {
            mId = id;
            mUsername = username;
        }

        public int getId() {
            return mId;
        }

        public Username getUsername() {
            return mUsername;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProvidedTypeConverterEntity that = (ProvidedTypeConverterEntity) o;
            return mId == that.mId &&
                    mUsername.equals(that.mUsername);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mId, mUsername);
        }

        @androidx.room.Dao
        public interface Dao {
            @Insert
            void insert(ProvidedTypeConverterEntity entity);

            @Query("SELECT mUsername FROM ProvidedTypeConverterEntity WHERE mId = :id")
            String getRawUsername(int id);

            @Query("SELECT * FROM ProvidedTypeConverterEntity WHERE mId = :id")
            ProvidedTypeConverterEntity get(int id);
        }
    }

    /**
     * Class that is serialized differently based on database
     */
    public static class Username {
        @NonNull
        private final String mName;
        @NonNull
        private final String mLastName;

        public Username(@NonNull String name, @NonNull String lastName) {
            mName = name;
            mLastName = lastName;
        }

        @NonNull
        public String getName() {
            return mName;
        }

        @NonNull
        public String getLastName() {
            return mLastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Username username = (Username) o;
            return mName.equals(username.mName) &&
                    mLastName.equals(username.mLastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mName, mLastName);
        }
    }

    @ProvidedTypeConverter
    public class NameLastNameSerializer {
        @TypeConverter
        public Username fromString(String input) {
            String[] sections = input.split("-");
            return new Username(sections[0], sections[1]);
        }

        @TypeConverter
        public String toString(Username input) {
            return input.getName() + "-" + input.getLastName();
        }
    }

    @ProvidedTypeConverter
    public class LastNameNameSerializer {
        @TypeConverter
        public Username fromString(String input) {
            String[] sections = input.split("-");
            return new Username(sections[1], sections[0]);
        }

        @TypeConverter
        public String toString(Username input) {
            return input.getLastName() + "-" + input.getName();
        }
    }
}
