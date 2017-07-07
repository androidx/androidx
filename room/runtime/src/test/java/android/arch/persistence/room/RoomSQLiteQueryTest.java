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

package android.arch.persistence.room;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.arch.persistence.db.SupportSQLiteProgram;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RunWith(JUnit4.class)
public class RoomSQLiteQueryTest {
    @Before
    public void clear() {
        RoomSQLiteQuery.sQueryPool.clear();
    }

    @Test
    public void acquireBasic() {
        RoomSQLiteQuery query = RoomSQLiteQuery.acquire("abc", 3);
        assertThat(query.getSql(), is("abc"));
        assertThat(query.mArgCount, is(3));
        assertThat(query.mBlobBindings.length, is(4));
        assertThat(query.mLongBindings.length, is(4));
        assertThat(query.mStringBindings.length, is(4));
        assertThat(query.mDoubleBindings.length, is(4));
    }

    @Test
    public void acquireSameSizeAgain() {
        RoomSQLiteQuery query = RoomSQLiteQuery.acquire("abc", 3);
        query.release();
        assertThat(RoomSQLiteQuery.acquire("blah", 3), sameInstance(query));
    }

    @Test
    public void acquireSameSizeWithoutRelease() {
        RoomSQLiteQuery query = RoomSQLiteQuery.acquire("abc", 3);
        assertThat(RoomSQLiteQuery.acquire("fda", 3), not(sameInstance(query)));
    }

    @Test
    public void bindings() {
        RoomSQLiteQuery query = RoomSQLiteQuery.acquire("abc", 6);
        byte[] myBlob = new byte[3];
        long myLong = 3L;
        double myDouble = 7.0;
        String myString = "ss";
        query.bindBlob(1, myBlob);
        query.bindLong(2, myLong);
        query.bindNull(3);
        query.bindDouble(4, myDouble);
        query.bindString(5, myString);
        query.bindNull(6);
        SupportSQLiteProgram program = mock(SupportSQLiteProgram.class);
        query.bindTo(program);

        verify(program).bindBlob(1, myBlob);
        verify(program).bindLong(2, myLong);
        verify(program).bindNull(3);
        verify(program).bindDouble(4, myDouble);
        verify(program).bindString(5, myString);
        verify(program).bindNull(6);
    }

    @Test
    public void dontKeepSameSizeTwice() {
        RoomSQLiteQuery query1 = RoomSQLiteQuery.acquire("abc", 3);
        RoomSQLiteQuery query2 = RoomSQLiteQuery.acquire("zx", 3);
        RoomSQLiteQuery query3 = RoomSQLiteQuery.acquire("qw", 0);

        query1.release();
        query2.release();
        assertThat(RoomSQLiteQuery.sQueryPool.size(), is(1));

        query3.release();
        assertThat(RoomSQLiteQuery.sQueryPool.size(), is(2));
    }

    @Test
    public void returnExistingForSmallerSize() {
        RoomSQLiteQuery query = RoomSQLiteQuery.acquire("abc", 3);
        query.release();
        assertThat(RoomSQLiteQuery.acquire("dsa", 2), sameInstance(query));
    }

    @Test
    public void returnNewForBigger() {
        RoomSQLiteQuery query = RoomSQLiteQuery.acquire("abc", 3);
        query.release();
        assertThat(RoomSQLiteQuery.acquire("dsa", 4), not(sameInstance(query)));
    }

    @Test
    public void pruneCache() {
        for (int i = 0; i < RoomSQLiteQuery.POOL_LIMIT; i++) {
            RoomSQLiteQuery.acquire("dsdsa", i).release();
        }
        pruneCacheTest();
    }

    @Test
    public void pruneCacheReverseInsertion() {
        List<RoomSQLiteQuery> queries = new ArrayList<>();
        for (int i = RoomSQLiteQuery.POOL_LIMIT - 1; i >= 0; i--) {
            queries.add(RoomSQLiteQuery.acquire("dsdsa", i));
        }
        for (RoomSQLiteQuery query : queries) {
            query.release();
        }
        pruneCacheTest();
    }

    private void pruneCacheTest() {
        assertThat(RoomSQLiteQuery.sQueryPool.size(), is(RoomSQLiteQuery.POOL_LIMIT));
        RoomSQLiteQuery.acquire("dsadsa", RoomSQLiteQuery.POOL_LIMIT + 1).release();
        assertThat(RoomSQLiteQuery.sQueryPool.size(), is(RoomSQLiteQuery.DESIRED_POOL_SIZE));
        Iterator<RoomSQLiteQuery> itr = RoomSQLiteQuery.sQueryPool.values().iterator();
        for (int i = 0; i < RoomSQLiteQuery.DESIRED_POOL_SIZE; i++) {
            assertThat(itr.next().mCapacity, is(i));
        }
    }
}
