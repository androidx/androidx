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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.PetCoupleDao;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.dao.SchoolDao;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.dao.UserPetDao;
import androidx.room.integration.testapp.vo.Coordinates;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetCouple;
import androidx.room.integration.testapp.vo.School;
import androidx.room.integration.testapp.vo.SchoolRef;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndGenericPet;
import androidx.room.integration.testapp.vo.UserAndPet;
import androidx.room.integration.testapp.vo.UserAndPetNonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmbeddedTest {
    private UserDao mUserDao;
    private PetDao mPetDao;
    private UserPetDao mUserPetDao;
    private SchoolDao mSchoolDao;
    private PetCoupleDao mPetCoupleDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = db.getUserDao();
        mPetDao = db.getPetDao();
        mUserPetDao = db.getUserPetDao();
        mSchoolDao = db.getSchoolDao();
        mPetCoupleDao = db.getPetCoupleDao();
    }

    @Test
    public void loadAll() {
        Pet pet = TestUtil.createPet(1);
        User user = TestUtil.createUser(2);
        pet.setUserId(user.getId());
        mUserDao.insert(user);
        mPetDao.insertOrReplace(pet);
        List<UserAndPet> all = mUserPetDao.loadAll();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUser(), is(user));
        assertThat(all.get(0).getPet(), is(pet));
    }

    @Test
    public void loadAllGeneric() {
        Pet pet = TestUtil.createPet(1);
        User user = TestUtil.createUser(2);
        pet.setUserId(user.getId());
        mUserDao.insert(user);
        mPetDao.insertOrReplace(pet);
        List<UserAndGenericPet> all = mUserPetDao.loadAllGeneric();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).user, is(user));
        assertThat(all.get(0).item, is(pet));
    }

    @Test
    public void loadFromUsers() {
        Pet pet = TestUtil.createPet(1);
        User user = TestUtil.createUser(2);
        pet.setUserId(user.getId());
        mUserDao.insert(user);
        mPetDao.insertOrReplace(pet);
        List<UserAndPet> all = mUserPetDao.loadUsers();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUser(), is(user));
        assertThat(all.get(0).getPet(), is(pet));
    }

    @Test
    public void loadFromUsersWithNullPet() {
        User user = TestUtil.createUser(2);
        mUserDao.insert(user);
        List<UserAndPet> all = mUserPetDao.loadUsers();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUser(), is(user));
        assertThat(all.get(0).getPet(), is(nullValue()));
    }

    @Test
    public void loadFromUsersWithNonNullPet() {
        User user = TestUtil.createUser(2);
        mUserDao.insert(user);
        List<UserAndPetNonNull> all = mUserPetDao.loadUsersWithNonNullPet();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUser(), is(user));
        assertThat(all.get(0).getPet(), is(new Pet()));
    }

    @Test
    public void loadFromPets() {
        Pet pet = TestUtil.createPet(1);
        User user = TestUtil.createUser(2);
        pet.setUserId(user.getId());
        mUserDao.insert(user);
        mPetDao.insertOrReplace(pet);
        List<UserAndPet> all = mUserPetDao.loadPets();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUser(), is(user));
        assertThat(all.get(0).getPet(), is(pet));
    }

    @Test
    public void loadFromPetsWithNullUser() {
        Pet pet = TestUtil.createPet(1);
        mPetDao.insertOrReplace(pet);
        List<UserAndPet> all = mUserPetDao.loadPets();
        assertThat(all.size(), is(1));
        assertThat(all.get(0).getUser(), is(nullValue()));
        assertThat(all.get(0).getPet(), is(pet));
    }

    @Test
    public void findSchoolByStreet() {
        School school = TestUtil.createSchool(3, 5);
        school.getAddress().setStreet("foo");
        mSchoolDao.insert(school);
        List<School> result = mSchoolDao.findByStreet("foo");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(school));
    }

    @Test
    public void loadSubFieldsAsPojo() throws Exception {
        loadSubFieldsTest(new Callable<List<School>>() {
            @Override
            public List<School> call() throws Exception {
                List<School> result = new ArrayList<>();
                for (SchoolRef ref : mSchoolDao.schoolAndManagerNamesAsPojo()) {
                    result.add(ref);
                }
                return result;
            }
        });
    }

    @Test
    public void loadSubFieldsAsEntity() throws Exception {
        loadSubFieldsTest(new Callable<List<School>>() {
            @Override
            public List<School> call() throws Exception {
                return mSchoolDao.schoolAndManagerNames();
            }
        });
    }

    public void loadSubFieldsTest(Callable<List<School>> loader) throws Exception {
        School school = TestUtil.createSchool(3, 5);
        school.setName("MTV High");
        school.getManager().setName("chet");
        mSchoolDao.insert(school);

        School school2 = TestUtil.createSchool(4, 6);
        school2.setName("MTV Low");
        school2.setManager(null);
        mSchoolDao.insert(school2);

        List<School> schools = loader.call();
        assertThat(schools.size(), is(2));
        assertThat(schools.get(0).getName(), is("MTV High"));
        assertThat(schools.get(1).getName(), is("MTV Low"));
        assertThat(schools.get(0).address, nullValue());
        assertThat(schools.get(1).address, nullValue());
        assertThat(schools.get(0).getManager(), notNullValue());
        assertThat(schools.get(1).getManager(), nullValue());
        assertThat(schools.get(0).getManager().getName(), is("chet"));
    }

    @Test
    public void loadNestedSub() {
        School school = TestUtil.createSchool(3, 5);
        school.getAddress().getCoordinates().lat = 3.;
        school.getAddress().getCoordinates().lng = 4.;
        mSchoolDao.insert(school);
        Coordinates coordinates = mSchoolDao.loadCoordinates(3);
        assertThat(coordinates.lat, is(3.));
        assertThat(coordinates.lng, is(4.));

        School asSchool = mSchoolDao.loadCoordinatesAsSchool(3);
        assertThat(asSchool.address.getCoordinates().lat, is(3.));
        assertThat(asSchool.address.getCoordinates().lng, is(4.));
        // didn't as for it so don't load
        assertThat(asSchool.getManager(), nullValue());
        assertThat(asSchool.address.getStreet(), nullValue());
    }

    @Test
    public void sameFieldType() {
        Pet male = TestUtil.createPet(3);
        Pet female = TestUtil.createPet(5);
        PetCouple petCouple = new PetCouple();
        petCouple.id = "foo";
        petCouple.male = male;
        petCouple.setFemale(female);
        mPetCoupleDao.insert(petCouple);
        List<PetCouple> petCouples = mPetCoupleDao.loadAll();
        assertThat(petCouples.size(), is(1));
        PetCouple loaded = petCouples.get(0);
        assertThat(loaded.id, is("foo"));
        assertThat(loaded.male, is(male));
        assertThat(loaded.getFemale(), is(female));
    }

    @Test
    public void sameFieldOneNull() {
        Pet loneWolf = TestUtil.createPet(3);
        PetCouple petCouple = new PetCouple();
        petCouple.id = "foo";
        petCouple.male = loneWolf;
        mPetCoupleDao.insert(petCouple);
        List<PetCouple> petCouples = mPetCoupleDao.loadAll();
        assertThat(petCouples.size(), is(1));
        PetCouple loaded = petCouples.get(0);
        assertThat(loaded.id, is("foo"));
        assertThat(loaded.male, is(loneWolf));
        assertThat(loaded.getFemale(), is(nullValue()));
    }
}
