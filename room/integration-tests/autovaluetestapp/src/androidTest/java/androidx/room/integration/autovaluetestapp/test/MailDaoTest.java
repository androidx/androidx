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

package androidx.room.integration.autovaluetestapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Build;

import androidx.room.Room;
import androidx.room.integration.autovaluetestapp.FtsTestDatabase;
import androidx.room.integration.autovaluetestapp.vo.Mail;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;


@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
public class MailDaoTest {

    private FtsTestDatabase mDatabase;
    private FtsTestDatabase.MailDao mMailDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mDatabase = Room.inMemoryDatabaseBuilder(context, FtsTestDatabase.class).build();
        mMailDao = mDatabase.getMailDao();
    }

    @Test
    public void readWrite() {
        Mail item = Mail.create(
                "Hello old friend",
                "How are you? Wanna grab coffee?");

        mMailDao.insert(item);

        List<Mail> loaded = mMailDao.getMail("coffee");
        assertThat(loaded.get(0), is(item));
    }
}
