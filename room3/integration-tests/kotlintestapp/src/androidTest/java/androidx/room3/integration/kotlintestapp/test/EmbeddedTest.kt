/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context
import androidx.kruth.assertThat
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.dao.PetCoupleDao
import androidx.room3.integration.kotlintestapp.dao.PetDao
import androidx.room3.integration.kotlintestapp.dao.SchoolDao
import androidx.room3.integration.kotlintestapp.dao.UserPetDao
import androidx.room3.integration.kotlintestapp.dao.UsersDao
import androidx.room3.integration.kotlintestapp.vo.PetCouple
import androidx.room3.integration.kotlintestapp.vo.School
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class EmbeddedTest {
    private lateinit var db: TestDatabase
    private lateinit var userDao: UsersDao
    private lateinit var petDao: PetDao
    private lateinit var userPetDao: UserPetDao
    private lateinit var schoolDao: SchoolDao
    private lateinit var petCoupleDao: PetCoupleDao

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder<TestDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        userDao = db.usersDao()
        petDao = db.petDao()
        userPetDao = db.userPetDao()
        schoolDao = db.schoolDao()
        petCoupleDao = db.petCoupleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun loadAll() {
        val pet = TestUtil.createPet(1)
        val user = TestUtil.createUser(2)
        pet.userId = user.uId
        userDao.insertUser(user)
        petDao.insertOrReplace(pet)
        val all = userPetDao.loadAll()
        assertThat(all).hasSize(1)
        assertThat(all.single().user).isEqualTo(user)
        assertThat(all.single().pet).isEqualTo(pet)
    }

    @Test
    fun loadAllGeneric() {
        val pet = TestUtil.createPet(1)
        val user = TestUtil.createUser(2)
        pet.userId = user.uId
        userDao.insertUser(user)
        petDao.insertOrReplace(pet)
        val all = userPetDao.loadAllGeneric()
        assertThat(all).hasSize(1)
        assertThat(all.single().user).isEqualTo(user)
        assertThat(all.single().item).isEqualTo(pet)
    }

    @Test
    fun loadFromUsers() {
        val pet = TestUtil.createPet(1)
        val user = TestUtil.createUser(2)
        pet.userId = user.uId
        userDao.insertUser(user)
        petDao.insertOrReplace(pet)
        val all = userPetDao.loadUsers()
        assertThat(all).hasSize(1)
        assertThat(all.single().user).isEqualTo(user)
        assertThat(all.single().pet).isEqualTo(pet)
    }

    @Test
    fun loadFromUsersWithNullPet() {
        val user = TestUtil.createUser(2)
        userDao.insertUser(user)
        val all = userPetDao.loadUsers()
        assertThat(all).hasSize(1)
        assertThat(all.single().user).isEqualTo(user)
        assertThat(all.single().pet).isNull()
    }

    @Test
    fun loadFromPets() {
        val pet = TestUtil.createPet(1)
        val user = TestUtil.createUser(2)
        pet.userId = user.uId
        userDao.insertUser(user)
        petDao.insertOrReplace(pet)
        val all = userPetDao.loadPets()
        assertThat(all).hasSize(1)
        assertThat(all.single().user).isEqualTo(user)
        assertThat(all.single().pet).isEqualTo(pet)
    }

    @Test
    fun loadFromPetsWithNullUser() {
        val pet = TestUtil.createPet(1)
        petDao.insertOrReplace(pet)
        val all = userPetDao.loadPets()
        assertThat(all).hasSize(1)
        assertThat(all.single().user).isNull()
        assertThat(all.single().pet).isEqualTo(pet)
    }

    @Test
    fun findSchoolByStreet() {
        val school = TestUtil.createSchool(3, 5)
        school.address!!.street = "foo"
        schoolDao.insert(school)
        val result = schoolDao.findByStreet("foo")
        assertThat(result).hasSize(1)
        assertThat(result.single()).isEqualTo(school)
    }

    @Test
    @Ignore
    fun loadSubFieldsAsPoKo() {
        loadSubFieldsTest { schoolDao.schoolAndManagerIdsAsPoKo().map { it } }
    }

    @Test
    fun loadSubFieldsAsEntity() {
        loadSubFieldsTest { schoolDao.schoolAndManagerNames() }
    }

    private fun loadSubFieldsTest(loader: () -> List<School>) {
        val school = TestUtil.createSchool(3, 5)
        school.name = "MTV High"
        school.manager!!.uId = 5
        schoolDao.insert(school)
        val school2 = TestUtil.createSchool(4, 6)
        school2.name = "MTV Low"
        school2.manager = null
        schoolDao.insert(school2)
        val schools = loader()
        assertThat(schools).hasSize(2)
        schools[0].let {
            assertThat(it.name).isEqualTo("MTV High")
            assertThat(it.address).isNull()
            assertThat(it.manager).isNotNull()
            assertThat(it.manager!!.uId).isEqualTo(5)
        }
        schools[1].let {
            assertThat(it.name).isEqualTo("MTV Low")
            assertThat(it.address).isNull()
            assertThat(it.manager).isNull()
        }
    }

    @Test
    fun loadNestedSub() {
        val school = TestUtil.createSchool(3, 5)
        school.address!!.coordinates!!.lat = 3.0
        school.address!!.coordinates!!.lng = 4.0
        schoolDao.insert(school)
        val coordinates = schoolDao.loadCoordinates(3)
        assertThat(coordinates.lat).isEqualTo(3.0)
        assertThat(coordinates.lng).isEqualTo(4.0)
        val asSchool = schoolDao.loadCoordinatesAsSchool(3)
        assertThat(asSchool.address!!.coordinates!!.lat).isEqualTo(3.0)
        assertThat(asSchool.address!!.coordinates!!.lng).isEqualTo(4.0)
        // didn't ask for it so don't load
        assertThat(asSchool.manager).isNull()
        assertThat(asSchool.address!!.street).isNull()
    }

    @Test
    fun sameFieldType() {
        val male = TestUtil.createPet(3)
        val female = TestUtil.createPet(5)
        val petCouple = PetCouple("foo", male, female)
        petCoupleDao.insert(petCouple)
        val petCouples = petCoupleDao.loadAll()
        assertThat(petCouples).hasSize(1)
        assertThat(petCouples.single().id).isEqualTo("foo")
        assertThat(petCouples.single().male).isEqualTo(male)
        assertThat(petCouples.single().female).isEqualTo(female)
    }

    @Test
    fun sameFieldOneNull() {
        val loneWolf = TestUtil.createPet(3)
        val petCouple = PetCouple("foo", loneWolf, null)
        petCoupleDao.insert(petCouple)
        val petCouples = petCoupleDao.loadAll()
        assertThat(petCouples).hasSize(1)
        assertThat(petCouples.single().id).isEqualTo("foo")
        assertThat(petCouples.single().male).isEqualTo(loneWolf)
        assertThat(petCouples.single().female).isNull()
    }
}
