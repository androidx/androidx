/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class EpochTimePlatformDataSourceTest {
    EpochTimePlatformDataSource platformDataSource =
            new EpochTimePlatformDataSource(
                    Instant::now,
                    new PlatformTimeUpdateNotifier() {
                        @Override
                        public void setReceiver(
                                @NonNull Executor executor, @NonNull Runnable tick) {}

                        @Override
                        public void clearReceiver() {}
                    });

    @Test
    public void afterRegistration_callbackIsCalledOnce() {
        List<Instant> consumer1 = new ArrayList<>();
        List<Instant> consumer2 = new ArrayList<>();

        platformDataSource.preRegister();
        platformDataSource.preRegister();
        platformDataSource.registerForData(new AddToListCallback<>(consumer1));
        assertThat(consumer1).isEmpty();
        platformDataSource.registerForData(new AddToListCallback<>(consumer2));

        assertThat(consumer1).isNotEmpty();
        assertThat(consumer2).isNotEmpty();
    }

    @Test
    public void newRegistration_callbackIsCalledAgain() {
        List<Instant> consumer1 = new ArrayList<>();
        List<Instant> consumer2 = new ArrayList<>();

        platformDataSource.preRegister();
        platformDataSource.registerForData(new AddToListCallback<>(consumer1));

        assertThat(consumer1).isNotEmpty();

        platformDataSource.preRegister();
        platformDataSource.registerForData(new AddToListCallback<>(consumer2));

        assertThat(consumer2).isNotEmpty();
    }
}
