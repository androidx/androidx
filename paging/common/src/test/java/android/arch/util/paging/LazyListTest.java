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

package android.arch.util.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(JUnit4.class)
public class LazyListTest {
    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mBackgroundThread = new TestExecutor();
    private TestCountedDataSource mDataSource;

    private LazyList.ChangeCallback mChangeCallback = Mockito.mock(LazyList.ChangeCallback.class);

    private LazyList<User> createLazyList(int pageSize, int prefetchCount) {
        LazyList<User> lazyList = new LazyList<>(mDataSource, mMainThread, mBackgroundThread,
                 new ListConfig(pageSize, prefetchCount));
        lazyList.addCallback(mChangeCallback);
        return lazyList;
    }

    @Before
    public void setup() {
        mDataSource = new TestCountedDataSource();
    }

    @Test
    public void initial() {
        LazyList<User> lazyList = createLazyList(20, 5);
        assertNull(lazyList.get(0));
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 20);

        lazyList = createLazyList(10, 5);
        assertNull(lazyList.get(0));
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 10);
    }

    @Test
    public void initialReturnsEmpty() {
        mDataSource.setCount(200);
        LazyList<User> lazyList = createLazyList(20, 5);
        assertEquals(200, lazyList.size());


        // trigger initial load that returns nothing
        mDataSource.setCount(0);
        lazyList.get(0);
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 0);

        // now further accesses don't trigger loads
        mDataSource.setCount(200);
        lazyList.get(100);
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 0);
    }

    @Test
    public void initialPrefetch() {
        LazyList<User> lazyList = createLazyList(20, 20);
        assertNull(lazyList.get(9));
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 40);
    }

    @Test
    public void initialPrefetchMultiple() {
        LazyList<User> lazyList = createLazyList(20, 90);
        lazyList.get(0);
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 100);
    }

    @Test
    public void incrementalLoading() {
        LazyList<User> lazyList = createLazyList(20, 20);
        lazyList.get(0);
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 40);

        lazyList.get(39);
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 60);

        lazyList.get(41);
        drain();
        TestCountedDataSource.verifyRange(lazyList, 0, 80);
    }

    private static <T> List<T> reverse(List<T> input) {
        ArrayList<T> result = new ArrayList<>(input);
        Collections.reverse(result);
        return result;
    }

    private void drain() {
        boolean executed;
        do {
            executed = mBackgroundThread.executeAll();
            executed |= mMainThread.executeAll();
        } while (executed);

    }
}
