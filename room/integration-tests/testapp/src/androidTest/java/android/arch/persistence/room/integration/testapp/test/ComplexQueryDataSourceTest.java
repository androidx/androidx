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

import android.arch.paging.BoundedDataSource;
import android.arch.paging.KeyedDataSource;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ComplexQueryDataSourceTest extends TestDatabaseTest {

    @SuppressWarnings("WeakerAccess")
    public class LastFirstIdKey {
        public final String lastName;
        public final String name;
        public final int id;

        public LastFirstIdKey(String lastName, String name, int id) {
            this.lastName = lastName;
            this.name = name;
            this.id = id;
        }
    }

    /**
     * Proper, keyed implementation.
     */
    public class KeyedUserQueryDataSource extends KeyedDataSource<LastFirstIdKey, User> {

        @NonNull
        @Override
        public LastFirstIdKey getKey(@NonNull User user) {
            return new LastFirstIdKey(
                    user.getLastName(),
                    user.getName(),
                    user.getId());
        }

        @Override
        public int countItemsBefore(@NonNull LastFirstIdKey key) {
            return mUserDao.userComplexCountBefore(
                    key.lastName,
                    key.name,
                    key.id);
        }

        @Override
        public int countItemsAfter(@NonNull LastFirstIdKey key) {
            return mUserDao.userComplexCountAfter(
                    key.lastName,
                    key.name,
                    key.id);
        }

        @Nullable
        @Override
        public List<User> loadInitial(int pageSize) {
            return mUserDao.userComplexInitial(pageSize);
        }

        @Nullable
        @Override
        public List<User> loadBefore(@NonNull LastFirstIdKey key, int pageSize) {
            return mUserDao.userComplexLoadBefore(
                    key.lastName,
                    key.name,
                    key.id,
                    pageSize);
        }

        @Nullable
        @Override
        public List<User> loadAfter(@Nullable LastFirstIdKey key, int pageSize) {
            return mUserDao.userComplexLoadAfter(
                    key.lastName,
                    key.name,
                    key.id,
                    pageSize);
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
            return mUserDao.userComplexLimitOffset(loadCount, startPosition);
        }
    }

    private static final User[] USERS_BY_LAST_FIRST_ID = new User[100];

    @BeforeClass
    public static void setupClass() {
        String[] lastNames = new String[10];

        String[] firstNames = new String[10];
        for (int i = 0; i < 10; i++) {
            lastNames[i] = "f" + (char) ('a' + i);
            firstNames[i] = "l" + (char) ('a' + i);
        }

        for (int i = 0; i < USERS_BY_LAST_FIRST_ID.length; i++) {
            User user = new User();
            user.setId(i);
            user.setName(firstNames[i % 10]);
            user.setLastName(lastNames[(i / 10) % 10]);
            user.setAge((int) (10 + Math.random() * 50));
            user.setCustomField(UUID.randomUUID().toString());
            user.setBirthday(new Date());
            USERS_BY_LAST_FIRST_ID[i] = user;
        }
    }

    @Before
    public void setup() {
        mUserDao.insertAll(USERS_BY_LAST_FIRST_ID);

        Arrays.sort(USERS_BY_LAST_FIRST_ID, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                int diff = o2.getLastName().compareTo(o1.getLastName());
                if (diff != 0) {
                    return diff;
                }
                diff = o2.getName().compareTo(o1.getName());
                if (diff != 0) {
                    return -diff; // Note: 'mName' is ASC, therefore diff reversed
                }

                return o2.getId() - o1.getId();
            }
        });
    }

    @Test
    public void testKeyedQueryDataSource() {
        QueryDataSourceTest.verifyUserDataSource(USERS_BY_LAST_FIRST_ID,
                new KeyedUserQueryDataSource());
    }

    @Test
    public void testIndexedQueryDataSourceFull() {
        QueryDataSourceTest.verifyUserDataSource(USERS_BY_LAST_FIRST_ID,
                new OffsetUserQueryDataSource());
    }
}
