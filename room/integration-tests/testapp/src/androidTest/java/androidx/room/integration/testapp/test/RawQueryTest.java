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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.util.Collections.emptyList;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.room.integration.testapp.dao.RawDao;
import androidx.room.integration.testapp.vo.NameAndLastName;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;
import androidx.room.integration.testapp.vo.UserAndPet;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RawQueryTest extends TestDatabaseTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    @Test
    public void entity_null() {
        User user = mRawDao.getUser(new SimpleSQLiteQuery("SELECT * FROM User WHERE mId = 0"));
        assertThat(user, is(nullValue()));
    }

    @Test
    public void entity_one() {
        User expected = TestUtil.createUser(3);
        mUserDao.insert(expected);
        User received = mRawDao.getUser(new SimpleSQLiteQuery("SELECT * FROM User WHERE mId = ?",
                new Object[]{3}));
        assertThat(received, is(expected));
    }

    @Test
    public void entity_list() {
        List<User> expected = TestUtil.createUsersList(1, 2, 3, 4);
        mUserDao.insertAll(expected.toArray(new User[4]));
        List<User> received = mRawDao.getUserList(
                new SimpleSQLiteQuery("SELECT * FROM User ORDER BY mId ASC"));
        assertThat(received, is(expected));
    }

    @Test
    public void entity_liveData_string() throws TimeoutException, InterruptedException {
        SupportSQLiteQuery query = new SimpleSQLiteQuery(
                "SELECT * FROM User WHERE mId = ?",
                new Object[]{3}
        );
        liveDataTest(mRawDao.getUserLiveData(query));
    }

    @Test
    public void entity_liveData_supportQuery() throws TimeoutException, InterruptedException {
        liveDataTest(mRawDao.getUserLiveData(
                new SimpleSQLiteQuery("SELECT * FROM User WHERE mId = ?", new Object[]{3})));
    }

    private void liveDataTest(
            final LiveData<User> liveData) throws TimeoutException, InterruptedException {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> liveData.observeForever(user -> { }));
        drain();
        assertThat(liveData.getValue(), is(nullValue()));
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        assertThat(liveData.getValue(), is(user));
        user.setLastName("cxZ");
        mUserDao.insertOrReplace(user);
        drain();
        assertThat(liveData.getValue(), is(user));
    }

    @Test
    public void entity_supportSql() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        SimpleSQLiteQuery query = new SimpleSQLiteQuery("SELECT * FROM User WHERE mId = ?",
                new Object[]{3});
        User received = mRawDao.getUser(query);
        assertThat(received, is(user));
    }

    @Test
    public void embedded() {
        User user = TestUtil.createUser(3);
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 1);
        mUserDao.insert(user);
        mPetDao.insertAll(pets);
        UserAndPet received = mRawDao.getUserAndPet(new SimpleSQLiteQuery(
                "SELECT * FROM User, Pet WHERE User.mId = Pet.mUserId LIMIT 1"));
        assertThat(received.getUser(), is(user));
        assertThat(received.getPet(), is(pets[0]));
    }

    @Test
    public void relation() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 10);
        mPetDao.insertAll(pets);
        UserAndAllPets result = mRawDao
                .getUserAndAllPets(new SimpleSQLiteQuery("SELECT * FROM User WHERE mId = ?",
                        new Object[]{3}));
        assertThat(result.user, is(user));
        assertThat(result.pets, is(Arrays.asList(pets)));
    }

    @Test
    public void pojo() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        NameAndLastName result =
                mRawDao.getUserNameAndLastName(new SimpleSQLiteQuery("SELECT * FROM User"));
        assertThat(result, is(new NameAndLastName(user.getName(), user.getLastName())));
    }

    @Test
    public void pojo_supportSql() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        NameAndLastName result =
                mRawDao.getUserNameAndLastNameWithObserved(new SimpleSQLiteQuery(
                        "SELECT * FROM User WHERE mId = ?",
                        new Object[]{3}
                ));
        assertThat(result, is(new NameAndLastName(user.getName(), user.getLastName())));
    }

    @Test
    public void pojo_typeConverter() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        RawDao.UserNameAndBirthday result = mRawDao.getUserAndBirthday(
                new SimpleSQLiteQuery("SELECT mName, mBirthday FROM user LIMIT 1"));
        assertThat(result.name, is(user.getName()));
        assertThat(result.birthday, is(user.getBirthday()));
    }

    @Test
    public void embedded_nullField() {
        User user = TestUtil.createUser(3);
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 1);
        mUserDao.insert(user);
        mPetDao.insertAll(pets);
        UserAndPet received = mRawDao.getUserAndPet(
                new SimpleSQLiteQuery("SELECT * FROM User LIMIT 1"));
        assertThat(received.getUser(), is(user));
        assertThat(received.getPet(), is(nullValue()));
    }

    @Test
    public void embedded_list() {
        User[] users = TestUtil.createUsersArray(3, 5);
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mUserDao.insertAll(users);
        mPetDao.insertAll(pets);
        List<UserAndPet> received = mRawDao.getUserAndPetList(
                new SimpleSQLiteQuery(
                        "SELECT * FROM User LEFT JOIN Pet ON (User.mId = Pet.mUserId)"
                        + " ORDER BY mId ASC, mPetId ASC"));
        assertThat(received.size(), is(3));
        // row 0
        assertThat(received.get(0).getUser(), is(users[0]));
        assertThat(received.get(0).getPet(), is(pets[0]));
        // row 1
        assertThat(received.get(1).getUser(), is(users[0]));
        assertThat(received.get(1).getPet(), is(pets[1]));
        // row 2
        assertThat(received.get(2).getUser(), is(users[1]));
        assertThat(received.get(2).getPet(), is(nullValue()));
    }

    @Test
    public void count() {
        mUserDao.insertAll(TestUtil.createUsersArray(3, 5, 7, 10));
        int count = mRawDao.count(new SimpleSQLiteQuery("SELECT COUNT(*) FROM User"));
        assertThat(count, is(4));
    }

    @Test
    public void embedded_liveData() throws TimeoutException, InterruptedException {
        LiveData<List<UserAndPet>> liveData = mRawDao.getUserAndPetListObservable(
                new SimpleSQLiteQuery("SELECT * FROM User LEFT JOIN Pet ON (User.mId = Pet.mUserId)"
                        + " ORDER BY mId ASC, mPetId ASC"));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> liveData.observeForever(user -> {
                })
        );
        drain();
        assertThat(liveData.getValue(), is(emptyList()));

        User[] users = TestUtil.createUsersArray(3, 5);
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mUserDao.insertAll(users);
        drain();
        List<UserAndPet> justUsers = liveData.getValue();
        //noinspection ConstantConditions
        assertThat(justUsers.size(), is(2));
        assertThat(justUsers.get(0).getUser(), is(users[0]));
        assertThat(justUsers.get(1).getUser(), is(users[1]));
        assertThat(justUsers.get(0).getPet(), is(nullValue()));
        assertThat(justUsers.get(1).getPet(), is(nullValue()));

        mPetDao.insertAll(pets);
        drain();
        List<UserAndPet> allItems = liveData.getValue();
        //noinspection ConstantConditions
        assertThat(allItems.size(), is(3));
        // row 0
        assertThat(allItems.get(0).getUser(), is(users[0]));
        assertThat(allItems.get(0).getPet(), is(pets[0]));
        // row 1
        assertThat(allItems.get(1).getUser(), is(users[0]));
        assertThat(allItems.get(1).getPet(), is(pets[1]));
        // row 2
        assertThat(allItems.get(2).getUser(), is(users[1]));
        assertThat(allItems.get(2).getPet(), is(nullValue()));

        mDatabase.clearAllTables();
        drain();
        assertThat(liveData.getValue(), is(emptyList()));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void relation_liveData() throws TimeoutException, InterruptedException {
        LiveData<UserAndAllPets> liveData = mRawDao
                .getUserAndAllPetsObservable(
                        new SimpleSQLiteQuery("SELECT * FROM User WHERE mId = ?",
                                new Object[]{3}));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> liveData.observeForever(user -> {
                })
        );
        drain();
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        assertThat(liveData.getValue().user, is(user));
        assertThat(liveData.getValue().pets, is(emptyList()));
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 5);
        mPetDao.insertAll(pets);
        drain();
        assertThat(liveData.getValue().user, is(user));
        assertThat(liveData.getValue().pets, is(Arrays.asList(pets)));
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }
}
