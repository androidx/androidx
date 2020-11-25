/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.rxjava2;

import static com.google.common.truth.Truth.assertThat;

import androidx.datastore.core.DataStore;
import androidx.datastore.core.DataStoreFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.subscribers.TestSubscriber;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

public class RxDataStoreTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static Single<Byte> incrementByte(Byte byteIn) {
        return Single.just(++byteIn);
    }

    @Test
    public void testGetSingleValue() throws Exception {
        File newFile = tempFolder.newFile();

        DataStore<Byte> byteStore = DataStoreFactory.INSTANCE.create(
                new TestingSerializer(),
                null,
                new ArrayList<>(),
                CoroutineScopeKt.CoroutineScope(Dispatchers.getIO()),
                () -> newFile);

        Byte firstByte = RxDataStore.data(byteStore).blockingFirst();
        assertThat(firstByte).isEqualTo(0);

        Single<Byte> incrementByte = RxDataStore.updateDataAsync(byteStore,
                RxDataStoreTest::incrementByte);

        assertThat(incrementByte.blockingGet()).isEqualTo(1);

        firstByte = RxDataStore.data(byteStore).blockingFirst();
        assertThat(firstByte).isEqualTo(1);
    }

    @Test
    public void testTake3() throws Exception {
        File newFile = tempFolder.newFile();

        DataStore<Byte> byteStore = DataStoreFactory.INSTANCE.create(
                new TestingSerializer(),
                null,
                new ArrayList<>(),
                CoroutineScopeKt.CoroutineScope(Dispatchers.getIO()),
                () -> newFile);

        TestSubscriber<Byte> testSubscriber = RxDataStore.data(byteStore).test();

        RxDataStore.updateDataAsync(byteStore, RxDataStoreTest::incrementByte);
        RxDataStore.updateDataAsync(byteStore, RxDataStoreTest::incrementByte);

        testSubscriber.awaitCount(3).assertValues((byte) 0, (byte) 1, (byte) 2);
    }


    @Test
    public void testReadFailure() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        DataStore<Byte> byteStore = DataStoreFactory.INSTANCE.create(
                testingSerializer,
                null,
                new ArrayList<>(),
                CoroutineScopeKt.CoroutineScope(Dispatchers.getIO()),
                () -> newFile);

        testingSerializer.setFailingRead(true);

        TestSubscriber<Byte> testSubscriber = RxDataStore.data(byteStore).test();

        assertThat(testSubscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)).isTrue();

        testSubscriber.assertError(IOException.class);

        testingSerializer.setFailingRead(false);

        testSubscriber = RxDataStore.data(byteStore).test();
        testSubscriber.awaitCount(1).assertValues((byte) 0);
    }

    @Test
    public void testWriteFailure() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        DataStore<Byte> byteStore = DataStoreFactory.INSTANCE.create(
                testingSerializer,
                null,
                new ArrayList<>(),
                CoroutineScopeKt.CoroutineScope(Dispatchers.getIO()),
                () -> newFile);

        TestSubscriber<Byte> testSubscriber = RxDataStore.data(byteStore).test();

        testingSerializer.setFailingWrite(true);
        Single<Byte> incrementByte = RxDataStore.updateDataAsync(byteStore,
                RxDataStoreTest::incrementByte);

        incrementByte.cache().test().await().assertError(IOException.class);

        testSubscriber.awaitCount(1).assertNoErrors().assertValues((byte) 0);
        testingSerializer.setFailingWrite(false);

        Single<Byte> incrementByte2 = RxDataStore.updateDataAsync(byteStore,
                RxDataStoreTest::incrementByte);
        assertThat(incrementByte2.blockingGet()).isEqualTo((byte) 1);

        testSubscriber.awaitCount(2).assertValues((byte) 0, (byte) 1);
    }
}
