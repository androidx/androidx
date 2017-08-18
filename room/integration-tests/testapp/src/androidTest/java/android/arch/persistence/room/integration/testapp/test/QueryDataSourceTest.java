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

package android.arch.persistence.room.integration.testapp.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.util.paging.BoundedDataSource;
import android.arch.util.paging.ContiguousDataSource;
import android.arch.util.paging.KeyedDataSource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class QueryDataSourceTest extends TestDatabaseTest {
    /**
     * Proper, keyed implementation.
     */
    public class KeyedUserQueryDataSource extends KeyedDataSource<User> {
        @Override
        public int loadCount() {
            return mUserDao.getUserCount();
        }

        @Nullable
        @Override
        public List<User> loadAfterInitial(int position, int pageSize) {
            return mUserDao.userNameLimitOffset(pageSize, position);
        }

        @Nullable
        @Override
        public List<User> loadAfter(@NonNull User currentEndItem, int pageSize) {
            return mUserDao.userNameLoadAfter(currentEndItem.getName(), pageSize);
        }

        @Nullable
        @Override
        public List<User> loadBefore(@NonNull User currentBeginItem, int pageSize) {
            return mUserDao.userNameLoadBefore(currentBeginItem.getName(), pageSize);
        }
    }

    /**
     * Lazy, LIMIT/OFFSET implementation.
     */
    public class OffsetUserQueryDataSource extends BoundedDataSource<User> {
        @Override
        public int loadCount() {
            return mUserDao.getUserCount();
        }

        @Nullable
        @Override
        public List<User> loadRange(int startPosition, int loadCount) {
            return mUserDao.userNameLimitOffset(loadCount, startPosition);
        }
    }

    private static final User[] USERS_BY_NAME = new User[50];

    @BeforeClass
    public static void setupClass() {
        for (int i = 0; i < USERS_BY_NAME.length; i++) {
            USERS_BY_NAME[i] = TestUtil.createUser(i);
        }
    }

    @Before
    public void setup() {
        mUserDao.insertAll(USERS_BY_NAME);

        Arrays.sort(USERS_BY_NAME, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return o2.getName().compareTo(o1.getName());
            }
        });
    }

    @Test
    public void testKeyedQueryDataSource() {
        verifyUserDataSource(USERS_BY_NAME, new KeyedUserQueryDataSource());
    }

    @Test
    public void testIndexedQueryDataSourceFull() {
        verifyUserDataSource(USERS_BY_NAME, new OffsetUserQueryDataSource());
    }


    public static void verifyUserDataSource(
            User[] expected, ContiguousDataSource<User> dataSource) {
        List<User> list = new ArrayList<>();
        List<User> p = dataSource.loadAfterInitial(15, 10);
        assertNotNull(p);
        list.addAll(p);

        assertArrayEquals(Arrays.copyOfRange(expected, 15, 25), list.toArray());
        p = dataSource.loadAfter(24, list.get(list.size() - 1), 10);
        assertNotNull(p);
        list.addAll(p);

        assertArrayEquals(Arrays.copyOfRange(expected, 15, 35), list.toArray());

        p = dataSource.loadBefore(15, list.get(0), 10);
        assertNotNull(p);
        for (User u : p) {
            list.add(0, u);
        }

        assertArrayEquals(Arrays.copyOfRange(expected, 5, 35), list.toArray());

        p = dataSource.loadBefore(5, list.get(0), 10);
        assertNotNull(p);
        for (User u : p) {
            list.add(0, u);
        }

        assertArrayEquals(Arrays.copyOfRange(expected, 0, 35), list.toArray());
    }
}
