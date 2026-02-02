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
package androidx.room3.integration.kotlintestapp.test

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.kruth.assertThat
import androidx.room3.integration.kotlintestapp.vo.User
import androidx.room3.rxjava3.EmptyResultSetException
import androidx.room3.rxjava3.NOTHING
import androidx.room3.rxjava3.createCompletable
import androidx.room3.rxjava3.createFlowable
import androidx.room3.rxjava3.createMaybe
import androidx.room3.rxjava3.createObservable
import androidx.room3.rxjava3.createSingle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.LockSupport
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RxJava3Test : TestDatabaseTest(UseDriver.ANDROID) {
    private lateinit var testScheduler: TestScheduler

    @Before
    fun setupSchedulers() {
        testScheduler = TestScheduler()
        testScheduler.start()
        ArchTaskExecutor.getInstance()
            .setDelegate(
                object : TaskExecutor() {
                    override fun executeOnDiskIO(runnable: Runnable) {
                        testScheduler.scheduleDirect(runnable)
                    }

                    override fun postToMainThread(runnable: Runnable) {
                        fail("no main thread in this test")
                    }

                    override fun isMainThread(): Boolean {
                        return false
                    }
                }
            )
    }

    @After
    fun clearSchedulers() {
        testScheduler.shutdown()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    private fun drainScheduler() {
        testScheduler.triggerActions()
    }

    @Test
    fun maybeUser_Empty() {
        val testObserver = usersDao.maybeUserId(3).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        testObserver.assertNoValues()
        testObserver.dispose()
    }

    @Test
    fun maybeUser_WithData() {
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        val testObserver = usersDao.maybeUserId(3).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        testObserver.assertValue(user)
        testObserver.dispose()
    }

    @Test
    fun maybeUsers_EmptyList() {
        val testObserver = usersDao.maybeUsersByIds(3, 5, 7).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
        testObserver.dispose()
    }

    @Test
    fun maybeUsers_WithValue() {
        val users = createUsersArray(3, 5)
        usersDao.insertUser(*users)
        val testObserver = usersDao.maybeUsersByIds(3, 5, 7).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver.assertValue(users.toList())
        testObserver.dispose()
    }

    @Test
    fun maybeUsers_keepMaybeReference() {
        val users = createUsersArray(1, 2)
        usersDao.insertUser(*users)
        val maybe1 = usersDao.maybeUserId(1)
        val testObserver1 = maybe1.observeOn(testScheduler).test()
        drainScheduler()
        testObserver1.assertComplete()
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver1.assertValue(users[0])
        val maybe2 = usersDao.maybeUserId(2)
        val testObserver2 = maybe2.observeOn(testScheduler).test()
        drainScheduler()
        testObserver2.assertComplete()
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver2.assertValue(users[1])
        val testObserver3 = maybe1.observeOn(testScheduler).test()
        drainScheduler()
        testObserver3.assertComplete()
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver3.assertValue(users[0])
        testObserver1.dispose()
        testObserver2.dispose()
        testObserver3.dispose()
    }

    @Test
    fun singleUser_Empty() {
        val testObserver = usersDao.singleUserId(3).observeOn(testScheduler).test()
        drainScheduler()
        // figure out which error we should dispatch
        testObserver.assertError(EmptyResultSetException::class.java)
        testObserver.assertNoValues()
        testObserver.dispose()
    }

    @Test
    fun singleUser_WithData() {
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        val testObserver = usersDao.singleUserId(3).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        testObserver.assertValue(user)
        testObserver.dispose()
    }

    @Test
    fun singleUsers_EmptyList() {
        val testObserver = usersDao.singleUsersByIds(3, 5, 7).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
        testObserver.dispose()
    }

    @Test
    fun singleUsers_WithValue() {
        val users = createUsersArray(3, 5)
        usersDao.insertUser(*users)
        val testObserver = usersDao.singleUsersByIds(3, 5, 7).observeOn(testScheduler).test()
        drainScheduler()
        testObserver.assertComplete()
        // since this is a clean db, it is ok to rely on the order for the test.
        testObserver.assertValue(users.toList())
        testObserver.dispose()
    }

    @Test
    fun singleUser_keepSingleReference() {
        val users = createUsersArray(1, 2)
        usersDao.insertUser(*users)
        val userSingle1 = usersDao.singleUserId(1)
        val testObserver1 = userSingle1.observeOn(testScheduler).test()
        drainScheduler()
        testObserver1.assertComplete()
        testObserver1.assertValue(users[0])
        testObserver1.dispose()

        // how get single for 2
        val userSingle2 = usersDao.singleUserId(2)
        val testObserver2 = userSingle2.observeOn(testScheduler).test()
        drainScheduler()
        testObserver2.assertComplete()
        testObserver2.assertValue(users[1])
        testObserver2.dispose()

        // now re-use the first single
        val testObserver3 = userSingle1.observeOn(testScheduler).test()
        drainScheduler()
        testObserver3.assertComplete()
        testObserver3.assertValue(users[0])
        testObserver3.dispose()
    }

    @Test
    fun observeOnce_Flowable() {
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        drainScheduler()
        val consumer = usersDao.flowableUserById(3).test()
        drainScheduler()
        consumer.assertValue(user)
        consumer.cancel()
    }

    @Test
    fun observeOnce_Observable() {
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        drainScheduler()
        val consumer = usersDao.observableUserById(3).test()
        drainScheduler()
        consumer.assertValue(user)
        consumer.dispose()
    }

    @Test
    fun observeChangeAndDispose_Flowable() {
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        drainScheduler()
        val consumer = usersDao.flowableUserById(3).observeOn(testScheduler).test()
        drainScheduler()
        assertThat(consumer.values()[0]).isEqualTo(user)
        user.email = user.email?.copy(address = "rxy")
        usersDao.insertOrReplaceUser(user)
        drainScheduler()
        val next = consumer.values()[1]
        assertThat(next).isEqualTo(user)
        consumer.cancel()
        user.email = user.email?.copy(address = "foo")
        usersDao.insertOrReplaceUser(user)
        drainScheduler()
        consumer.assertValueCount(2)
    }

    @Test
    fun observeChangeAndDispose_Observable() {
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        drainScheduler()
        val consumer = usersDao.observableUserById(3).observeOn(testScheduler).test()
        drainScheduler()
        assertThat(consumer.values()[0]).isEqualTo(user)
        user.email = user.email?.copy(address = "rxy")
        usersDao.insertOrReplaceUser(user)
        drainScheduler()
        val next = consumer.values()[1]
        assertThat(next).isEqualTo(user)
        consumer.dispose()
        user.email = user.email?.copy(address = "foo")
        usersDao.insertOrReplaceUser(user)
        drainScheduler()
        consumer.assertValueCount(2)
    }

    @Test
    @MediumTest
    fun observeEmpty_Flowable() {
        val consumer = usersDao.flowableUserById(3).observeOn(testScheduler).test()
        drainScheduler()
        consumer.assertNoValues()
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        drainScheduler()
        assertThat(consumer.values()[0]).isEqualTo(user)
        consumer.cancel()
        user.email = user.email?.copy(address = "foo")
        usersDao.insertOrReplaceUser(user)
        drainScheduler()
        consumer.assertValueCount(1)
    }

    @Test
    @MediumTest
    fun observeEmpty_Observable() {
        val consumer = usersDao.observableUserById(3).observeOn(testScheduler).test()
        drainScheduler()
        consumer.assertNoValues()
        val user = TestUtil.createUser(3)
        usersDao.insertUser(user)
        drainScheduler()
        assertThat(consumer.values()[0]).isEqualTo(user)
        consumer.dispose()
        user.email = user.email?.copy(address = "foo")
        usersDao.insertOrReplaceUser(user)
        drainScheduler()
        consumer.assertValueCount(1)
    }

    @Test
    fun keepReference_Flowable() {
        val users = createUsersArray(1, 2)
        usersDao.insertUser(*users)
        drainScheduler()
        val flowable1 = usersDao.flowableUserById(1)
        val consumer1 = flowable1.test()
        drainScheduler()
        consumer1.assertValue(users[0])
        val consumer2 = usersDao.flowableUserById(2).test()
        drainScheduler()
        consumer2.assertValue(users[1])
        val consumer3 = flowable1.test()
        drainScheduler()
        consumer3.assertValue(users[0])
        consumer1.cancel()
        consumer2.cancel()
        consumer3.cancel()
    }

    @Test
    fun keepReference_Observable() {
        val users = createUsersArray(1, 2)
        usersDao.insertUser(*users)
        drainScheduler()
        val observable1 = usersDao.observableUserById(1)
        val consumer1 = observable1.test()
        drainScheduler()
        consumer1.assertValue(users[0])
        val consumer2 = usersDao.observableUserById(2).test()
        drainScheduler()
        consumer2.assertValue(users[1])
        val consumer3 = observable1.test()
        drainScheduler()
        consumer3.assertValue(users[0])
        consumer1.dispose()
        consumer2.dispose()
        consumer3.dispose()
    }

    @Test
    fun countUsers_Flowable() {
        val consumer = usersDao.flowableCountUsers().observeOn(testScheduler).test()
        drainScheduler()
        assertThat(consumer.values()[0]).isEqualTo(0)
        usersDao.insertUser(*createUsersArray(1, 3, 4, 6))
        drainScheduler()
        assertThat(consumer.values()[1]).isEqualTo(4)
        usersDao.deleteByUids(3, 7)
        drainScheduler()
        assertThat(consumer.values()[2]).isEqualTo(3)
        usersDao.deleteByUids(101)
        drainScheduler()
        consumer.assertValueCount(3)
    }

    @Test
    @MediumTest
    fun countUsers_Publisher() {
        val subscriber = TestSubscriber<Int>()
        usersDao.publisherCountUsers().subscribe(subscriber)
        drainScheduler()
        subscriber.request(2)
        drainScheduler()
        subscriber.assertValue(0)
        usersDao.insertUser(TestUtil.createUser(2))
        drainScheduler()
        subscriber.assertValues(0, 1)
        subscriber.cancel()
        subscriber.assertNoErrors()
    }

    @Test
    fun singleFromCallable_emptyResult_disposed() {
        val queryLatch = CountDownLatch(1)
        val bgThreadLatch = CountDownLatch(1)
        val testObserver =
            Single.fromCallable {
                    bgThreadLatch.countDown()
                    queryLatch.await()
                    throw EmptyResultSetException("Empty result")
                }
                .subscribeOn(testScheduler)
                .test()
        val t = Thread {
            try {
                RxJavaPlugins.setErrorHandler { e: Throwable ->
                    assertThat(e).isInstanceOf<UndeliverableException>()
                    RxJavaPlugins.setErrorHandler(null)
                }
                drainScheduler()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
        t.start()
        bgThreadLatch.await()
        testObserver.assertNotComplete()
        testObserver.dispose()
        queryLatch.countDown()
        t.join()
        testObserver.assertNoValues()
        testObserver.assertNotComplete()
    }

    @Test
    fun createCompletable_cancellable() {
        val testObserver = TestObserver<Void>()
        val completable =
            createCompletable(database, false, false) { LockSupport.parkNanos(Long.MAX_VALUE) }
        completable.subscribe(testObserver)
        testObserver.assertNotComplete()
        testObserver.dispose()
        testObserver.assertNoErrors()
    }

    @Test
    fun createSingle_cancellable() {
        val testObserver = TestObserver<String>()
        val single =
            createSingle(database, false, false) {
                LockSupport.parkNanos(Long.MAX_VALUE)
                ""
            }
        single.subscribe(testObserver)
        testObserver.assertNotComplete()
        testObserver.dispose()
        testObserver.assertNoErrors()
    }

    @Test
    fun createMaybe_cancellable() {
        val testObserver = TestObserver<String>()
        val maybe =
            createMaybe(database, false, false) {
                LockSupport.parkNanos(Long.MAX_VALUE)
                ""
            }
        maybe.subscribe(testObserver)
        testObserver.assertNotComplete()
        testObserver.dispose()
        testObserver.assertNoErrors()
    }

    @Test
    fun createObservable_helper() {
        // Validates the public API utility helper, the ones not used by generated code
        val testObservable = createObservable(database, "user").test()
        drainScheduler()
        testObservable.assertValueCount(1)
        testObservable.assertValueAt(0, NOTHING)
        database.usersDao().insertUser(TestUtil.createUser(1))
        drainScheduler()
        testObservable.assertValueCount(2)
        testObservable.assertValueAt(1, NOTHING)
    }

    @Test
    fun createFlowable_helper() {
        // Validates the public API utility helper, the ones not used by generated code
        val testConsumer = createFlowable(database, "user").test()
        drainScheduler()
        testConsumer.assertValueCount(1)
        testConsumer.assertValueAt(0, NOTHING)
        database.usersDao().insertUser(TestUtil.createUser(1))
        drainScheduler()
        testConsumer.assertValueCount(2)
        testConsumer.assertValueAt(1, NOTHING)
    }

    private fun createUsersArray(vararg ids: Int): Array<User> {
        return Array(ids.size) { TestUtil.createUser(ids[it]) }
    }
}
