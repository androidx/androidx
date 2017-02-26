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

package com.android.support.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.annotation.Nullable;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.executors.AppToolkitTaskExecutor;
import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.LifecycleRegistry;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.Observer;
import com.android.support.room.integration.testapp.vo.AvgWeightByAge;
import com.android.support.room.integration.testapp.vo.Pet;
import com.android.support.room.integration.testapp.vo.User;
import com.android.support.room.integration.testapp.vo.UserAndAllPets;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Tests invalidation tracking.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LiveDataQueryTest extends TestDatabaseTest {
    @Test
    public void observeById() throws InterruptedException, ExecutionException {
        final LiveData<User> userLiveData = mUserDao.liveUserById(5);
        final TestLifecycleProvider testProvider = new TestLifecycleProvider();
        testProvider.handleEvent(Lifecycle.ON_CREATE);
        final LatchObserver<User> observer = new LatchObserver<>();
        observe(userLiveData, testProvider, observer);

        observer.assertNoUpdate();

        observer.reset();
        testProvider.handleEvent(Lifecycle.ON_START);
        assertThat(observer.get(), is(nullValue()));

        // another id
        observer.reset();
        mUserDao.insert(TestUtil.createUser(7));
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        final User u5 = TestUtil.createUser(5);
        mUserDao.insert(u5);
        assertThat(observer.get(), is(notNullValue()));

        u5.setName("foo-foo-foo");
        observer.reset();
        mUserDao.insertOrReplace(u5);
        final User updated = observer.get();
        assertThat(updated, is(notNullValue()));
        assertThat(updated.getName(), is("foo-foo-foo"));

        testProvider.handleEvent(Lifecycle.ON_STOP);
        observer.reset();
        u5.setName("baba");
        mUserDao.insertOrReplace(u5);
        observer.assertNoUpdate();
    }

    @Test
    public void observeListQuery() throws InterruptedException, ExecutionException {
        final LiveData<List<User>> userLiveData = mUserDao.liveUsersListByName("frida");
        final TestLifecycleProvider lifecycleProvider = new TestLifecycleProvider();
        lifecycleProvider.handleEvent(Lifecycle.ON_START);
        final LatchObserver<List<User>> observer = new LatchObserver<>();
        observe(userLiveData, lifecycleProvider, observer);
        assertThat(observer.get(), is(Collections.<User>emptyList()));

        observer.reset();
        final User user1 = TestUtil.createUser(3);
        user1.setName("dog frida");
        mUserDao.insert(user1);
        assertThat(observer.get(), is(Collections.singletonList(user1)));


        observer.reset();
        final User user2 = TestUtil.createUser(5);
        user2.setName("does not match");
        mUserDao.insert(user2);
        assertThat(observer.get(), is(Collections.singletonList(user1)));

        observer.reset();
        user1.setName("i don't match either");
        mUserDao.insertOrReplace(user1);
        assertThat(observer.get(), is(Collections.<User>emptyList()));

        lifecycleProvider.handleEvent(Lifecycle.ON_STOP);

        observer.reset();
        final User user3 = TestUtil.createUser(9);
        user3.setName("painter frida");
        mUserDao.insertOrReplace(user3);
        observer.assertNoUpdate();

        observer.reset();
        final User user4 = TestUtil.createUser(11);
        user4.setName("friday");
        mUserDao.insertOrReplace(user4);
        observer.assertNoUpdate();

        lifecycleProvider.handleEvent(Lifecycle.ON_START);
        assertThat(observer.get(), is(Arrays.asList(user4, user3)));
    }

    @Test
    public void liveDataWithPojo() throws ExecutionException, InterruptedException {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9);
        users[0].setAge(10);
        users[0].setWeight(15);

        users[1].setAge(20);
        users[1].setWeight(25);

        users[2].setAge(20);
        users[2].setWeight(26);

        users[3].setAge(10);
        users[3].setWeight(21);

        final TestLifecycleProvider lifecycleProvider = new TestLifecycleProvider();
        lifecycleProvider.handleEvent(Lifecycle.ON_START);

        final LatchObserver<AvgWeightByAge> observer = new LatchObserver<>();
        LiveData<AvgWeightByAge> liveData = mUserDao.maxWeightByAgeGroup();

        observe(liveData, lifecycleProvider, observer);
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        mUserDao.insertAll(users);
        assertThat(observer.get(), is(new AvgWeightByAge(20, 25.5f)));

        observer.reset();
        User user3 = mUserDao.load(3);
        user3.setWeight(79);
        mUserDao.insertOrReplace(user3);

        assertThat(observer.get(), is(new AvgWeightByAge(10, 50)));
    }

    @Test
    public void withRelation() throws ExecutionException, InterruptedException {
        final LiveData<UserAndAllPets> liveData = mUserPetDao.liveUserWithPets(3);
        final LatchObserver<UserAndAllPets> observer = new LatchObserver<>();
        final TestLifecycleProvider lifecycleProvider = new TestLifecycleProvider();
        lifecycleProvider.handleEvent(Lifecycle.ON_START);
        observe(liveData, lifecycleProvider, observer);
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        final UserAndAllPets noPets = observer.get();
        assertThat(noPets.user, is(user));

        observer.reset();
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mPetDao.insertAll(pets);

        final UserAndAllPets withPets = observer.get();
        assertThat(withPets.user, is(user));
        assertThat(withPets.pets, is(Arrays.asList(pets)));
    }

    private void observe(final LiveData liveData, final LifecycleProvider provider,
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

    static class TestLifecycleProvider implements LifecycleProvider {
        private LifecycleRegistry mLifecycle;

        TestLifecycleProvider() {
            mLifecycle = new LifecycleRegistry(this);
        }

        @Override
        public Lifecycle getLifecycle() {
            return mLifecycle;
        }

        void handleEvent(@Lifecycle.Event int event) {
            mLifecycle.handleLifecycleEvent(event);
        }
    }

    private class LatchObserver<T> implements Observer<T> {
        static final int TIMEOUT = 3;
        T mLastData;
        CountDownLatch mSetLatch = new CountDownLatch(1);

        void reset() {
            mSetLatch = new CountDownLatch(1);
        }
        @Override
        public void onChanged(@Nullable T o) {
            mLastData = o;
            mSetLatch.countDown();
        }

        void assertNoUpdate() throws InterruptedException {
            assertThat(mSetLatch.await(TIMEOUT, TimeUnit.SECONDS),
                    is(false));
        }

        T get() throws InterruptedException {
            assertThat(mSetLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
            return mLastData;
        }
    }
}
