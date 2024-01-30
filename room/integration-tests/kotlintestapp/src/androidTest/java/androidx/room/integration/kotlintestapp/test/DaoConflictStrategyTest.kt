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
package androidx.room.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.room.Room
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.dao.PetDao
import androidx.room.integration.kotlintestapp.dao.ToyDao
import androidx.room.integration.kotlintestapp.vo.Pet
import androidx.room.integration.kotlintestapp.vo.Toy
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DaoConflictStrategyTest {
    private lateinit var mToyDao: ToyDao
    private lateinit var mOriginalToy: Toy
    private lateinit var mPetDao: PetDao
    private lateinit var mPet: Pet

    @Before
    fun createDbAndSetUpToys() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db: TestDatabase =
            Room.inMemoryDatabaseBuilder(context, TestDatabase::class.java).build()
        mToyDao = db.toyDao()
        mPetDao = db.petDao()
        mPet = TestUtil.createPet(1)
        mOriginalToy = Toy(10, "originalToy", 1)
        mPetDao.insertOrReplace(mPet)
        mToyDao.insert(mOriginalToy)
    }

    @Test
    fun testInsertOnConflictReplace() {
        val newToy = Toy(10, "newToy", 1)
        mToyDao.insertOrReplace(newToy)
        val output: Toy? = mToyDao.getToy(10)
        assertThat(output).isNotNull()
        assertThat(output!!.mName).isEqualTo(newToy.mName)
    }

    @Test
    fun testInsertOnConflictIgnore() {
        val newToy = Toy(10, "newToy", 1)
        mToyDao.insertOrIgnore(newToy)
        val output: Toy? = mToyDao.getToy(10)
        assertThat(output).isNotNull()
        assertThat(output!!.mName).isEqualTo(mOriginalToy.mName)
    }

    @Test
    fun testUpdateOnConflictReplace() {
        val newToy = Toy(11, "newToy", 1)
        mToyDao.insert(newToy)
        val conflictToy = Toy(11, "originalToy", 1)
        mToyDao.updateOrReplace(conflictToy)

        // Conflicting row is deleted
        assertThat(mToyDao.getToy(10)).isNull()

        // Row is updated
        val output: Toy? = mToyDao.getToy(11)
        assertThat(output).isNotNull()
        assertThat(output!!.mName).isEqualTo(conflictToy.mName)
    }

    @Test
    fun testUpdateOnConflictIgnore() {
        val newToy = Toy(11, "newToy", 1)
        mToyDao.insert(newToy)
        val conflictToy = Toy(11, "newToy", 1)
        mToyDao.updateOrIgnore(conflictToy)

        // Conflicting row is kept
        assertThat(mToyDao.getToy(10)).isNotNull()

        // Row is not updated
        val output: Toy? = mToyDao.getToy(11)
        assertThat(output).isNotNull()
        assertThat(output!!.mName).isEqualTo(newToy.mName)
    }
}
