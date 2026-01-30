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
package androidx.room3.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room3.integration.kotlintestapp.dao.PetDao
import androidx.room3.integration.kotlintestapp.dao.ToyDao
import androidx.room3.integration.kotlintestapp.vo.Pet
import androidx.room3.integration.kotlintestapp.vo.Toy
import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class DaoConflictStrategyTest(useDriver: UseDriver) : TestDatabaseTest(useDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.BUNDLED, UseDriver.ANDROID)
    }

    private lateinit var toyDao: ToyDao
    private lateinit var originalToy: Toy
    private lateinit var petDao: PetDao
    private lateinit var pet: Pet

    @Before
    fun createDbAndSetUpToys() {
        toyDao = database.toyDao()
        petDao = database.petDao()
        pet = TestUtil.createPet(1)
        originalToy = Toy(10, "originalToy", 1)
        petDao.insertOrReplace(pet)
        toyDao.insert(originalToy)
    }

    @Test
    fun testInsertOnConflictReplace() {
        val newToy = Toy(10, "newToy", 1)
        toyDao.insertOrReplace(newToy)
        val output: Toy? = toyDao.getToy(10)
        assertThat(output).isNotNull()
        assertThat(output!!.name).isEqualTo(newToy.name)
    }

    @Test
    fun testInsertOnConflictIgnore() {
        val newToy = Toy(10, "newToy", 1)
        toyDao.insertOrIgnore(newToy)
        val output: Toy? = toyDao.getToy(10)
        assertThat(output).isNotNull()
        assertThat(output!!.name).isEqualTo(originalToy.name)
    }

    @Test
    fun testUpdateOnConflictReplace() {
        val newToy = Toy(11, "newToy", 1)
        toyDao.insert(newToy)
        val conflictToy = Toy(11, "originalToy", 1)
        toyDao.updateOrReplace(conflictToy)

        // Conflicting row is deleted
        assertThat(toyDao.getToy(10)).isNull()

        // Row is updated
        val output: Toy? = toyDao.getToy(11)
        assertThat(output).isNotNull()
        assertThat(output!!.name).isEqualTo(conflictToy.name)
    }

    @Test
    fun testUpdateOnConflictIgnore() {
        val newToy = Toy(11, "newToy", 1)
        toyDao.insert(newToy)
        val conflictToy = Toy(11, "newToy", 1)
        toyDao.updateOrIgnore(conflictToy)

        // Conflicting row is kept
        assertThat(toyDao.getToy(10)).isNotNull()

        // Row is not updated
        val output: Toy? = toyDao.getToy(11)
        assertThat(output).isNotNull()
        assertThat(output!!.name).isEqualTo(newToy.name)
    }
}
