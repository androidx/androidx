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

import static junit.framework.TestCase.fail;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class PagedListTest {
    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mBackgroundThread = new TestExecutor();
    private TestDataSource mDataSource = new TestDataSource();

    private PagedList.ChangeCallback mChangeCallback = Mockito.mock(PagedList.ChangeCallback.class);

    @Test
    public void callbackSimple() {
        PagedList.ChangeCallback callback = Mockito.mock(PagedList.ChangeCallback.class);
        PagedList.ChangeCallbackWrapper wrapper =
                new PagedList.ChangeCallbackWrapper(callback, 0, 0);

        wrapper.dispatchLoaded(0, 10);
        verify(callback).onInserted(0, 10);
        verifyNoMoreInteractions(callback);

        wrapper.dispatchLoaded(0, 10);
        verifyNoMoreInteractions(callback);

        wrapper.dispatchLoaded(0, 20);
        verify(callback).onInserted(10, 10);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void callbackComplex() {
        PagedList.ChangeCallback callback = Mockito.mock(PagedList.ChangeCallback.class);
        PagedList.ChangeCallbackWrapper wrapper =
                new PagedList.ChangeCallbackWrapper(callback, 0, 0);

        wrapper.dispatchLoaded(0, 8);
        verify(callback).onInserted(0, 8);
        verifyNoMoreInteractions(callback);

        wrapper.dispatchLoaded(10, 18);
        verify(callback).onInserted(0, 10);
        verifyNoMoreInteractions(callback);

        wrapper.dispatchLoaded(10, 22);
        verify(callback).onInserted(18, 4);
        verifyNoMoreInteractions(callback);

        // Note: prepend before append is impl detail
        wrapper.dispatchLoaded(12, 25);
        verify(callback).onInserted(0, 2);
        verify(callback).onInserted(24, 1);
        verifyNoMoreInteractions(callback);
    }

    private PagedList<User> createPagedList(int pageSize, int prefetchDistance) {
        PagedList<User> pagedList = new PagedList<>(
                mDataSource, mMainThread, mBackgroundThread,
                new ListConfig(pageSize, prefetchDistance));
        pagedList.addCallback(mChangeCallback);
        return pagedList;
    }

    private PagedList<User> createPagedListInitializedFrom(PagedList<User> old) {
        PagedList<User> pagedList = new PagedList<>(
                mDataSource, mMainThread, mBackgroundThread,
                old.mConfig);
        if (!pagedList.initializeFrom(old)) {
            fail();
        }
        return pagedList;
    }

    @Test(expected = IllegalArgumentException.class)
    public void requirePrefetch() {
        createPagedList(20, 0);
    }

    @Test
    public void initial() {
        PagedList<User> pagedList = createPagedList(20, 5);
        pagedList.triggerInitialLoad(null);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 20);

        pagedList = createPagedList(10, 5);
        pagedList.triggerInitialLoad(null);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 10);
    }

    @Test
    public void initialPrefetch() {
        PagedList<User> pagedList = createPagedList(20, 20);
        pagedList.triggerInitialLoad(null);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 20);

        // Note: pagedList doesn't prefetch after initial load until first get call
        pagedList.get(10);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 40);
    }

    @Test
    public void initialPrefetchMultiple() {
        PagedList<User> pagedList = createPagedList(20, 90);
        pagedList.triggerInitialLoad(null);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 20);

        // Note: pagedList doesn't prefetch after initial load until first get call
        pagedList.get(0);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 100);
    }

    @Test
    public void incrementalLoading() {
        PagedList<User> pagedList = createPagedList(20, 20);
        pagedList.triggerInitialLoad(null);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 20);

        pagedList.get(5);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 40);

        pagedList.get(39);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 60);

        pagedList.get(41);
        drain();
        TestDataSource.verifyRange(pagedList, 0, 80);
    }

    @Test(expected = IllegalArgumentException.class)
    public void get_tooEarly() {
        PagedList<User> pagedList = createPagedList(20, 20);
        pagedList.triggerInitialLoad(null);
        assertEquals(0, pagedList.size());
        pagedList.get(0); // task hasn't run, zero size currently
    }

    @Test
    public void initializeFrom() {
        PagedList<User> pagedList = createPagedList(20, 20);
        pagedList.triggerInitialLoad(null);
        drain();
    }

    private void drain() {
        boolean executed;
        do {
            executed = mBackgroundThread.executeAll();
            executed |= mMainThread.executeAll();
        } while (executed);

    }
}
