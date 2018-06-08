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
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.vo.AvgWeightByAge;
import androidx.room.integration.testapp.vo.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PojoTest {
    private UserDao mUserDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        mUserDao = db.getUserDao();
    }

    @Test
    public void weightsByAge() {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 10);
        users[0].setAge(10);
        users[0].setWeight(20);

        users[1].setAge(10);
        users[1].setWeight(30);

        users[2].setAge(15);
        users[2].setWeight(12);

        users[3].setAge(35);
        users[3].setWeight(55);

        mUserDao.insertAll(users);
        assertThat(mUserDao.weightByAge(), is(
                Arrays.asList(
                        new AvgWeightByAge(35, 55),
                        new AvgWeightByAge(10, 25),
                        new AvgWeightByAge(15, 12)
                )
        ));
    }
}
