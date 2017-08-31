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

package android.arch.persistence.room.integration.testapp.paging;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.core.executor.testing.CountingTaskExecutorRule;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.persistence.room.integration.testapp.test.TestDatabaseTest;
import android.arch.persistence.room.integration.testapp.test.TestUtil;
import android.arch.persistence.room.integration.testapp.vo.User;
import android.arch.paging.PagedList;
import android.support.annotation.Nullable;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class LivePagedListProviderTest extends TestDatabaseTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    @Test
    @LargeTest
    public void getUsersAsPagedList()
            throws InterruptedException, ExecutionException, TimeoutException {
        mDatabase.beginTransaction();
        try {
            for (int i = 0; i < 100; i++) {
                final User user = TestUtil.createUser(i + 1);
                user.setAge(i);
                mUserDao.insert(user);
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
        assertThat(mUserDao.count(), is(100));
        final LiveData<PagedList<User>> livePagedUsers = mUserDao.loadPagedByAge(3).create(
                0,
                new PagedList.Config.Builder()
                        .setPageSize(10)
                        .setPrefetchDistance(1)
                        .setInitialLoadSizeHint(10).build());

        final TestLifecycleOwner testOwner = new TestLifecycleOwner();
        testOwner.handleEvent(Lifecycle.Event.ON_CREATE);
        drain();
        PagedListObserver<User> observer = new PagedListObserver<>();

        observe(livePagedUsers, testOwner, observer);
        assertThat(observer.get(), nullValue());
        observer.reset();

        testOwner.handleEvent(Lifecycle.Event.ON_START);
        drain();

        PagedList<User> pagedList1 = observer.get();
        assertThat(pagedList1, is(notNullValue()));

        assertThat(pagedList1.size(), is(96));
        assertThat(getAndLoad(pagedList1, 20), is(nullValue()));
        drain();
        assertThat(getAndLoad(pagedList1, 31), nullValue());
        assertThat(getAndLoad(pagedList1, 20), notNullValue());
        assertThat(getAndLoad(pagedList1, 16), notNullValue());

        drain();
        assertThat(getAndLoad(pagedList1, 31), notNullValue());
        assertThat(getAndLoad(pagedList1, 50), nullValue());
        drain();
        assertThat(getAndLoad(pagedList1, 50), notNullValue());
        observer.reset();
        // now invalidate the database but don't get the new paged list
        mUserDao.updateById(50, "foo");
        assertThat(getAndLoad(pagedList1, 70), nullValue());
        drain();
        assertThat(getAndLoad(pagedList1, 70), nullValue());
        PagedList<User> pagedList = observer.get();
        assertThat(getAndLoad(pagedList, 70), notNullValue());
    }

    private <T> T getAndLoad(PagedList<T> list, int pos) {
        T result = list.get(pos);
        list.loadAround(pos);
        return result;
    }

    private void drain() throws InterruptedException, TimeoutException {
        mExecutorRule.drainTasks(60, TimeUnit.SECONDS);
    }

    private void observe(final LiveData liveData, final LifecycleOwner provider,
            final Observer observer) throws ExecutionException, InterruptedException {
        FutureTask<Void> futureTask = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //noinspection unchecked
                liveData.observe(provider, observer);
                return null;
            }
        });
        AppToolkitTaskExecutor.getInstance().executeOnMainThread(futureTask);
        futureTask.get();
    }

    static class TestLifecycleOwner implements LifecycleOwner {

        private LifecycleRegistry mLifecycle;

        TestLifecycleOwner() {
            mLifecycle = new LifecycleRegistry(this);
        }

        @Override
        public Lifecycle getLifecycle() {
            return mLifecycle;
        }

        void handleEvent(Lifecycle.Event event) {
            mLifecycle.handleLifecycleEvent(event);
        }
    }

    private static class PagedListObserver<T> implements Observer<PagedList<T>> {
        private PagedList<T> mList;
        public void reset() {
            mList = null;
        }

        public PagedList<T> get() {
            return mList;
        }

        @Override
        public void onChanged(@Nullable PagedList<T> pagedList) {
            mList = pagedList;
        }
    }
}
