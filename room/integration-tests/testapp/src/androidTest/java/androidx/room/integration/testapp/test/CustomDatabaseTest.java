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

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.database.Customer;
import androidx.room.integration.testapp.database.SampleDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CustomDatabaseTest {

    @Test
    public void invalidationTrackerAfterClose() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RoomDatabase.Builder<SampleDatabase> builder =
                Room.databaseBuilder(context, SampleDatabase.class, "db")
                        .openHelperFactory(new RethrowExceptionFactory());
        Customer customer = new Customer();
        for (int i = 0; i < 100; i++) {
            SampleDatabase db = builder.build();
            db.getCustomerDao().insert(customer);
            // Give InvalidationTracker enough time to start #mRefreshRunnable and pass the
            // initialization check.
            SystemClock.sleep(1);
            // InvalidationTracker#mRefreshRunnable will cause race condition if its database query
            // happens after close.
            db.close();
        }
    }

    /**
     * This is mostly {@link FrameworkSQLiteOpenHelperFactory}, but the returned {@link
     * SupportSQLiteDatabase} fails with {@link RuntimeException} instead of {@link
     * IllegalStateException} or {@link SQLiteException}. This way, we can simulate custom database
     * implementation that throws its own exception types.
     */
    private static class RethrowExceptionFactory implements SupportSQLiteOpenHelper.Factory {

        @Override
        public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration configuration) {
            final FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory();
            final SupportSQLiteOpenHelper helper = factory.create(configuration);
            SupportSQLiteOpenHelper helperMock = mock(SupportSQLiteOpenHelper.class,
                    delegatesTo(helper));
            // Inject mocks to the object hierarchy.
            doAnswer(new Answer() {
                @Override
                public SupportSQLiteDatabase answer(InvocationOnMock invocation)
                        throws Throwable {
                    final SupportSQLiteDatabase db = helper.getWritableDatabase();
                    SupportSQLiteDatabase dbMock = mock(SupportSQLiteDatabase.class,
                            delegatesTo(db));
                    doAnswer(new Answer() {
                        @Override
                        public Cursor answer(InvocationOnMock invocation) throws Throwable {
                            SupportSQLiteQuery query = invocation.getArgument(0);
                            try {
                                return db.query(query);
                            } catch (IllegalStateException | SQLiteException e) {
                                // Rethrow the exception in order to simulate the way custom
                                // database implementation throws its own exception types.
                                throw new RuntimeException("closed", e);
                            }
                        }
                    }).when(dbMock).query(any(SupportSQLiteQuery.class));
                    return dbMock;
                }
            }).when(helperMock).getWritableDatabase();
            return helperMock;
        }
    }
}
