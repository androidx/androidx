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

package androidx.datastore.core;

import static com.google.common.truth.Truth.assertThat;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.datastore.guava.GuavaDataStore;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class GuavaDataStoreSingleProcessTest {
    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static Byte incrementByte(Byte byteIn) {
        return ++byteIn;
    }

    private static Byte sameValueByte(Byte byteIn) {
        return byteIn;
    }

    @Test
    public void testCreate() throws Exception {
        BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            File testFile = tmp.newFile();
            GuavaDataStore<Byte> store = new GuavaDataStore.Builder<Byte>(
                    new TestingSerializer(),
                    () -> testFile
            ).build();

            state.pauseTiming();
            Assert.assertNotNull(store);
            state.resumeTiming();
        }
    }

    @Test
    public void testRead() throws Exception {
        BenchmarkState state = benchmarkRule.getState();
        File testFile = tmp.newFile();
        GuavaDataStore<Byte> store = new GuavaDataStore.Builder<Byte>(
                new TestingSerializer(),
                () -> testFile
        ).build();
        ListenableFuture<Byte> updateFuture = store.updateDataAsync(
                GuavaDataStoreSingleProcessTest::incrementByte
        );
        assertThat(updateFuture.get()).isEqualTo(1);

        while (state.keepRunning()) {
            Byte currentData = store.getDataAsync().get();

            state.pauseTiming();
            assertThat(currentData).isEqualTo(1);
            state.resumeTiming();
        }
    }

    @Test
    public void testUpdate_withoutValueChange() throws Exception {
        BenchmarkState state = benchmarkRule.getState();
        File testFile = tmp.newFile();
        GuavaDataStore<Byte> store = new GuavaDataStore.Builder<Byte>(
                new TestingSerializer(),
                () -> testFile
        ).build();
        ListenableFuture<Byte> updateFuture = store.updateDataAsync(
                GuavaDataStoreSingleProcessTest::incrementByte
        );
        assertThat(updateFuture.get()).isEqualTo(1);

        while (state.keepRunning()) {
            Byte updatedData = store.updateDataAsync(
                    GuavaDataStoreSingleProcessTest::sameValueByte).get();

            state.pauseTiming();
            assertThat(updatedData).isEqualTo(1);
            state.resumeTiming();
        }
    }

    @Test
    public void testUpdate_withValueChange() throws Exception {
        BenchmarkState state = benchmarkRule.getState();
        File testFile = tmp.newFile();
        byte counter = 0;
        GuavaDataStore<Byte> store = new GuavaDataStore.Builder<Byte>(
                new TestingSerializer(),
                () -> testFile
        ).build();
        // first update creates the file
        ListenableFuture<Byte> updateFuture = store.updateDataAsync(
                GuavaDataStoreSingleProcessTest::incrementByte
        );
        counter++;
        assertThat(updateFuture.get()).isEqualTo(counter);

        while (state.keepRunning()) {
            Byte updatedData = store.updateDataAsync(
                    GuavaDataStoreSingleProcessTest::incrementByte).get();

            state.pauseTiming();
            counter++;
            assertThat(updatedData).isEqualTo(counter);
            state.resumeTiming();
        }
    }
}
