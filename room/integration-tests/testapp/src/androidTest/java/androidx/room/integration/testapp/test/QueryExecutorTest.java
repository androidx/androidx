/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.DefaultTaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.vo.Pet;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class QueryExecutorTest {

    /**
     * Blocks the calling thread while executing input commands on a background thread. Also records
     * executed tasks into a queue.
     */
    private static class RecordingExecutor implements Executor {
        private final Queue<Runnable> mTaskQueue = new ArrayDeque<>();

        private ExecutorService mExecutorService = Executors.newFixedThreadPool(3);

        @Override
        public void execute(Runnable command) {
            try {
                mExecutorService.submit(command).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mTaskQueue.add(command);
        }
    }

    private RecordingExecutor mRecordingExecutor;
    private TestDatabase mTestDatabase;
    private PetDao mPetDao;

    @Before
    public void setUp() {
        // Set ArchTaskExecutor to crash. It should never be invoked.
        ArchTaskExecutor.getInstance().setDelegate(new DefaultTaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                throw new IllegalStateException(
                        "ArchTestExecutor was used instead of the set Executor.");
            }
        });

        mRecordingExecutor = new RecordingExecutor();
        mTestDatabase = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                TestDatabase.class)
                .setQueryExecutor(mRecordingExecutor)
                .build();
        mPetDao = mTestDatabase.getPetDao();
    }

    @After
    public void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
        mTestDatabase.close();
    }

    @Test
    public void testInitDoesNotUseExecutor() {
        assertThat(
                mRecordingExecutor.mTaskQueue.isEmpty(),
                is(true));
    }

    @Test
    public void testGuavaListenableFutureUsesExecutorOnQuery()
            throws ExecutionException, InterruptedException {
        mPetDao.insertOrReplace(TestUtil.createPet(2));
        ListenableFuture<Optional<Pet>> petsNamedFuture = mPetDao.petWithIdFuture(2);

        assertThat(
                mRecordingExecutor.mTaskQueue.size(),
                greaterThan(0));
        assertThat(
                petsNamedFuture.get().isPresent(),
                is(true));
        assertThat(
                petsNamedFuture.get().get().getPetId(),
                is(2));
    }

    @Test
    public void testRxUsesExecutorOnConsume() {
        mPetDao.insertOrReplace(TestUtil.createPet(2));
        Pet pet = mPetDao.petWithIdFlowable(2).blockingFirst();

        assertThat(
                mRecordingExecutor.mTaskQueue.size(),
                greaterThan(0));
        assertThat(
                pet.getPetId(),
                is(2));
    }

    @Test
    public void testLiveDataUsesExecutorOnObserve()
            throws ExecutionException, InterruptedException {
        mPetDao.insertOrReplace(TestUtil.createPet(2));
        LiveData<Pet> petLiveData = mPetDao.petWithIdLiveData(2);

        SettableFuture<Pet> observedPet = SettableFuture.create();
        getInstrumentation().runOnMainSync(() -> petLiveData.observeForever(pet -> {
            observedPet.set(pet);
        }));
        getInstrumentation().waitForIdleSync();

        assertThat(
                mRecordingExecutor.mTaskQueue.size(),
                greaterThan(0));
        assertThat(petLiveData.getValue().getPetId(), is(2));
        assertThat(observedPet.get().getPetId(), is(2));
    }
}
