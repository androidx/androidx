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

import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.integration.testapp.vo.User;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class WithClauseTest extends TestDatabaseTest{
    @Test
    public void noSourceOfData() {
        @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
        List<Integer> expected = Arrays.asList(1);
        List<Integer> actual = mWithClauseDao.getFactorials(0);
        assertThat(expected, is(actual));

        expected = Arrays.asList(1, 1, 2, 6, 24);
        actual = mWithClauseDao.getFactorials(4);
        assertThat(expected, is(actual));
    }

    @Test
    public void sourceOfData() {
        List<String> expected = new ArrayList<>();
        List<String> actual = mWithClauseDao.getUsersWithFactorialIds(0);
        assertThat(expected, is(actual));

        User user = new User();
        user.setId(0);
        user.setName("Zero");
        mUserDao.insert(user);
        actual = mWithClauseDao.getUsersWithFactorialIds(0);
        assertThat(expected, is(actual));

        user = new User();
        user.setId(1);
        user.setName("One");
        mUserDao.insert(user);
        expected.add("One");
        actual = mWithClauseDao.getUsersWithFactorialIds(0);
        assertThat(expected, is(actual));

        user = new User();
        user.setId(6);
        user.setName("Six");
        mUserDao.insert(user);
        actual = mWithClauseDao.getUsersWithFactorialIds(0);
        assertThat(expected, is(actual));

        actual = mWithClauseDao.getUsersWithFactorialIds(3);
        expected.add("Six");
        assertThat(expected, is(actual));
    }
}
