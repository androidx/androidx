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

package androidx.room.integration.testapp.paging;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.room.integration.testapp.test.TestDatabaseTest;
import androidx.room.integration.testapp.test.TestLifecycleOwner;
import androidx.room.integration.testapp.test.TestUtil;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DataSourceFactoryTest extends TestDatabaseTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    private interface LivePagedListFactory {
        LiveData<PagedList<User>> create();
    }

    @Test
    public void getUsersAsPagedList()
            throws InterruptedException, ExecutionException, TimeoutException {
        validateUsersAsPagedList(() -> new LivePagedListBuilder<>(
                mUserDao.loadPagedByAge(3),
                new PagedList.Config.Builder()
                        .setPageSize(10)
                        .setPrefetchDistance(1)
                        .setInitialLoadSizeHint(10).build())
                .build());
    }

    @Test
    public void getUsersAsPagedList_ViaRawQuery_WithObservable()
            throws InterruptedException, ExecutionException, TimeoutException {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(
                "SELECT * FROM user where mAge > ?",
                new Object[]{3});
        validateUsersAsPagedList(() -> new LivePagedListBuilder<>(
                mUserDao.loadPagedByAgeWithObserver(query),
                new PagedList.Config.Builder()
                        .setPageSize(10)
                        .setPrefetchDistance(1)
                        .setInitialLoadSizeHint(10).build())
                .build());
    }

    private void validateUsersAsPagedList(
            LivePagedListFactory factory)
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

        final LiveData<PagedList<User>> livePagedUsers = factory.create();

        final TestLifecycleOwner testOwner = new TestLifecycleOwner();
        testOwner.handleEvent(Lifecycle.Event.ON_CREATE);
        drain();
        PagedListObserver<User> observer = new PagedListObserver<>();

        observe(livePagedUsers, testOwner, observer);
        assertThat(observer.get(), nullValue());
        observer.reset();

        testOwner.handleEvent(Lifecycle.Event.ON_START);
        drain();

        final PagedList<User> pagedList1 = observer.get();
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
        drain(); // Sync with InvalidationTracker
        assertThat(getAndLoad(pagedList1, 70), nullValue());
        drain();
        assertThat(getAndLoad(pagedList1, 70), nullValue());
        final PagedList<User> pagedList = observer.get();
        assertThat(getAndLoad(pagedList, 50), notNullValue());
        assertThat(getAndLoad(pagedList, 70), nullValue());
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
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            //noinspection unchecked
            liveData.observe(provider, observer);
            return null;
        });
        ArchTaskExecutor.getInstance().executeOnMainThread(futureTask);
        futureTask.get();
    }

    @Test
    public void withRelation() throws ExecutionException, InterruptedException, TimeoutException {
        // verify DataSourceFactory can be created from a multi table join
        DataSource.Factory<Integer, UserAndAllPets> factory =
                mUserPetDao.dataSourceFactoryMultiTable();
        LiveData<PagedList<UserAndAllPets>> liveData =
                new LivePagedListBuilder<>(mUserPetDao.dataSourceFactoryMultiTable(), 10).build();
        assertNotNull(factory.create());

        PagedListObserver<UserAndAllPets> observer = new PagedListObserver<>();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        observe(liveData, lifecycleOwner, observer);
        drain();
        assertThat(observer.get(), is(Collections.emptyList()));

        observer.reset();
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        final UserAndAllPets noPets = observer.get().get(0);
        assertThat(noPets.user, is(user));

        observer.reset();
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mPetDao.insertAll(pets);

        drain();
        final UserAndAllPets withPets = observer.get().get(0);
        assertThat(withPets.user, is(user));
        assertThat(withPets.pets, is(Arrays.asList(pets)));
    }

    private static class PagedListObserver<T> implements Observer<PagedList<T>> {
        private PagedList<T> mList;

        void reset() {
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
