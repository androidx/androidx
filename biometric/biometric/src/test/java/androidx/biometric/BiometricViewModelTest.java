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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class BiometricViewModelTest {
    private BiometricViewModel mViewModel;

    @Before
    public void setUp() {
        mViewModel = new BiometricViewModel();
    }

    @Test
    public void testCanUpdateLiveDataValue_OnMainThread() {
        mViewModel.setNegativeButtonPressPending(true);
        assertThat(mViewModel.isNegativeButtonPressPending().getValue()).isTrue();
    }

    @Test
    public void testCanUpdateLiveDataValue_OnBackgroundThread() throws Exception {
        final Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mViewModel.setNegativeButtonPressPending(true);
            }
        });
        backgroundThread.start();
        backgroundThread.join();
        ShadowLooper.runUiThreadTasks();
        assertThat(mViewModel.isNegativeButtonPressPending().getValue()).isTrue();
    }
}
