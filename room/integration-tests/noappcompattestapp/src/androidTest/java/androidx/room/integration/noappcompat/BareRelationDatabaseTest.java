/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.noappcompat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

// More than a simple read & write, this test that we generate correct relationship collector
// code that doesn't use androidx.collection
@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("WeakerAccess") // to avoid naming field with m
public class BareRelationDatabaseTest {

    @Test
    public void simpleReadWrite() {
        RelationDatabase db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), RelationDatabase.class)
                .build();
        UserPetDao dao = db.getDao();
        dao.insertUser(new User(1L));
        dao.insertPet(new Pet(1L, 1L));
        dao.insertPet(new Pet(2L, 1L));

        UserAndPets result = dao.getUserWithPets(1);
        assertThat(result.user.userId, is(1L));
        assertThat(result.pets.size(), is(2));
        assertThat(result.pets.get(0).petId, is(1L));
        assertThat(result.pets.get(1).petId, is(2L));
    }

    @Test
    public void large_nonCollectionRelation() {
        RelationDatabase db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), RelationDatabase.class)
                .build();
        UserPetDao dao = db.getDao();

        int count = 2000;
        db.runInTransaction(() -> {
            for (int i = 1; i <= count; i++) {
                dao.insertUser(new User(i));
                dao.insertPet(new Pet(i, i));
            }
        });

        List<UserAndPet> ownerAndPet = dao.getUsersWithPet();
        assertThat(ownerAndPet.size(), is(count));
        for (int i = 0; i < count; i++) {
            assertThat(ownerAndPet.get(i).pet.petId, is(i + 1L));
        }
    }

    @Database(entities = {User.class, Pet.class}, version = 1, exportSchema = false)
    abstract static class RelationDatabase extends RoomDatabase {
        abstract UserPetDao getDao();
    }

    @Dao
    interface UserPetDao {
        @Query("SELECT * FROM User WHERE userId = :id")
        UserAndPets getUserWithPets(long id);

        @Query("SELECT * FROM User")
        List<UserAndPet> getUsersWithPet();

        @Insert
        void insertUser(User user);

        @Insert
        void insertPet(Pet pet);
    }

    @Entity
    static class User {
        @PrimaryKey
        public long userId;

        User(long userId) {
            this.userId = userId;
        }
    }

    @Entity
    static class Pet {
        @PrimaryKey
        public long petId;
        public long ownerId;

        Pet(long petId, long ownerId) {
            this.petId = petId;
            this.ownerId = ownerId;
        }
    }

    static class UserAndPets {
        @Embedded
        public User user;
        @Relation(parentColumn = "userId", entityColumn = "ownerId")
        public List<Pet> pets;
    }

    static class UserAndPet {
        @Embedded
        public User user;
        @Relation(parentColumn = "userId", entityColumn = "ownerId")
        public Pet pet;
    }
}
