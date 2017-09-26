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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.arch.paging.BoundedDataSource;
import android.arch.paging.ContiguousDataSource;
import android.arch.paging.KeyedDataSource;
import android.arch.paging.NullPaddedList;
import android.arch.paging.PositionalDataSource;
import android.arch.persistence.room.integration.testapp.vo.User;
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
    public class KeyedUserQueryDataSource extends KeyedDataSource<String, User> {
        @NonNull
        @Override
        public String getKey(@NonNull User item) {
            return item.getName();
        }

        @Override
        public int countItemsBefore(@NonNull String userName) {
            return mUserDao.userNameCountBefore(userName);
        }

        @Override
        public int countItemsAfter(@NonNull String userName) {
            return mUserDao.userNameCountAfter(userName);
        }

        @Nullable
        @Override
        public List<User> loadInitial(int pageSize) {
            return mUserDao.userNameInitial(pageSize);
        }

        @Nullable
        @Override
        public List<User> loadBefore(@NonNull String userName, int pageSize) {
            return mUserDao.userNameLoadBefore(userName, pageSize);
        }

        @Nullable
        @Override
        public List<User> loadAfter(@Nullable String userName, int pageSize) {
            return mUserDao.userNameLoadAfter(userName, pageSize);
        }
    }

    /**
     * Lazy, LIMIT/OFFSET implementation.
     */
    public class OffsetUserQueryDataSource extends BoundedDataSource<User> {
        @Override
        public int countItems() {
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


    public static <Key> void verifyUserDataSource(User[] expected,
            ContiguousDataSource<Key, User> dataSource) {
        List<User> list = new ArrayList<>();

        Object key;
        if (dataSource instanceof PositionalDataSource) {
            // start at 15 by loading 10 items around key 20
            key = 20;
        } else {
            // start at 15 by loading 10 items around key 19 (note, keyed is exclusive, pos isn't)
            KeyedDataSource<String, User> keyedDataSource =
                    (KeyedDataSource<String, User>) dataSource;
            key = keyedDataSource.getKey(expected[19]);
        }
        @SuppressWarnings("unchecked")
        NullPaddedList<User> initial = dataSource.loadInitial((Key) key, 10, true);

        assertNotNull(initial);
        assertEquals(15, initial.getLeadingNullCount());
        assertEquals(expected.length - 25, initial.getTrailingNullCount());
        assertEquals(expected.length, initial.size());

        for (int i = 15; i < initial.size() - initial.getTrailingNullCount(); i++) {
            list.add(initial.get(i));
        }

        assertArrayEquals(Arrays.copyOfRange(expected, 15, 25), list.toArray());
        List<User> p = dataSource.loadAfter(24, list.get(list.size() - 1), 10);
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
