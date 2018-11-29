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

package androidx.room.integration.noappcompat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BareDatabaseTest {
    private BareDatabase mDb;

    @Before
    public void init() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDb = Room
                .inMemoryDatabaseBuilder(context, BareDatabase.class)
                .build();
    }

    @Test
    public void simpleRead() {
        BareDatabase.BareEntity entity = new BareDatabase.BareEntity(1, "foo");
        mDb.dao().insert(entity);
        BareDatabase.BareEntity received = mDb.dao().get(1);
        assertThat(received, is(entity));
    }

    @Test
    public void observeInvalidation() throws InterruptedException {
        CountDownLatch invalidateLatch = new CountDownLatch(1);
        InvalidationTracker.Observer observer = new InvalidationTracker.Observer("BareEntity") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                invalidateLatch.countDown();
            }
        };
        mDb.getInvalidationTracker().addObserver(observer);
        mDb.dao().insert(new BareDatabase.BareEntity(1, "foo"));
        assertThat(invalidateLatch.await(10, TimeUnit.SECONDS), is(true));
    }
}
