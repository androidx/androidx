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

import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.util.paging.CountedDataSource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ComplexQueryDataSourceTest extends TestDatabaseTest {
    /**
     * Proper, keyed implementation.
     */
    public class KeyedUserQueryDataSource extends CountedDataSource<User> {
        @Override
        public int loadCount() {
            return mUserDao.getUserCount();
        }

        @Nullable
        @Override
        public List<User> loadAfterInitial(int position, int pageSize) {
            return mUserDao.userComplexLimitOffset(pageSize, position + 1);
        }

        @Nullable
        @Override
        public List<User> loadAfter(int currentEndIndex, @NonNull User currentEndItem,
                int pageSize) {
            return mUserDao.userComplexLoadAfter(
                    currentEndItem.getLastName(),
                    currentEndItem.getName(),
                    currentEndItem.getId(),
                    pageSize);
        }

        @Nullable
        @Override
        public List<User> loadBefore(int currentBeginIndex, @NonNull User currentBeginItem,
                int pageSize) {
            return mUserDao.userComplexLoadBefore(
                    currentBeginItem.getLastName(),
                    currentBeginItem.getName(),
                    currentBeginItem.getId(),
                    pageSize);
        }
    }

    /**
     * Lazy, LIMIT/OFFSET implementation.
     */
    public class OffsetUserQueryDataSource extends CountedDataSource<User> {

        @Override
        public int loadCount() {
            return mUserDao.getUserCount();
        }

        @Nullable
        @Override
        public List<User> loadAfterInitial(int position, int pageSize) {
            return mUserDao.userComplexLimitOffset(pageSize, position + 1);
        }

        @Nullable
        @Override
        public List<User> loadAfter(int currentEndIndex, @NonNull User currentEndItem,
                int pageSize) {
            return mUserDao.userComplexLimitOffset(pageSize, currentEndIndex + 1);
        }

        @Nullable
        @Override
        public List<User> loadBefore(int currentBeginIndex, @NonNull User currentBeginItem,
                int pageSize) {
            int targetOffset = currentBeginIndex - pageSize;
            int offset = Math.max(0, targetOffset);
            int limit = Math.min(pageSize, pageSize + targetOffset);

            List<User> users = mUserDao.userComplexLimitOffset(limit, offset);
            Collections.reverse(users); // :P
            return users;
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
