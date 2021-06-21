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

import static androidx.testutils.AssertionsKt.assertThrows;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;

public class RxDataStoreTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static Single<Byte> incrementByte(Byte byteIn) {
        return Single.just(++byteIn);
    }

    @Test
    public void testGetSingleValue() throws Exception {
        File newFile = tempFolder.newFile();

        RxDataStore<Byte> byteStore =
                new RxDataStoreBuilder<Byte>(() -> newFile, new TestingSerializer()).build();


        Byte firstByte = byteStore.data().blockingFirst();
        assertThat(firstByte).isEqualTo(0);

        Single<Byte> incrementByte = byteStore.updateDataAsync(
                RxDataStoreTest::incrementByte);

        assertThat(incrementByte.blockingGet()).isEqualTo(1);

        firstByte = byteStore.data().blockingFirst();
        assertThat(firstByte).isEqualTo(1);
    }

    @Test
    public void testTake3() throws Exception {
        File newFile = tempFolder.newFile();

        RxDataStore<Byte> byteStore =
                new RxDataStoreBuilder<Byte>(() -> newFile, new TestingSerializer())
                        .build();

        TestSubscriber<Byte> testSubscriber = byteStore.data().test();

        byteStore.updateDataAsync(RxDataStoreTest::incrementByte);
        // Wait for our subscriber to see the second write, otherwise we may skip from 0 - 2
        testSubscriber.awaitCount(2);
        byteStore.updateDataAsync(RxDataStoreTest::incrementByte);
        testSubscriber.awaitCount(3).assertValues((byte) 0, (byte) 1, (byte) 2);
    }


    @Test
    public void testReadFailure() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        RxDataStore<Byte> byteStore =
                new RxDataStoreBuilder<Byte>(() -> newFile, testingSerializer).build();


        testingSerializer.setFailingRead(true);

        TestSubscriber<Byte> testSubscriber = byteStore.data().test();

        assertThat(testSubscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)).isTrue();

        testSubscriber.assertError(IOException.class);

        testingSerializer.setFailingRead(false);

        testSubscriber = byteStore.data().test();
        testSubscriber.awaitCount(1).assertValues((byte) 0);
    }

    @Test
    public void testWriteFailure() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        RxDataStore<Byte> byteStore =
                new RxDataStoreBuilder<Byte>(() -> newFile, testingSerializer).build();

        TestSubscriber<Byte> testSubscriber = byteStore.data().test();

        testingSerializer.setFailingWrite(true);
        Single<Byte> incrementByte = byteStore.updateDataAsync(RxDataStoreTest::incrementByte);

        incrementByte.cache().test().await().assertError(IOException.class);

        testSubscriber.awaitCount(1).assertNoErrors().assertValues((byte) 0);
        testingSerializer.setFailingWrite(false);

        Single<Byte> incrementByte2 = byteStore.updateDataAsync(RxDataStoreTest::incrementByte);
        assertThat(incrementByte2.blockingGet()).isEqualTo((byte) 1);

        testSubscriber.awaitCount(2).assertValues((byte) 0, (byte) 1);
    }

    @Test
    public void openingSameDataStoreTwice_throwsException() throws IOException {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        RxDataStore<Byte> byteRxDataStore = new RxDataStoreBuilder<Byte>(() -> newFile,
                testingSerializer).build();

        assertThat(byteRxDataStore.data().blockingFirst()).isEqualTo((byte) 0);

        RxDataStore<Byte> byteRxDataStore2 = new RxDataStoreBuilder<Byte>(() -> newFile,
                testingSerializer).build();

        assertThrows(IllegalStateException.class, () -> byteRxDataStore2.data().blockingFirst());
    }

    @Test
    public void canCloseDataStore() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        RxDataStore<Byte> byteRxDataStore = new RxDataStoreBuilder<Byte>(() -> newFile,
                testingSerializer).build();

        assertThat(byteRxDataStore.data().blockingFirst()).isEqualTo((byte) 0);

        byteRxDataStore.dispose();
        byteRxDataStore.shutdownComplete().blockingAwait();


        TestSubscriber<Byte> testSubscriber = byteRxDataStore.data().test();

        assertThat(testSubscriber.awaitTerminalEvent()).isTrue();
        testSubscriber.assertTerminated()
                // FIXME: This used to be different from coroutines bc asFlowable used to convert
                //        the CancellationException to onComplete. This behavior changed with
                //        kotlinx-coroutines-rx2:1.5.0
                //        https://github.com/Kotlin/kotlinx.coroutines/issues/2173
                //.assertNoErrors()
                //.assertComplete()
                .assertNoValues();


        // NOTE(rohitsat): this is different from data()
        TestObserver<Byte> testObserver = byteRxDataStore.updateDataAsync(Single::just).test();
        testObserver.awaitTerminalEvent();

        testObserver.assertTerminated()
                .assertError(CancellationException.class)
                .assertNoValues();


        RxDataStore<Byte> byteRxDataStore2 = new RxDataStoreBuilder<Byte>(() -> newFile,
                testingSerializer).build();

        // Should not throw
        assertThat(byteRxDataStore2.data().blockingFirst()).isEqualTo((byte) 0);
    }
}
