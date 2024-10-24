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
package androidx.datastore.rxjava3;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler;
import androidx.test.core.app.ApplicationProvider;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RxDataStoreBuilderTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static Single<Byte> incrementByte(Byte byteIn) {
        return Single.just(++byteIn);
    }

    @Test
    public void testConstructWithProduceFile() throws Exception {
        File file = tempFolder.newFile();
        RxDataStore<Byte> dataStore =
                new RxDataStoreBuilder<Byte>(() -> file, new TestingSerializer())
                        .build();
        Single<Byte> incrementByte = dataStore.updateDataAsync(
                RxDataStoreBuilderTest::incrementByte);
        assertThat(incrementByte.blockingGet()).isEqualTo(1);
        dataStore.dispose();
        dataStore.shutdownComplete().blockingAwait();

        // Construct it again and confirm that the data is still there:
        dataStore =
                new RxDataStoreBuilder<Byte>(() -> file, new TestingSerializer())
                        .build();
        assertThat(dataStore.data().blockingFirst()).isEqualTo(1);
    }

    @Test
    public void testConstructWithContextAndName() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String name = "my_data_store";
        RxDataStore<Byte> dataStore =
                new RxDataStoreBuilder<Byte>(context, name, new TestingSerializer())
                        .build();
        Single<Byte> set1 = dataStore.updateDataAsync(input -> Single.just((byte) 1));
        assertThat(set1.blockingGet()).isEqualTo(1);
        dataStore.dispose();
        dataStore.shutdownComplete().blockingAwait();

        // Construct it again and confirm that the data is still there:
        dataStore =
                new RxDataStoreBuilder<Byte>(context, name, new TestingSerializer())
                        .build();
        assertThat(dataStore.data().blockingFirst()).isEqualTo(1);
        dataStore.dispose();
        dataStore.shutdownComplete().blockingAwait();


        // Construct it again with the expected file path and confirm that the data is there:
        dataStore =
                new RxDataStoreBuilder<Byte>(() -> new File(context.getFilesDir().getPath()
                        + "/datastore/" + name), new TestingSerializer()
                )
                        .build();
        assertThat(dataStore.data().blockingFirst()).isEqualTo(1);
    }

    @Test
    public void testMigrationsAreInstalledAndRun() throws Exception {
        RxDataMigration<Byte> plusOneMigration = new RxDataMigration<Byte>() {
            @Override
            public @NonNull Single<Boolean> shouldMigrate(@NonNull Byte currentData) {
                return Single.just(true);
            }

            @Override
            public @NonNull Single<Byte> migrate(@NonNull Byte currentData) {
                return incrementByte(currentData);
            }

            @Override
            public @NonNull Completable cleanUp() {
                return Completable.complete();
            }
        };

        RxDataStore<Byte> dataStore = new RxDataStoreBuilder<Byte>(
                () -> tempFolder.newFile(), new TestingSerializer())
                .addRxDataMigration(plusOneMigration)
                .build();

        assertThat(dataStore.data().blockingFirst()).isEqualTo(1);
    }

    @Test
    public void testSpecifiedSchedulerIsUser() throws Exception {
        Scheduler singleThreadedScheduler =
                Schedulers.from(Executors.newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "TestingThread");
                    }
                }));


        RxDataStore<Byte> dataStore = new RxDataStoreBuilder<Byte>(() -> tempFolder.newFile(),
                new TestingSerializer())
                .setIoScheduler(singleThreadedScheduler)
                .build();
        Single<Byte> update = dataStore.updateDataAsync(input -> {
            Thread currentThread = Thread.currentThread();
            assertThat(currentThread.getName()).isEqualTo("TestingThread");
            return Single.just(input);
        });
        assertThat(update.blockingGet()).isEqualTo((byte) 0);
        Single<Byte> subsequentUpdate = dataStore.updateDataAsync(input -> {
            Thread currentThread = Thread.currentThread();
            assertThat(currentThread.getName()).isEqualTo("TestingThread");
            return Single.just(input);
        });
        assertThat(subsequentUpdate.blockingGet()).isEqualTo((byte) 0);
    }

    @Test
    public void testCorruptionHandlerIsUser() {
        TestingSerializer testingSerializer = new TestingSerializer();
        testingSerializer.setFailReadWithCorruptionException(true);
        ReplaceFileCorruptionHandler<Byte> replaceFileCorruptionHandler =
                new ReplaceFileCorruptionHandler<Byte>(exception -> (byte) 99);


        RxDataStore<Byte> dataStore = new RxDataStoreBuilder<Byte>(
                () -> tempFolder.newFile(),
                testingSerializer)
                .setCorruptionHandler(replaceFileCorruptionHandler)
                .build();
        assertThat(dataStore.data().blockingFirst()).isEqualTo(99);
    }

    @Test
    public void isDisposed() {
        TestingSerializer testingSerializer = new TestingSerializer();
        testingSerializer.setFailReadWithCorruptionException(true);
        RxDataStore<Byte> dataStore = new RxDataStoreBuilder<Byte>(
                () -> tempFolder.newFile(),
                testingSerializer)
                .build();
        assertThat(dataStore.isDisposed()).isFalse();
        dataStore.dispose();
        assertThat(dataStore.isDisposed()).isTrue();
    }
}