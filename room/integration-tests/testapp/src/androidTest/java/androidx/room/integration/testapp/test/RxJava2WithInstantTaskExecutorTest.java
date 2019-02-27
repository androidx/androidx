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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.vo.User;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.subscribers.TestSubscriber;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RxJava2WithInstantTaskExecutorTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestDatabase mDatabase;

    @Before
    public void initDb() throws Exception {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        mDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
                TestDatabase.class)
                // allowing main thread queries, just for testing
                .allowMainThreadQueries()
                .build();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFlowableInTransaction() {
        // When subscribing to the emissions of the user
        TestSubscriber<User> subscriber = mDatabase.getUserDao().flowableUserById(3).test();
        subscriber.assertValueCount(0);

        // When inserting a new user in the data source
        mDatabase.beginTransaction();
        try {
            mDatabase.getUserDao().insert(TestUtil.createUser(3));
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }

        subscriber.assertValueCount(1);
    }
}
