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

import android.os.Build;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.datastore.guava.GuavaDataStore;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class GuavaDataStoreSingleProcessTest {
    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

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
        ListenableFuture<Byte> updateFuture = store.updateDataAsync((input -> ++input));
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
        ListenableFuture<Byte> updateFuture = store.updateDataAsync(input -> ++input);
        assertThat(updateFuture.get()).isEqualTo(1);

        while (state.keepRunning()) {
            Byte updatedData = store.updateDataAsync(byteIn -> byteIn).get();

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
        ListenableFuture<Byte> updateFuture = store.updateDataAsync(input -> ++input);
        counter++;
        assertThat(updateFuture.get()).isEqualTo(counter);

        while (state.keepRunning()) {
            Byte updatedData = store.updateDataAsync(input -> ++input).get();

            state.pauseTiming();
            counter++;
            assertThat(updatedData).isEqualTo(counter);
            state.resumeTiming();
        }
    }
}
