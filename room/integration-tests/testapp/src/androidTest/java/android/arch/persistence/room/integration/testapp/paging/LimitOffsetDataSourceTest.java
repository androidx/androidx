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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.persistence.room.integration.testapp.test.TestDatabaseTest;
import android.arch.persistence.room.integration.testapp.test.TestUtil;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.util.paging.CountedDataSource;
import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LimitOffsetDataSourceTest extends TestDatabaseTest {
    @Test
    public void emptyPage() {
        CountedDataSource<User> dataSource = mUserDao.loadUsersByAgeDesc();
        assertThat(dataSource.loadCount(), is(0));
    }

    @Test
    public void initial() {
        List<User> users = createTestData();
        CountedDataSource<User> dataSource = mUserDao.loadUsersByAgeDesc();
        assertThat(dataSource.loadCount(), is(10));
        List<User> initial = dataSource.loadAfterInitial(-1, 1);
        assertThat(initial.get(0), is(users.get(0)));
        List<User> second = dataSource.loadAfterInitial(0, 1);
        assertThat(second.get(0), is(users.get(1)));
    }

    @Test
    public void loadAll() {
        List<User> users = createTestData();

        CountedDataSource<User> dataSource = mUserDao.loadUsersByAgeDesc();
        List<User> all = dataSource.loadAfterInitial(-1, 10);
        assertThat(users, is(all));
    }

    @Test
    public void loadAfter() {
        List<User> users = createTestData();
        CountedDataSource<User> dataSource = mUserDao.loadUsersByAgeDesc();
        List<User> result = dataSource.loadAfter(3, users.get(3), 2);
        assertThat(result, is(users.subList(4, 6)));
    }

    @Test
    public void loadBefore() {
        List<User> users = createTestData();
        CountedDataSource<User> dataSource = mUserDao.loadUsersByAgeDesc();
        List<User> result = dataSource.loadBefore(5, users.get(5), 3);
        List<User> expected = new ArrayList<>(users.subList(2, 5));
        Collections.reverse(expected);
        assertThat(result, is(expected));
    }

    @NonNull
    private List<User> createTestData() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = TestUtil.createUser(i);
            user.setAge(1);
            mUserDao.insert(user);
            users.add(user);
        }
        return users;
    }
}
