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

package android.arch.persistence.room.integration.testapp.paging;

import static android.test.MoreAsserts.assertEmpty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.persistence.room.integration.testapp.test.TestDatabaseTest;
import android.arch.persistence.room.integration.testapp.test.TestUtil;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.persistence.room.paging.LimitOffsetDataSource;
import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LimitOffsetDataSourceTest extends TestDatabaseTest {

    @After
    public void teardown() {
        mUserDao.deleteEverything();
    }

    private LimitOffsetDataSource<User> loadUsersByAgeDesc() {
        return (LimitOffsetDataSource<User>) mUserDao.loadUsersByAgeDesc();
    }

    @Test
    public void emptyPage() {
        LimitOffsetDataSource<User> dataSource = loadUsersByAgeDesc();
        assertThat(dataSource.countItems(), is(0));
    }

    @Test
    public void loadCount() {
        createUsers(6);
        LimitOffsetDataSource<User> dataSource = loadUsersByAgeDesc();
        assertThat(dataSource.countItems(), is(6));
    }

    @Test
    public void singleItem() {
        List<User> users = createUsers(1);
        LimitOffsetDataSource<User> dataSource = loadUsersByAgeDesc();
        assertThat(dataSource.countItems(), is(1));
        List<User> initial = dataSource.loadRange(0, 10);

        assertThat(initial.get(0), is(users.get(0)));
        assertEmpty(dataSource.loadRange(1, 10));
    }

    @Test
    public void initial() {
        List<User> users = createUsers(10);
        LimitOffsetDataSource<User> dataSource = loadUsersByAgeDesc();
        assertThat(dataSource.countItems(), is(10));
        List<User> initial = dataSource.loadRange(0, 1);
        assertThat(initial.get(0), is(users.get(0)));
        List<User> second = dataSource.loadRange(1, 1);
        assertThat(second.get(0), is(users.get(1)));
    }

    @Test
    public void loadAll() {
        List<User> users = createUsers(10);

        LimitOffsetDataSource<User> dataSource = loadUsersByAgeDesc();
        List<User> all = dataSource.loadRange(0, 10);
        assertThat(users, is(all));
    }

    @Test
    public void loadAfter() {
        List<User> users = createUsers(10);
        LimitOffsetDataSource<User> dataSource = loadUsersByAgeDesc();
        List<User> result = dataSource.loadRange(4, 2);
        assertThat(result, is(users.subList(4, 6)));
    }

    @NonNull
    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = TestUtil.createUser(i);
            user.setAge(1);
            mUserDao.insert(user);
            users.add(user);
        }
        return users;
    }
}
