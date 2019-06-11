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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Build;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.room.EmptyResultSetException;
import androidx.room.Room;
import androidx.room.RxRoom;
import androidx.room.integration.testapp.FtsTestDatabase;
import androidx.room.integration.testapp.dao.MailDao;
import androidx.room.integration.testapp.vo.Mail;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RxJava2Test extends TestDatabaseTest {

    private TestScheduler mTestScheduler;

    @Before
    public void setupSchedulers() {
        mTestScheduler = new TestScheduler();
        mTestScheduler.start();
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                mTestScheduler.scheduleDirect(runnable);
            }

            @Override
            public void postToMainThread(Runnable runnable) {
                Assert.fail("no main thread in this test");
            }

            @Override
            public boolean isMainThread() {
                return false;
            }
        });
    }

    @After
    public void clearSchedulers() {
        mTestScheduler.shutdown();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    private void drain() throws InterruptedException {
        mTestScheduler.triggerActions();
    }

    @Test
    public void maybeUser_Empty() throws InterruptedException {
        TestObserver<User> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.maybeUserById(3).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        testObserver.assertNoValues();
        disposable.dispose();
    }

    @Test
    public void maybeUser_WithData() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        TestObserver<User> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.maybeUserById(3).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        testObserver.assertValue(user);

        disposable.dispose();
    }

    @Test
    public void maybeUsers_EmptyList() throws InterruptedException {
        TestObserver<List<User>> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.maybeUsersByIds(3, 5, 7).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        testObserver.assertValue(Collections.<User>emptyList());
        disposable.dispose();
    }

    @Test
    public void maybeUsers_WithValue() throws InterruptedException {
        User[] users = TestUtil.createUsersArray(3, 5);
        mUserDao.insertAll(users);
        TestObserver<List<User>> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.maybeUsersByIds(3, 5, 7).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver.assertValue(Arrays.asList(users));
        disposable.dispose();
    }

    @Test
    public void maybeUsers_keepMaybeReference() throws InterruptedException {
        User[] users = TestUtil.createUsersArray(1, 2);
        mUserDao.insertAll(users);
        TestObserver<User> testObserver1 = new TestObserver<>();
        Maybe<User> maybe1 = mUserDao.maybeUserById(1);
        Disposable disposable1 = maybe1.observeOn(mTestScheduler)
                .subscribeWith(testObserver1);
        drain();
        testObserver1.assertComplete();
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver1.assertValue(users[0]);

        TestObserver<User> testObserver2 = new TestObserver<>();
        Maybe<User> maybe2 = mUserDao.maybeUserById(2);
        Disposable disposable2 = maybe2.observeOn(mTestScheduler)
                .subscribeWith(testObserver2);
        drain();
        testObserver2.assertComplete();
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver2.assertValue(users[1]);

        TestObserver<User> testObserver3 = new TestObserver<>();

        Disposable disposable3 = maybe1.observeOn(mTestScheduler)
                .subscribeWith(testObserver3);
        drain();
        testObserver3.assertComplete();
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver3.assertValue(users[0]);

        disposable1.dispose();
        disposable2.dispose();
        disposable3.dispose();
    }

    @Test
    public void singleUser_Empty() throws InterruptedException {
        TestObserver<User> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.singleUserById(3).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        // figure out which error we should dispatch
        testObserver.assertError(EmptyResultSetException.class);
        testObserver.assertNoValues();
        disposable.dispose();
    }

    @Test
    public void singleUser_WithData() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        TestObserver<User> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.singleUserById(3).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        testObserver.assertValue(user);

        disposable.dispose();
    }

    @Test
    public void singleUsers_EmptyList() throws InterruptedException {
        TestObserver<List<User>> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.singleUsersByIds(3, 5, 7).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        testObserver.assertValue(Collections.<User>emptyList());
        disposable.dispose();
    }

    @Test
    public void singleUsers_WithValue() throws InterruptedException {
        User[] users = TestUtil.createUsersArray(3, 5);
        mUserDao.insertAll(users);
        TestObserver<List<User>> testObserver = new TestObserver<>();
        Disposable disposable = mUserDao.singleUsersByIds(3, 5, 7).observeOn(mTestScheduler)
                .subscribeWith(testObserver);
        drain();
        testObserver.assertComplete();
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver.assertValue(Arrays.asList(users));
        disposable.dispose();
    }

    @Test
    public void singleUser_keepSingleReference() throws InterruptedException {
        User[] users = TestUtil.createUsersArray(1, 2);
        mUserDao.insertAll(users);
        TestObserver<User> testObserver1 = new TestObserver<>();
        Single<User> userSingle1 = mUserDao.singleUserById(1);
        Disposable disposable1 = userSingle1.observeOn(mTestScheduler)
                .subscribeWith(testObserver1);
        drain();
        testObserver1.assertComplete();
        testObserver1.assertValue(users[0]);
        disposable1.dispose();

        // how get single for 2
        TestObserver<User> testObserver2 = new TestObserver<>();
        Single<User> userSingle2 = mUserDao.singleUserById(2);
        Disposable disposable2 = userSingle2.observeOn(mTestScheduler)
                .subscribeWith(testObserver2);
        drain();
        testObserver2.assertComplete();
        testObserver2.assertValue(users[1]);
        disposable2.dispose();

        // now re-use the first single
        TestObserver<User> testObserver3 = new TestObserver<>();
        Disposable disposable3 = userSingle1.observeOn(mTestScheduler)
                .subscribeWith(testObserver3);
        drain();
        testObserver3.assertComplete();
        testObserver3.assertValue(users[0]);
        disposable3.dispose();
    }

    @Test
    public void observeOnce_Flowable() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        TestSubscriber<User> consumer = new TestSubscriber<>();
        Disposable disposable = mUserDao.flowableUserById(3).subscribeWith(consumer);
        drain();
        consumer.assertValue(user);
        disposable.dispose();
    }

    @Test
    public void observeOnce_Observable() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        TestObserver<User> consumer = new TestObserver<>();
        Disposable disposable = mUserDao.observableUserById(3).subscribeWith(consumer);
        drain();
        consumer.assertValue(user);
        disposable.dispose();
    }

    @Test
    public void observeChangeAndDispose_Flowable() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        TestSubscriber<User> consumer = new TestSubscriber<>();
        Disposable disposable = mUserDao.flowableUserById(3).observeOn(mTestScheduler)
                .subscribeWith(consumer);
        drain();
        assertThat(consumer.values().get(0), is(user));
        user.setName("rxy");
        mUserDao.insertOrReplace(user);
        drain();
        User next = consumer.values().get(1);
        assertThat(next, is(user));
        disposable.dispose();
        user.setName("foo");
        mUserDao.insertOrReplace(user);
        drain();
        assertThat(consumer.valueCount(), is(2));
    }

    @Test
    public void observeChangeAndDispose_Observable() throws InterruptedException {
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        TestObserver<User> consumer = new TestObserver<>();
        Disposable disposable = mUserDao.observableUserById(3).observeOn(mTestScheduler)
                .subscribeWith(consumer);
        drain();
        assertThat(consumer.values().get(0), is(user));
        user.setName("rxy");
        mUserDao.insertOrReplace(user);
        drain();
        User next = consumer.values().get(1);
        assertThat(next, is(user));
        disposable.dispose();
        user.setName("foo");
        mUserDao.insertOrReplace(user);
        drain();
        assertThat(consumer.valueCount(), is(2));
    }

    @Test
    @MediumTest
    public void observeEmpty_Flowable() throws InterruptedException {
        TestSubscriber<User> consumer = new TestSubscriber<>();
        Disposable disposable = mUserDao.flowableUserById(3).observeOn(mTestScheduler)
                .subscribeWith(consumer);
        drain();
        consumer.assertNoValues();
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        assertThat(consumer.values().get(0), is(user));
        disposable.dispose();
        user.setAge(88);
        mUserDao.insertOrReplace(user);
        drain();
        assertThat(consumer.valueCount(), is(1));
    }

    @Test
    @MediumTest
    public void observeEmpty_Observable() throws InterruptedException {
        TestObserver<User> consumer = new TestObserver<>();
        Disposable disposable = mUserDao.observableUserById(3).observeOn(mTestScheduler)
                .subscribeWith(consumer);
        drain();
        consumer.assertNoValues();
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        assertThat(consumer.values().get(0), is(user));
        disposable.dispose();
        user.setAge(88);
        mUserDao.insertOrReplace(user);
        drain();
        assertThat(consumer.valueCount(), is(1));
    }

    @Test
    public void keepReference_Flowable() throws InterruptedException {
        User[] users = TestUtil.createUsersArray(1, 2);
        mUserDao.insertAll(users);
        drain();

        TestSubscriber<User> consumer1 = new TestSubscriber<>();
        Flowable<User> flowable1 = mUserDao.flowableUserById(1);
        Disposable disposable1 = flowable1.subscribeWith(consumer1);
        drain();
        consumer1.assertValue(users[0]);

        TestSubscriber<User> consumer2 = new TestSubscriber<>();
        Disposable disposable2 = mUserDao.flowableUserById(2).subscribeWith(consumer2);
        drain();
        consumer2.assertValue(users[1]);

        TestSubscriber<User> consumer3 = new TestSubscriber<>();
        Disposable disposable3 = flowable1.subscribeWith(consumer3);
        drain();
        consumer3.assertValue(users[0]);

        disposable1.dispose();
        disposable2.dispose();
        disposable3.dispose();
    }

    @Test
    public void keepReference_Observable() throws InterruptedException {
        User[] users = TestUtil.createUsersArray(1, 2);
        mUserDao.insertAll(users);
        drain();

        TestObserver<User> consumer1 = new TestObserver<>();
        Observable<User> flowable1 = mUserDao.observableUserById(1);
        Disposable disposable1 = flowable1.subscribeWith(consumer1);
        drain();
        consumer1.assertValue(users[0]);

        TestObserver<User> consumer2 = new TestObserver<>();
        Disposable disposable2 = mUserDao.observableUserById(2).subscribeWith(consumer2);
        drain();
        consumer2.assertValue(users[1]);

        TestObserver<User> consumer3 = new TestObserver<>();
        Disposable disposable3 = flowable1.subscribeWith(consumer3);
        drain();
        consumer3.assertValue(users[0]);

        disposable1.dispose();
        disposable2.dispose();
        disposable3.dispose();
    }

    @Test
    public void countUsers_Flowable() throws InterruptedException {
        TestSubscriber<Integer> consumer = new TestSubscriber<>();
        mUserDao.flowableCountUsers()
                .observeOn(mTestScheduler)
                .subscribe(consumer);
        drain();
        assertThat(consumer.values().get(0), is(0));
        mUserDao.insertAll(TestUtil.createUsersArray(1, 3, 4, 6));
        drain();
        assertThat(consumer.values().get(1), is(4));
        mUserDao.deleteByUids(3, 7);
        drain();
        assertThat(consumer.values().get(2), is(3));
        mUserDao.deleteByUids(101);
        drain();
        assertThat(consumer.valueCount(), is(3));
    }

    @Test
    @MediumTest
    public void countUsers_Publisher() throws InterruptedException {
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        mUserDao.publisherCountUsers().subscribe(subscriber);
        drain();
        subscriber.assertSubscribed();
        subscriber.request(2);
        drain();
        subscriber.assertValue(0);
        mUserDao.insert(TestUtil.createUser(2));
        drain();
        subscriber.assertValues(0, 1);
        subscriber.cancel();
        subscriber.assertNoErrors();
    }

    @Test
    public void withRelation_Flowable() throws InterruptedException {
        final TestSubscriber<UserAndAllPets> subscriber = new TestSubscriber<>();

        mUserPetDao.flowableUserWithPets(3).subscribe(subscriber);
        drain();
        subscriber.assertSubscribed();

        drain();
        subscriber.assertNoValues();

        final User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        subscriber.assertValue(new Predicate<UserAndAllPets>() {
            @Override
            public boolean test(UserAndAllPets userAndAllPets) throws Exception {
                return userAndAllPets.user.equals(user);
            }
        });
        subscriber.assertValueCount(1);
        final Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mPetDao.insertAll(pets);
        drain();
        subscriber.assertValueAt(1, new Predicate<UserAndAllPets>() {
            @Override
            public boolean test(UserAndAllPets userAndAllPets) throws Exception {
                return userAndAllPets.user.equals(user)
                        && userAndAllPets.pets.equals(Arrays.asList(pets));
            }
        });
    }

    @Test
    public void withRelation_Observable() throws InterruptedException {
        final TestObserver<UserAndAllPets> subscriber = new TestObserver<>();

        mUserPetDao.observableUserWithPets(3).subscribe(subscriber);
        drain();
        subscriber.assertSubscribed();

        drain();
        subscriber.assertNoValues();

        final User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        drain();
        subscriber.assertValue(new Predicate<UserAndAllPets>() {
            @Override
            public boolean test(UserAndAllPets userAndAllPets) throws Exception {
                return userAndAllPets.user.equals(user);
            }
        });
        subscriber.assertValueCount(1);
        final Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mPetDao.insertAll(pets);
        drain();
        subscriber.assertValueAt(1, new Predicate<UserAndAllPets>() {
            @Override
            public boolean test(UserAndAllPets userAndAllPets) throws Exception {
                return userAndAllPets.user.equals(user)
                        && userAndAllPets.pets.equals(Arrays.asList(pets));
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Test
    public void updateInTransaction_Flowable() throws InterruptedException {
        // When subscribing to the emissions of the user
        final TestSubscriber<User> userTestSubscriber = mUserDao
                .flowableUserById(3)
                .observeOn(mTestScheduler)
                .test();
        drain();
        userTestSubscriber.assertValueCount(0);

        // When inserting a new user in the data source
        mDatabase.beginTransaction();
        try {
            mUserDao.insert(TestUtil.createUser(3));
            mDatabase.setTransactionSuccessful();

        } finally {
            mDatabase.endTransaction();
        }
        drain();
        userTestSubscriber.assertValueCount(1);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void updateInTransaction_Observable() throws InterruptedException {
        // When subscribing to the emissions of the user
        final TestObserver<User> userTestSubscriber = mUserDao
                .observableUserById(3)
                .observeOn(mTestScheduler)
                .test();
        drain();
        userTestSubscriber.assertValueCount(0);

        // When inserting a new user in the data source
        mDatabase.beginTransaction();
        try {
            mUserDao.insert(TestUtil.createUser(3));
            mDatabase.setTransactionSuccessful();

        } finally {
            mDatabase.endTransaction();
        }
        drain();
        userTestSubscriber.assertValueCount(1);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    public void withFtsTable_Flowable() throws InterruptedException {
        final Context context = ApplicationProvider.getApplicationContext();
        final FtsTestDatabase db = Room.inMemoryDatabaseBuilder(context, FtsTestDatabase.class)
                .build();
        final MailDao mailDao = db.getMailDao();
        final TestSubscriber<List<Mail>> subscriber = new TestSubscriber<>();

        mailDao.getFlowableMail().subscribe(subscriber);
        drain();
        subscriber.assertSubscribed();
        subscriber.assertValue(Collections.emptyList());

        Mail mail0 = TestUtil.createMail(1, "subject0", "body0");
        mailDao.insert(mail0);
        drain();
        subscriber.assertValueAt(1, new Predicate<List<Mail>>() {
            @Override
            public boolean test(List<Mail> mailList) throws Exception {
                return mailList.equals(Lists.newArrayList(mail0));
            }
        });

        Mail mail1 = TestUtil.createMail(2, "subject1", "body1");
        mailDao.insert(mail1);
        drain();
        subscriber.assertValueAt(2, new Predicate<List<Mail>>() {
            @Override
            public boolean test(List<Mail> mailList) throws Exception {
                return mailList.equals(Lists.newArrayList(mail0, mail1));
            }
        });
    }

    @Test
    public void singleFromCallable_emptyResult_disposed() throws InterruptedException {
        CountDownLatch queryLatch = new CountDownLatch(1);
        CountDownLatch bgThreadLatch = new CountDownLatch(1);
        TestObserver<Boolean> testObserver = new TestObserver<>();
        Disposable disposable = Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                bgThreadLatch.countDown();
                queryLatch.await();
                throw new EmptyResultSetException("Empty result");
            }
        }).subscribeOn(mTestScheduler).subscribeWith(testObserver);

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    RxJavaPlugins.setErrorHandler(e -> {
                        assertThat(e, instanceOf(UndeliverableException.class));
                        RxJavaPlugins.setErrorHandler(null);
                    });
                    drain();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        t.start();

        bgThreadLatch.await();
        testObserver.assertNotComplete();
        disposable.dispose();
        queryLatch.countDown();
        t.join();

        testObserver.assertNoValues();
        testObserver.assertNotTerminated();
    }

    @Test
    public void createSingle_emptyResult_disposed() throws InterruptedException {
        CountDownLatch queryLatch = new CountDownLatch(1);
        CountDownLatch bgThreadLatch = new CountDownLatch(1);
        TestObserver<Boolean> testObserver = new TestObserver<>();
        Disposable disposable = RxRoom.createSingle(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                bgThreadLatch.countDown();
                queryLatch.await();
                throw new EmptyResultSetException("Empty result");
            }
        }).subscribeOn(mTestScheduler).subscribeWith(testObserver);

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    drain();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        t.start();

        bgThreadLatch.await();
        testObserver.assertNotComplete();
        disposable.dispose();
        queryLatch.countDown();
        t.join();

        testObserver.assertNoValues();
        testObserver.assertNotTerminated();
    }
}
