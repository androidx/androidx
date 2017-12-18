/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.arch.persistence.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.arch.persistence.room.Room;
import android.arch.persistence.room.integration.testapp.TestDatabase;
import android.arch.persistence.room.integration.testapp.dao.BlobEntityDao;
import android.arch.persistence.room.integration.testapp.dao.PetDao;
import android.arch.persistence.room.integration.testapp.dao.ProductDao;
import android.arch.persistence.room.integration.testapp.dao.UserDao;
import android.arch.persistence.room.integration.testapp.dao.UserPetDao;
import android.arch.persistence.room.integration.testapp.vo.BlobEntity;
import android.arch.persistence.room.integration.testapp.vo.Pet;
import android.arch.persistence.room.integration.testapp.vo.Product;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.persistence.room.integration.testapp.vo.UserAndAllPets;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SimpleEntityReadWriteTest {
    private UserDao mUserDao;
    private BlobEntityDao mBlobEntityDao;
    private PetDao mPetDao;
    private UserPetDao mUserPetDao;
    private ProductDao mProductDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = db.getUserDao();
        mPetDao = db.getPetDao();
        mUserPetDao = db.getUserPetDao();
        mBlobEntityDao = db.getBlobEntityDao();
        mProductDao = db.getProductDao();
    }

    @Test
    public void writeUserAndReadInList() throws Exception {
        User user = TestUtil.createUser(3);
        user.setName("george");
        mUserDao.insert(user);
        List<User> byName = mUserDao.findUsersByName("george");
        assertThat(byName.get(0), equalTo(user));
    }

    @Test
    public void insertNull() throws Exception {
        @SuppressWarnings("ConstantConditions")
        Product product = new Product(1, null);
        Throwable throwable = null;
        try {
            mProductDao.insert(product);
        } catch (Throwable t) {
            throwable = t;
        }
        assertNotNull("Was expecting an exception", throwable);
        assertThat(throwable, instanceOf(SQLiteConstraintException.class));
    }

    @Test
    public void insertDifferentEntities() throws Exception {
        User user1 = TestUtil.createUser(3);
        user1.setName("george");
        Pet pet = TestUtil.createPet(1);
        pet.setUserId(3);
        pet.setName("a");
        mUserPetDao.insertUserAndPet(user1, pet);
        assertThat(mUserDao.count(), is(1));
        List<UserAndAllPets> inserted = mUserPetDao.loadAllUsersWithTheirPets();
        assertThat(inserted, hasSize(1));
        assertThat(inserted.get(0).user.getId(), is(3));
        assertThat(inserted.get(0).user.getName(), is(equalTo("george")));
        assertThat(inserted.get(0).pets, hasSize(1));
        assertThat(inserted.get(0).pets.get(0).getPetId(), is(1));
        assertThat(inserted.get(0).pets.get(0).getName(), is("a"));
        assertThat(inserted.get(0).pets.get(0).getUserId(), is(3));
        pet.setName("b");
        mUserPetDao.updateUsersAndPets(new User[]{user1}, new Pet[]{pet});
        List<UserAndAllPets> updated = mUserPetDao.loadAllUsersWithTheirPets();
        assertThat(updated, hasSize(1));
        assertThat(updated.get(0).pets, hasSize(1));
        assertThat(updated.get(0).pets.get(0).getName(), is("b"));
        User user2 = TestUtil.createUser(5);
        user2.setName("chet");
        mUserDao.insert(user2);
        assertThat(mUserDao.count(), is(2));
        mUserPetDao.delete2UsersAndPets(user1, user2, new Pet[]{pet});
        List<UserAndAllPets> deleted = mUserPetDao.loadAllUsersWithTheirPets();
        assertThat(deleted, hasSize(0));
    }

    @Test
    public void insertDifferentEntities_transaction() throws Exception {
        Pet pet = TestUtil.createPet(1);
        mPetDao.insertOrReplace(pet);
        assertThat(mPetDao.count(), is(1));
        User user = TestUtil.createUser(3);
        try {
            mUserPetDao.insertUserAndPet(user, pet);
            fail("Exception expected");
        } catch (SQLiteConstraintException ignored) {
        }
        assertThat(mUserDao.count(), is(0));
        assertThat(mPetDao.count(), is(1));
    }

    @Test
    public void throwExceptionOnConflict() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);

        User user2 = TestUtil.createUser(3);
        try {
            mUserDao.insert(user2);
            throw new AssertionFailedError("didn't throw in conflicting insertion");
        } catch (SQLiteException ignored) {
        }
    }

    @Test
    public void replaceOnConflict() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);

        User user2 = TestUtil.createUser(3);
        mUserDao.insertOrReplace(user2);

        assertThat(mUserDao.load(3), equalTo(user2));
        assertThat(mUserDao.load(3), not(equalTo(user)));
    }

    @Test
    public void updateSimple() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        user.setName("i am an updated name");
        assertThat(mUserDao.update(user), is(1));
        assertThat(mUserDao.load(user.getId()), equalTo(user));
    }

    @Test
    public void updateNonExisting() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        User user2 = TestUtil.createUser(4);
        assertThat(mUserDao.update(user2), is(0));
    }

    @Test
    public void updateList() {
        List<User> users = TestUtil.createUsersList(3, 4, 5);
        mUserDao.insertAll(users.toArray(new User[3]));
        for (User user : users) {
            user.setName("name " + user.getId());
        }
        assertThat(mUserDao.updateAll(users), is(3));
        for (User user : users) {
            assertThat(mUserDao.load(user.getId()).getName(), is("name " + user.getId()));
        }
    }

    @Test
    public void updateListPartial() {
        List<User> existingUsers = TestUtil.createUsersList(3, 4, 5);
        mUserDao.insertAll(existingUsers.toArray(new User[3]));
        for (User user : existingUsers) {
            user.setName("name " + user.getId());
        }
        List<User> allUsers = TestUtil.createUsersList(7, 8, 9);
        allUsers.addAll(existingUsers);
        assertThat(mUserDao.updateAll(allUsers), is(3));
        for (User user : existingUsers) {
            assertThat(mUserDao.load(user.getId()).getName(), is("name " + user.getId()));
        }
    }

    @Test
    public void delete() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        assertThat(mUserDao.delete(user), is(1));
        assertThat(mUserDao.delete(user), is(0));
        assertThat(mUserDao.load(3), is(nullValue()));
    }

    @Test
    public void deleteAll() {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9);
        mUserDao.insertAll(users);
        // there is actually no guarantee for this order by works fine since they are ordered for
        // the test and it is a new database (no pages to recycle etc)
        assertThat(mUserDao.loadByIds(3, 5, 7, 9), is(users));
        int deleteCount = mUserDao.deleteAll(new User[]{users[0], users[3],
                TestUtil.createUser(9)});
        assertThat(deleteCount, is(2));
        assertThat(mUserDao.loadByIds(3, 5, 7, 9), is(new User[]{users[1], users[2]}));
    }

    @Test
    public void deleteEverything() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        assertThat(mUserDao.count(), is(1));
        int count = mUserDao.deleteEverything();
        assertThat(count, is(1));
        assertThat(mUserDao.count(), is(0));
    }

    @Test
    public void findByBoolean() {
        User user1 = TestUtil.createUser(3);
        user1.setAdmin(true);
        User user2 = TestUtil.createUser(5);
        user2.setAdmin(false);
        mUserDao.insert(user1);
        mUserDao.insert(user2);
        assertThat(mUserDao.findByAdmin(true), is(Arrays.asList(user1)));
        assertThat(mUserDao.findByAdmin(false), is(Arrays.asList(user2)));
    }

    @Test
    public void findByCollateNoCase() {
        User user = TestUtil.createUser(3);
        user.setCustomField("abc");
        mUserDao.insert(user);
        List<User> users = mUserDao.findByCustomField("ABC");
        assertThat(users, hasSize(1));
        assertThat(users.get(0).getId(), is(3));
    }

    @Test
    public void deleteByAge() {
        User user1 = TestUtil.createUser(3);
        user1.setAge(30);
        User user2 = TestUtil.createUser(5);
        user2.setAge(45);
        mUserDao.insert(user1);
        mUserDao.insert(user2);
        assertThat(mUserDao.deleteAgeGreaterThan(60), is(0));
        assertThat(mUserDao.deleteAgeGreaterThan(45), is(0));
        assertThat(mUserDao.deleteAgeGreaterThan(35), is(1));
        assertThat(mUserDao.loadByIds(3, 5), is(new User[]{user1}));
    }

    @Test
    public void deleteByAgeRange() {
        User user1 = TestUtil.createUser(3);
        user1.setAge(30);
        User user2 = TestUtil.createUser(5);
        user2.setAge(45);
        mUserDao.insert(user1);
        mUserDao.insert(user2);
        assertThat(mUserDao.deleteByAgeRange(35, 40), is(0));
        assertThat(mUserDao.deleteByAgeRange(25, 30), is(1));
        assertThat(mUserDao.loadByIds(3, 5), is(new User[]{user2}));
    }

    @Test
    public void deleteByUIds() {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9, 11);
        mUserDao.insertAll(users);
        assertThat(mUserDao.deleteByUids(2, 4, 6), is(0));
        assertThat(mUserDao.deleteByUids(3, 11), is(2));
        assertThat(mUserDao.loadByIds(3, 5, 7, 9, 11), is(new User[]{
                users[1], users[2], users[3]
        }));
    }

    @Test
    public void updateNameById() {
        User[] usersArray = TestUtil.createUsersArray(3, 5, 7);
        mUserDao.insertAll(usersArray);
        assertThat("test sanity", usersArray[1].getName(), not(equalTo("updated name")));
        int changed = mUserDao.updateById(5, "updated name");
        assertThat(changed, is(1));
        assertThat(mUserDao.load(5).getName(), is("updated name"));
    }

    @Test
    public void incrementIds() {
        User[] usersArr = TestUtil.createUsersArray(2, 4, 6);
        mUserDao.insertAll(usersArr);
        mUserDao.incrementIds(1);
        assertThat(mUserDao.loadIds(), is(Arrays.asList(3, 5, 7)));
    }

    @Test
    public void incrementAgeOfAll() {
        User[] users = TestUtil.createUsersArray(3, 5, 7);
        users[0].setAge(3);
        users[1].setAge(5);
        users[2].setAge(7);
        mUserDao.insertAll(users);
        assertThat(mUserDao.count(), is(3));
        mUserDao.incrementAgeOfAll();
        for (User user : mUserDao.loadByIds(3, 5, 7)) {
            assertThat(user.getAge(), is(user.getId() + 1));
        }
    }

    @Test
    public void findByIntQueryParameter() {
        User user = TestUtil.createUser(1);
        final String name = "my name";
        user.setName(name);
        mUserDao.insert(user);
        assertThat(mUserDao.findByNameLength(name.length()), is(Collections.singletonList(user)));
    }

    @Test
    public void findByIntFieldMatch() {
        User user = TestUtil.createUser(1);
        user.setAge(19);
        mUserDao.insert(user);
        assertThat(mUserDao.findByAge(19), is(Collections.singletonList(user)));
    }

    @Test
    public void customConverterField() {
        User user = TestUtil.createUser(20);
        Date theDate = new Date(System.currentTimeMillis() - 200);
        user.setBirthday(theDate);
        mUserDao.insert(user);
        assertThat(mUserDao.findByBirthdayRange(new Date(theDate.getTime() - 100),
                new Date(theDate.getTime() + 1)).get(0), is(user));
        assertThat(mUserDao.findByBirthdayRange(new Date(theDate.getTime()),
                new Date(theDate.getTime() + 1)).size(), is(0));
    }

    @Test
    public void renamedField() {
        User user = TestUtil.createUser(3);
        user.setCustomField("foo laaa");
        mUserDao.insertOrReplace(user);
        User loaded = mUserDao.load(3);
        assertThat(loaded.getCustomField(), is("foo laaa"));
        assertThat(loaded, is(user));
    }

    @Test
    public void readViaCursor() {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9);
        mUserDao.insertAll(users);
        Cursor cursor = mUserDao.findUsersAsCursor(3, 5, 9);
        try {
            assertThat(cursor.getCount(), is(3));
            assertThat(cursor.moveToNext(), is(true));
            assertThat(cursor.getInt(0), is(3));
            assertThat(cursor.moveToNext(), is(true));
            assertThat(cursor.getInt(0), is(5));
            assertThat(cursor.moveToNext(), is(true));
            assertThat(cursor.getInt(0), is(9));
            assertThat(cursor.moveToNext(), is(false));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void readDirectWithTypeAdapter() {
        User user = TestUtil.createUser(3);
        user.setBirthday(null);
        mUserDao.insert(user);
        assertThat(mUserDao.getBirthday(3), is(nullValue()));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 3);
        Date birthday = calendar.getTime();
        user.setBirthday(birthday);

        mUserDao.update(user);
        assertThat(mUserDao.getBirthday(3), is(birthday));
    }

    @Test
    public void emptyInQuery() {
        User[] users = mUserDao.loadByIds();
        assertThat(users, is(new User[0]));
    }

    @Test
    public void blob() {
        BlobEntity a = new BlobEntity(1, "abc".getBytes());
        BlobEntity b = new BlobEntity(2, "def".getBytes());
        mBlobEntityDao.insert(a);
        mBlobEntityDao.insert(b);
        List<BlobEntity> list = mBlobEntityDao.selectAll();
        assertThat(list, hasSize(2));
        mBlobEntityDao.updateContent(2, "ghi".getBytes());
        assertThat(mBlobEntityDao.getContent(2), is(equalTo("ghi".getBytes())));
    }

    @Test
    public void transactionByRunnable() {
        User a = TestUtil.createUser(3);
        User b = TestUtil.createUser(5);
        mUserDao.insertBothByRunnable(a, b);
        assertThat(mUserDao.count(), is(2));
    }

    @Test
    public void transactionByRunnable_failure() {
        User a = TestUtil.createUser(3);
        User b = TestUtil.createUser(3);
        boolean caught = false;
        try {
            mUserDao.insertBothByRunnable(a, b);
        } catch (SQLiteConstraintException e) {
            caught = true;
        }
        assertTrue("SQLiteConstraintException expected", caught);
        assertThat(mUserDao.count(), is(0));
    }

    @Test
    public void transactionByCallable() {
        User a = TestUtil.createUser(3);
        User b = TestUtil.createUser(5);
        int count = mUserDao.insertBothByCallable(a, b);
        assertThat(mUserDao.count(), is(2));
        assertThat(count, is(2));
    }

    @Test
    public void transactionByCallable_failure() {
        User a = TestUtil.createUser(3);
        User b = TestUtil.createUser(3);
        boolean caught = false;
        try {
            mUserDao.insertBothByCallable(a, b);
        } catch (SQLiteConstraintException e) {
            caught = true;
        }
        assertTrue("SQLiteConstraintException expected", caught);
        assertThat(mUserDao.count(), is(0));
    }

    @Test
    public void multipleInParamsFollowedByASingleParam_delete() {
        User user = TestUtil.createUser(3);
        user.setAge(30);
        mUserDao.insert(user);
        assertThat(mUserDao.deleteByAgeAndIds(20, Arrays.asList(3, 5)), is(0));
        assertThat(mUserDao.count(), is(1));
        assertThat(mUserDao.deleteByAgeAndIds(30, Arrays.asList(3, 5)), is(1));
        assertThat(mUserDao.count(), is(0));
    }

    @Test
    public void multipleInParamsFollowedByASingleParam_update() {
        User user = TestUtil.createUser(3);
        user.setAge(30);
        user.setWeight(10f);
        mUserDao.insert(user);
        assertThat(mUserDao.updateByAgeAndIds(3f, 20, Arrays.asList(3, 5)), is(0));
        assertThat(mUserDao.loadByIds(3)[0].getWeight(), is(10f));
        assertThat(mUserDao.updateByAgeAndIds(3f, 30, Arrays.asList(3, 5)), is(1));
        assertThat(mUserDao.loadByIds(3)[0].getWeight(), is(3f));
    }

    @Test
    public void transactionByAnnotation() {
        User a = TestUtil.createUser(3);
        User b = TestUtil.createUser(5);
        mUserDao.insertBothByAnnotation(a, b);
        assertThat(mUserDao.count(), is(2));
    }

    @Test
    public void transactionByAnnotation_failure() {
        User a = TestUtil.createUser(3);
        User b = TestUtil.createUser(3);
        boolean caught = false;
        try {
            mUserDao.insertBothByAnnotation(a, b);
        } catch (SQLiteConstraintException e) {
            caught = true;
        }
        assertTrue("SQLiteConstraintException expected", caught);
        assertThat(mUserDao.count(), is(0));
    }
}
