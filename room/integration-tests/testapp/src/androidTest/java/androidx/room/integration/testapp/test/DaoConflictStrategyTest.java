/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Context;

import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.dao.ToyDao;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.Toy;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DaoConflictStrategyTest {

    private ToyDao mToyDao;
    private Toy mOriginalToy;
    private PetDao mPetDao;
    private Pet mPet;

    @Before
    public void createDbAndSetUpToys() {
        Context context = ApplicationProvider.getApplicationContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mToyDao = db.getToyDao();
        mPetDao = db.getPetDao();

        mPet = TestUtil.createPet(1);
        mOriginalToy = new Toy();
        mOriginalToy.setId(10);
        mOriginalToy.setName("originalToy");
        mOriginalToy.setPetId(1);

        mPetDao.insertOrReplace(mPet);
        mToyDao.insert(mOriginalToy);
    }

    @Test
    public void testInsertOnConflictReplace() {
        Toy newToy = new Toy();
        newToy.setId(10);
        newToy.setName("newToy");
        newToy.setPetId(1);
        mToyDao.insertOrReplace(newToy);

        Toy output = mToyDao.getToy(10);
        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo(newToy.getName());
    }

    @Test
    public void testInsertOnConflictIgnore() {
        Toy newToy = new Toy();
        newToy.setId(10);
        newToy.setName("newToy");
        newToy.setPetId(1);
        mToyDao.insertOrIgnore(newToy);

        Toy output = mToyDao.getToy(10);
        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo(mOriginalToy.getName());
    }

    @Test
    public void testUpdateOnConflictReplace() {
        Toy newToy = new Toy();
        newToy.setId(11);
        newToy.setName("newToy");
        newToy.setPetId(1);
        mToyDao.insert(newToy);

        Toy conflictToy = new Toy();
        conflictToy.setId(11);
        conflictToy.setName("originalToy");
        conflictToy.setPetId(1);
        mToyDao.updateOrReplace(conflictToy);

        // Conflicting row is deleted
        assertThat(mToyDao.getToy(10)).isNull();

        // Row is updated
        Toy output = mToyDao.getToy(11);
        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo(conflictToy.getName());
    }

    @Test
    public void testUpdateOnConflictIgnore() {
        Toy newToy = new Toy();
        newToy.setId(11);
        newToy.setName("newToy");
        newToy.setPetId(1);
        mToyDao.insert(newToy);

        Toy conflictToy = new Toy();
        conflictToy.setId(11);
        conflictToy.setName("originalToy");
        conflictToy.setPetId(1);
        mToyDao.updateOrIgnore(conflictToy);

        // Conflicting row is kept
        assertThat(mToyDao.getToy(10)).isNotNull();

        // Row is not updated
        Toy output = mToyDao.getToy(11);
        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo(newToy.getName());
    }
}

