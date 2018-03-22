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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.InvalidationTrackerTrojan;
import androidx.room.integration.testapp.vo.AvgWeightByAge;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetsToys;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests invalidation tracking.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class LiveDataQueryTest extends TestDatabaseTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    @Test
    public void observeById() throws InterruptedException, ExecutionException, TimeoutException {
        final LiveData<User> userLiveData = mUserDao.liveUserById(5);
        final TestLifecycleOwner testOwner = new TestLifecycleOwner();
        testOwner.handleEvent(Lifecycle.Event.ON_CREATE);
        final TestObserver<User> observer = new TestObserver<>();
        observe(userLiveData, testOwner, observer);
        assertThat(observer.hasValue(), is(false));
        observer.reset();

        testOwner.handleEvent(Lifecycle.Event.ON_START);
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

        testOwner.handleEvent(Lifecycle.Event.ON_STOP);
        observer.reset();
        u5.setName("baba");
        mUserDao.insertOrReplace(u5);
        assertThat(observer.hasValue(), is(false));
    }

    @Test
    public void observeListQuery() throws InterruptedException, ExecutionException,
            TimeoutException {
        final LiveData<List<User>> userLiveData = mUserDao.liveUsersListByName("frida");
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        final TestObserver<List<User>> observer = new TestObserver<>();
        observe(userLiveData, lifecycleOwner, observer);
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

        lifecycleOwner.handleEvent(Lifecycle.Event.ON_STOP);

        observer.reset();
        final User user3 = TestUtil.createUser(9);
        user3.setName("painter frida");
        mUserDao.insertOrReplace(user3);
        assertThat(observer.hasValue(), is(false));

        observer.reset();
        final User user4 = TestUtil.createUser(11);
        user4.setName("friday");
        mUserDao.insertOrReplace(user4);
        assertThat(observer.hasValue(), is(false));

        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        assertThat(observer.get(), is(Arrays.asList(user4, user3)));
    }

    @Test
    public void liveDataWithPojo() throws ExecutionException, InterruptedException,
            TimeoutException {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9);
        users[0].setAge(10);
        users[0].setWeight(15);

        users[1].setAge(20);
        users[1].setWeight(25);

        users[2].setAge(20);
        users[2].setWeight(26);

        users[3].setAge(10);
        users[3].setWeight(21);

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);

        final TestObserver<AvgWeightByAge> observer = new TestObserver<>();
        LiveData<AvgWeightByAge> liveData = mUserDao.maxWeightByAgeGroup();

        observe(liveData, lifecycleOwner, observer);
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
    public void withRelation() throws ExecutionException, InterruptedException, TimeoutException {
        final LiveData<UserAndAllPets> liveData = mUserPetDao.liveUserWithPets(3);
        final TestObserver<UserAndAllPets> observer = new TestObserver<>();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        observe(liveData, lifecycleOwner, observer);
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

    @Test
    public void withRelationOnly() throws ExecutionException, InterruptedException,
            TimeoutException {
        LiveData<PetsToys> liveData = mSpecificDogDao.getSpecificDogsToys();

        PetsToys expected = new PetsToys();
        expected.petId = 123;

        Toy toy = new Toy();
        toy.setId(1);
        toy.setPetId(123);
        toy.setName("ball");

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        final TestObserver<PetsToys> observer = new TestObserver<>();
        observe(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(expected));

        observer.reset();
        expected.toys.add(toy);
        mToyDao.insert(toy);
        assertThat(observer.get(), is(expected));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void withWithClause() throws ExecutionException, InterruptedException,
            TimeoutException {
        LiveData<List<String>> actual =
                mWithClauseDao.getUsersWithFactorialIdsLiveData(0);
        List<String> expected = new ArrayList<>();

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        final TestObserver<List<String>> observer = new TestObserver<>();
        observe(actual, lifecycleOwner, observer);
        assertThat(observer.get(), is(expected));

        observer.reset();
        User user = new User();
        user.setId(0);
        user.setName("Zero");
        mUserDao.insert(user);
        assertThat(observer.get(), is(expected));

        observer.reset();
        user = new User();
        user.setId(1);
        user.setName("One");
        mUserDao.insert(user);
        expected.add("One");
        assertThat(observer.get(), is(expected));

        observer.reset();
        user = new User();
        user.setId(6);
        user.setName("Six");
        mUserDao.insert(user);
        assertThat(observer.get(), is(expected));

        actual = mWithClauseDao.getUsersWithFactorialIdsLiveData(3);
        observe(actual, lifecycleOwner, observer);
        expected.add("Six");
        assertThat(observer.get(), is(expected));
    }

    @MediumTest
    @Test
    public void handleGc() throws ExecutionException, InterruptedException, TimeoutException {
        LiveData<User> liveData = mUserDao.liveUserById(3);
        final TestObserver<User> observer = new TestObserver<>();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        observe(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));
        observer.reset();
        final User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        assertThat(observer.get(), is(notNullValue()));
        observer.reset();
        forceGc();
        String name = UUID.randomUUID().toString();
        mUserDao.updateById(3, name);
        assertThat(observer.get().getName(), is(name));

        // release references
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                lifecycleOwner.handleEvent(Lifecycle.Event.ON_DESTROY);
            }
        });
        WeakReference<LiveData> weakLiveData = new WeakReference<LiveData>(liveData);
        //noinspection UnusedAssignment
        liveData = null;
        forceGc();
        mUserDao.updateById(3, "Bar");
        forceGc();
        assertThat(InvalidationTrackerTrojan.countObservers(mDatabase.getInvalidationTracker()),
                is(0));
        assertThat(weakLiveData.get(), nullValue());
    }

    @Test
    public void booleanLiveData() throws ExecutionException, InterruptedException,
            TimeoutException {
        User user = TestUtil.createUser(3);
        user.setAdmin(false);
        LiveData<Boolean> adminLiveData = mUserDao.isAdminLiveData(3);
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START);
        final TestObserver<Boolean> observer = new TestObserver<>();
        observe(adminLiveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));
        mUserDao.insert(user);
        assertThat(observer.get(), is(false));
        user.setAdmin(true);
        mUserDao.insertOrReplace(user);
        assertThat(observer.get(), is(true));
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
        ArchTaskExecutor.getInstance().executeOnMainThread(futureTask);
        futureTask.get();
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }

    private static void forceGc() {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
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

    private class TestObserver<T> implements Observer<T> {
        private T mLastData;
        private boolean mHasValue = false;

        void reset() {
            mHasValue = false;
            mLastData = null;
        }

        @Override
        public void onChanged(@Nullable T o) {
            mLastData = o;
            mHasValue = true;
        }

        boolean hasValue() throws TimeoutException, InterruptedException {
            drain();
            return mHasValue;
        }

        T get() throws InterruptedException, TimeoutException {
            drain();
            assertThat(hasValue(), is(true));
            return mLastData;
        }
    }
}
