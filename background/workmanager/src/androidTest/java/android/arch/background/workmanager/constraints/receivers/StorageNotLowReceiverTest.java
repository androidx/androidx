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
package android.arch.background.workmanager.constraints.receivers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class StorageNotLowReceiverTest {
    private StorageNotLowReceiver mReceiver;

    @Before
    public void setUp() {
        mReceiver = new StorageNotLowReceiver(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void testGetIntentFilter() {
        IntentFilter intentFilter = mReceiver.getIntentFilter();
        assertThat(intentFilter.hasAction(Intent.ACTION_DEVICE_STORAGE_OK), is(true));
        assertThat(intentFilter.hasAction(Intent.ACTION_DEVICE_STORAGE_LOW), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }
}
