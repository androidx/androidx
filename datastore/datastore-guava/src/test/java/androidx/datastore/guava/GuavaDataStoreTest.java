/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.datastore.guava;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GuavaDataStoreTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static Byte incrementByte(Byte byteIn) {
        return ++byteIn;
    }

    @Test
    public void testGetSingleValue() throws Exception {
        File newFile = tempFolder.newFile();

        GuavaDataStore<Byte> byteStore =
                new GuavaDataStore.Builder<Byte>(new TestingSerializer(), () -> newFile).build();

        ListenableFuture<Byte> firstByte = byteStore.getDataAsync();
        assertThat(firstByte.get()).isEqualTo(0);

        ListenableFuture<Byte> incrementByte = byteStore.updateDataAsync(
                GuavaDataStoreTest::incrementByte);
        assertThat(incrementByte.get()).isEqualTo(1);

        ListenableFuture<Byte> secondByte = byteStore.getDataAsync();
        assertThat(secondByte.get()).isEqualTo(1);
    }

    @Test
    public void testReadFailure() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        GuavaDataStore<Byte> byteStore =
                new GuavaDataStore.Builder<Byte>(testingSerializer, () -> newFile).build();

        testingSerializer.setFailingRead(true);

        Throwable e = assertThrows(ExecutionException.class, () -> byteStore.getDataAsync().get());
        assertThat(e).hasCauseThat().isInstanceOf(IOException.class);

        testingSerializer.setFailingRead(false);

        assertThat(byteStore.getDataAsync().get()).isEqualTo(0);
    }

    @Test
    public void testWriteFailure() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        GuavaDataStore<Byte> byteStore =
                new GuavaDataStore.Builder<Byte>(testingSerializer, () -> newFile).build();

        testingSerializer.setFailingWrite(true);

        ListenableFuture<Byte> incrementByte = byteStore.updateDataAsync(
                GuavaDataStoreTest::incrementByte);
        Throwable e = assertThrows(ExecutionException.class, () -> incrementByte.get());
        assertThat(e).hasCauseThat().isInstanceOf(IOException.class);

        testingSerializer.setFailingWrite(false);

        ListenableFuture<Byte> incrementByte2 = byteStore.updateDataAsync(
                GuavaDataStoreTest::incrementByte);
        assertThat(incrementByte2.get()).isEqualTo((byte) 1);
    }

    @Test
    public void openingSameDataStoreTwice_throwsException() throws Exception {
        File newFile = tempFolder.newFile();
        TestingSerializer testingSerializer = new TestingSerializer();

        GuavaDataStore<Byte> byteGuavaDataStore = new GuavaDataStore.Builder<Byte>(
                testingSerializer,
                () -> newFile).build();

        ListenableFuture<Byte> readFuture1 = byteGuavaDataStore.getDataAsync();
        assertThat(readFuture1.get()).isEqualTo((byte) 0);

        GuavaDataStore<Byte> byteGuavaDataStore2 = new GuavaDataStore.Builder<Byte>(
                testingSerializer, () -> newFile).build();

        ListenableFuture<Byte> readFuture2 = byteGuavaDataStore2.getDataAsync();
        ExecutionException e = assertThrows(ExecutionException.class, () -> readFuture2.get());
        Throwable cause = e.getCause();
        assertThat(cause).isInstanceOf(IllegalStateException.class);
        assertThat(cause).hasMessageThat().contains(
                "There are multiple DataStores active for the same file"
        );
    }
}
