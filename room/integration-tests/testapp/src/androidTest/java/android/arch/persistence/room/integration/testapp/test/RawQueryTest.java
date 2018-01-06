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

package android.arch.persistence.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.room.integration.testapp.dao.RawDao;
import android.arch.persistence.room.integration.testapp.vo.NameAndLastName;
import android.arch.persistence.room.integration.testapp.vo.Pet;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.persistence.room.integration.testapp.vo.UserAndAllPets;
import android.arch.persistence.room.integration.testapp.vo.UserAndPet;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

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
        User user = mRawDao.getUser("SELECT * FROM User WHERE mId = 0");
        assertThat(user, is(nullValue()));
    }

    @Test
    public void entity_one() {
        User expected = TestUtil.createUser(3);
        mUserDao.insert(expected);
        User received = mRawDao.getUser("SELECT * FROM User WHERE mId = 3");
        assertThat(received, is(expected));
    }

    @Test
    public void entity_list() {
        List<User> expected = TestUtil.createUsersList(1, 2, 3, 4);
        mUserDao.insertAll(expected.toArray(new User[4]));
        List<User> received = mRawDao.getUserList("SELECT * FROM User ORDER BY mId ASC");
        assertThat(received, is(expected));
    }

    @Test
    public void entity_liveData() throws TimeoutException, InterruptedException {
        LiveData<User> liveData = mRawDao.getUserLiveData("SELECT * FROM User WHERE mId = 3");
        liveData.observeForever(user -> {
        });
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
        UserAndPet received = mRawDao.getUserAndPet(
                "SELECT * FROM User, Pet WHERE User.mId = Pet.mUserId LIMIT 1");
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
                .getUserAndAllPets("SELECT * FROM User WHERE mId = 3");
        assertThat(result.user, is(user));
        assertThat(result.pets, is(Arrays.asList(pets)));
    }

    @Test
    public void pojo() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        NameAndLastName result =
                mRawDao.getUserNameAndLastName("SELECT * FROM User");
        assertThat(result, is(new NameAndLastName(user.getName(), user.getLastName())));
    }

    @Test
    public void pojo_supportSql() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        NameAndLastName result =
                mRawDao.getUserNameAndLastName(new SimpleSQLiteQuery(
                        "SELECT * FROM User WHERE mId = ?",
                        new Object[] {3}
                ));
        assertThat(result, is(new NameAndLastName(user.getName(), user.getLastName())));
    }

    @Test
    public void pojo_typeConverter() {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        RawDao.UserNameAndBirthday result = mRawDao.getUserAndBirthday(
                "SELECT mName, mBirthday FROM user LIMIT 1");
        assertThat(result.name, is(user.getName()));
        assertThat(result.birthday, is(user.getBirthday()));
    }

    @Test
    public void embedded_nullField() {
        User user = TestUtil.createUser(3);
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 1);
        mUserDao.insert(user);
        mPetDao.insertAll(pets);
        UserAndPet received = mRawDao.getUserAndPet("SELECT * FROM User LIMIT 1");
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
                "SELECT * FROM User LEFT JOIN Pet ON (User.mId = Pet.mUserId)"
                        + " ORDER BY mId ASC, mPetId ASC");
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
        int count = mRawDao.count("SELECT COUNT(*) FROM User");
        assertThat(count, is(4));
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }
}
