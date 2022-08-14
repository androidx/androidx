/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.vo.Pet;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EntityUpsertionAdapterTest{
    private TestDatabase mTestDatabase;
    private PetDao mPetDao;

    private EntityInsertionAdapter<Pet> mInsertionAdapter;

    private EntityDeletionOrUpdateAdapter<Pet> mUpdateAdapter;

    private EntityUpsertionAdapter<Pet> mUpsertionAdapter;

    @Before
    public void setUp() {
        mTestDatabase = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        TestDatabase.class).build();
        mPetDao = mTestDatabase.getPetDao();
        mInsertionAdapter =
                new EntityInsertionAdapter<Pet>(mTestDatabase) {
                    @Override
                    protected void bind(@Nullable SupportSQLiteStatement statement, Pet entity) {
                        statement.bindLong(1, entity.getPetId());
                        statement.bindLong(2, entity.getUserId());
                        statement.bindString(3, entity.getName());
                        statement.bindString(4, entity.getAdoptionDate().toString());
                    }

                    @NonNull
                    @Override
                    protected String createQuery() {
                        return "INSERT INTO `Pet` (`mPetId`, `mUserId`, `mPetName`,`mAdoptionDate`)"
                                + " VALUES (?,?,?,?)";
                    }
                };
        mUpdateAdapter =
                new EntityDeletionOrUpdateAdapter<Pet>(mTestDatabase) {
                    @NonNull
                    @Override
                    protected String createQuery() {
                        return "UPDATE `Pet` SET `mPetName` = ?, `mAdoptionDate` = ? WHERE `mPetId`"
                                + " = ?";
                    }

                    @Override
                    protected void bind(@NonNull SupportSQLiteStatement statement, Pet entity) {
                        statement.bindString(1, entity.getName());
                        statement.bindString(2, entity.getAdoptionDate().toString());
                        statement.bindLong(3, entity.getPetId());
                    }
                };
        mUpsertionAdapter =
                new EntityUpsertionAdapter<>(mInsertionAdapter, mUpdateAdapter);
    }

    @After
    public void tearDown() {
        mTestDatabase.close();
    }

    @Test
    public void testUpsert() {
        Pet newPet = new Pet();
        Date newDate = new Date(123456);
        Date testDate = new Date(123458);
        newPet.setPetId(1);
        newPet.setName("petname");
        newPet.setAdoptionDate(newDate);

        Pet testPet = new Pet();
        testPet.setPetId(1);
        testPet.setName("anotherName");
        testPet.setAdoptionDate(testDate);

        mInsertionAdapter.insert(newPet);
        mUpsertionAdapter.upsert(testPet);

        assertThat(testPet.getName()).isEqualTo(mPetDao.petWithId(1).getName());
    }

    @Test
    public void testUpsertList() {
        Pet[] testPets = TestUtil.createPetsForUser(0, 3, 9);
        Pet[] petArray = TestUtil.createPetsForUser(0, 1, 10);
        mInsertionAdapter.insert(petArray);
        mUpsertionAdapter.upsert(testPets);
        assertThat(mPetDao.petWithId(2).getName()).isEqualTo(petArray[1].getName());
        assertThat(mPetDao.petWithId(7).getName()).isEqualTo(testPets[4].getName());
        assertThat(mPetDao.count()).isEqualTo(11);
    }

    @Test
    public void testUpsertReturnId() {
        Pet testPet = TestUtil.createPet(343562);
        Pet testPet2 = TestUtil.createPet(343562);
        long resultId = mUpsertionAdapter.upsertAndReturnId(testPet);
        long result2 = mUpsertionAdapter.upsertAndReturnId(testPet2);
        assertThat(resultId).isEqualTo(343562);
        assertThat(result2).isEqualTo(-1);
        assertThat(mPetDao.petWithId(343562).getName()).isEqualTo(testPet2.getName());
    }

    @Test
    public void testUpsertReturnIds() {
        Pet[] testPets = TestUtil.createPetsForUser(0, 1, 10);
        Pet[] testPets2 = TestUtil.createPetsForUser(0, 5, 9);
        long[] result = mUpsertionAdapter.upsertAndReturnIdArray(testPets);
        long[] check = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertThat(result[3]).isEqualTo(check[3]);
        long[] testResult = mUpsertionAdapter.upsertAndReturnIdArray(testPets2);
        assertThat(testResult[8]).isEqualTo(13);
        assertThat(testResult[2]).isEqualTo(-1);
    }

    @Test
    public void testUpsertWithoutTryCatch() {
        Pet testPet = TestUtil.createPet(232);
        Pet testPet2 = TestUtil.createPet(232);
        Pet testPet3 = TestUtil.createPet(454);
        EntityInsertionAdapter<Pet> insertionAdapter =
                new EntityInsertionAdapter<Pet>(mTestDatabase) {
            @Override
            protected void bind(@Nullable SupportSQLiteStatement statement, Pet entity) {
                statement.bindLong(1, entity.getPetId());
                statement.bindLong(2, entity.getUserId());
                statement.bindString(3, entity.getName());
                statement.bindString(4, entity.getAdoptionDate().toString());
            }

            @NonNull
            @Override
            protected String createQuery() {
                return "INSERT OR IGNORE INTO `Pet` (`mPetId`, `mUserId`, `mPetName`,"
                        + "`mAdoptionDate`)"
                        + " VALUES (?,?,?,?)";
            }
        };
        insertionAdapter.insertAndReturnId(testPet);
        long result = insertionAdapter.insertAndReturnId(testPet2);
        if (result == -1) {
            mUpdateAdapter.handle(testPet2);
        }
        insertionAdapter.insertAndReturnId(testPet3);
        assertThat(mPetDao.count()).isEqualTo(2);
        assertThat(mPetDao.petWithId(232).getName()).isEqualTo(testPet2.getName());
    }

    @Test
    public void testUpsertWithMultipleEntity() {
        Pet[] testPets = TestUtil.createPetsForUser(0, 1, 10);
        Pet[] testPets2 = TestUtil.createPetsForUser(0, 5, 10);
        long[] resultArray;
        long[] resultArray2 = new long[10];
        EntityInsertionAdapter<Pet> insertionAdapter =
                new EntityInsertionAdapter<Pet>(mTestDatabase) {
                    @Override
                    protected void bind(@Nullable SupportSQLiteStatement statement, Pet entity) {
                        statement.bindLong(1, entity.getPetId());
                        statement.bindLong(2, entity.getUserId());
                        statement.bindString(3, entity.getName());
                        statement.bindString(4, entity.getAdoptionDate().toString());
                    }

                    @NonNull
                    @Override
                    protected String createQuery() {
                        return "INSERT OR IGNORE INTO `Pet` (`mPetId`, `mUserId`, `mPetName`,"
                                + "`mAdoptionDate`)"
                                + " VALUES (?,?,?,?)";
                    }
                };
        resultArray = insertionAdapter.insertAndReturnIdsArray(testPets);
        assertThat(resultArray[4]).isEqualTo(5);
        for (int i = 0; i < 10; i++) {
            resultArray2[i] = insertionAdapter.insertAndReturnId(testPets2[i]);
            if (resultArray2[i] == -1) {
                mUpdateAdapter.handle(testPets2[i]);
            }
        }
        assertThat(resultArray2[4]).isEqualTo(-1);
        assertThat(resultArray2[7]).isEqualTo(12);
        assertThat(mPetDao.petWithId(6).getName()).isEqualTo(testPets2[1].getName());
    }
}
