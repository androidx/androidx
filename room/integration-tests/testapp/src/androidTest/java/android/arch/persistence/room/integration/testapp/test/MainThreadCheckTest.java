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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.core.util.Function;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.integration.testapp.TestDatabase;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MainThreadCheckTest {

    @Test
    public void testMainThread() {
        final Throwable error = test(false, new Function<TestDatabase, Void>() {
            @Override
            public Void apply(TestDatabase db) {
                db.getUserDao().load(3);
                return null;
            }
        });
        assertThat(error, notNullValue());
        assertThat(error, instanceOf(IllegalStateException.class));
    }

    @Test
    public void testFlowableOnMainThread() {
        final Throwable error = test(false, new Function<TestDatabase, Void>() {
            @Override
            public Void apply(TestDatabase db) {
                db.getUserDao().flowableUserById(3);
                return null;
            }
        });
        assertThat(error, nullValue());
    }

    @Test
    public void testLiveDataOnMainThread() {
        final Throwable error = test(false, new Function<TestDatabase, Void>() {
            @Override
            public Void apply(TestDatabase db) {
                db.getUserDao().liveUserById(3);
                return null;
            }
        });
        assertThat(error, nullValue());
    }

    @Test
    public void testAllowMainThread() {
        final Throwable error = test(true, new Function<TestDatabase, Void>() {
            @Override
            public Void apply(TestDatabase db) {
                db.getUserDao().load(3);
                return null;
            }
        });
        assertThat(error, nullValue());
    }

    private Throwable test(boolean allowMainThread, final Function<TestDatabase, Void> fun) {
        Context context = InstrumentationRegistry.getTargetContext();
        final RoomDatabase.Builder<TestDatabase> builder = Room.inMemoryDatabaseBuilder(
                context, TestDatabase.class);
        if (allowMainThread) {
            builder.allowMainThreadQueries();
        }
        final TestDatabase db = builder.build();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    try {
                        fun.apply(db);
                    } catch (Throwable t) {
                        error.set(t);
                    }
                }
            });
        } finally {
            db.close();
        }
        return error.get();
    }
}
