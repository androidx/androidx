/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.room;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.sqlite.db.SupportSQLiteStatement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@RunWith(JUnit4.class)
public class SharedSQLiteStatementTest {
    private SharedSQLiteStatement mSharedStmt;
    RoomDatabase mDb;
    @Before
    public void init() {
        mDb = mock(RoomDatabase.class);
        when(mDb.compileStatement(anyString())).thenAnswer(new Answer<SupportSQLiteStatement>() {

            @Override
            public SupportSQLiteStatement answer(InvocationOnMock invocation) throws Throwable {
                return mock(SupportSQLiteStatement.class);
            }
        });
        when(mDb.getInvalidationTracker()).thenReturn(mock(InvalidationTracker.class));
        mSharedStmt = new SharedSQLiteStatement(mDb) {
            @Override
            protected String createQuery() {
                return "foo";
            }
        };
    }

    @Test
    public void checkMainThread() {
        mSharedStmt.acquire();
        verify(mDb).assertNotMainThread();
    }

    @Test
    public void basic() {
        assertThat(mSharedStmt.acquire(), notNullValue());
    }

    @Test
    public void getTwiceWithoutReleasing() {
        SupportSQLiteStatement stmt1 = mSharedStmt.acquire();
        SupportSQLiteStatement stmt2 = mSharedStmt.acquire();
        assertThat(stmt1, notNullValue());
        assertThat(stmt2, notNullValue());
        assertThat(stmt1, is(not(stmt2)));
    }

    @Test
    public void getTwiceWithReleasing() {
        SupportSQLiteStatement stmt1 = mSharedStmt.acquire();
        mSharedStmt.release(stmt1);
        SupportSQLiteStatement stmt2 = mSharedStmt.acquire();
        assertThat(stmt1, notNullValue());
        assertThat(stmt1, is(stmt2));
    }

    @Test
    public void getFromAnotherThreadWhileHolding() throws ExecutionException, InterruptedException {
        SupportSQLiteStatement stmt1 = mSharedStmt.acquire();
        FutureTask<SupportSQLiteStatement> task = new FutureTask<>(
                new Callable<SupportSQLiteStatement>() {
                    @Override
                    public SupportSQLiteStatement call() throws Exception {
                        return mSharedStmt.acquire();
                    }
                });
        new Thread(task).run();
        SupportSQLiteStatement stmt2 = task.get();
        assertThat(stmt1, notNullValue());
        assertThat(stmt2, notNullValue());
        assertThat(stmt1, is(not(stmt2)));
    }

    @Test
    public void getFromAnotherThreadAfterReleasing() throws ExecutionException,
            InterruptedException {
        SupportSQLiteStatement stmt1 = mSharedStmt.acquire();
        mSharedStmt.release(stmt1);
        FutureTask<SupportSQLiteStatement> task = new FutureTask<>(
                new Callable<SupportSQLiteStatement>() {
                    @Override
                    public SupportSQLiteStatement call() throws Exception {
                        return mSharedStmt.acquire();
                    }
                });
        new Thread(task).run();
        SupportSQLiteStatement stmt2 = task.get();
        assertThat(stmt1, notNullValue());
        assertThat(stmt1, is(stmt2));
    }
}
